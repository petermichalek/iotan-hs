package org.iotus.hs;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import iotus.core.Iotus$;
import iotus.core.IMContext;
import org.projecthaystack.HGrid;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincWriter;
import org.projecthaystack.server.HOp;
import org.projecthaystack.util.Base64;

// Note: this will be placed in web.xml
/*
WEB-INF/web.xml
     <servlet-class>org.iotus.hs.IMHServlet</servlet-class>
 */

/**
 * IMHServlet implements the haystack HTTP REST API for
 * querying entities and history data.
 *
 * @see <a href='http://project-haystack.org/doc/Rest'>Project Haystack</a>
 */
public class IMHServlet extends HttpServlet
{
    // set false to disable auth for testing purposes (but disable never in production)
    private static boolean AUTH_ENABLED = true;
    // db connection pool
    private static DbPool dbPool = new DbPool();
    // list of project deemed to exist and accessible at /api/{project}
    private static List<String> projects = new LinkedList();

    //////////////////////////////////////////////////////////////////////////
    // Database Hook
    //////////////////////////////////////////////////////////////////////////

  /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger("org.iotus");
    private static final List<String> UNAUTH_OPS = Arrays.asList(
                "about", "formats", "ops"
        );

    /**
     * Number of minutes after login when session expires on inactive connection
     */
    private int sessionExpire = 10;

    // Interface with singleton Iotus in scala
    private static Iotus$ iotus = Iotus$.MODULE$;

    static {
        //Logger.getLogger("com.datastax.driver.core.Session").setLevel(Level.parse("INFO"));
        //Logger.getLogger("com.datastax.driver.core.Connection").setLevel(Level.parse("INFO"));
        logger.setLevel(Level.parse("INFO"));
        iotus.start();

        initProjects();
    }

    private static void initProjects() {
        HGrid grid = iotus.listProjects(null).toGrid();
        //for (int i = 0; i < grid.numRows(); i++) {
        //    HRow row = grid.iterator();
        //}
        logger.info("projects grid: " + HZincWriter.gridToString(grid));
        synchronized (projects) {
            projects.clear();
            if (grid.numRows() == 0) {
                logger.severe("No project configured in the database.");
            } else {
                logger.info(String.format("%d projects configured in the database.", grid.numRows()));
                Iterator<HRow> it = grid.iterator();
                while (it.hasNext()) {
                    HRow row = it.next();
                    projects.add(row.getStr("pid"));
                }
            }
        }

    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException
    {
        onService("GET", req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException
    {
        onService("POST", req, res);
    }

    /**
     * Validates authorization headers
     * @param req
     * @return true if validation passed, false otherwise
     */
    private boolean validateAuth(HttpServletRequest req) {
        boolean rc = false;
        //logger.info("validateAuth start");
        // perform authorization
        String authHeader = req.getHeader("Authorization");
        String username = null;
        String password = null;
        if (authHeader != null) {
            //out.println("Base64-encoded Authorization Value: <em>" + encodedValue);
            try {
                String encodedValue = authHeader.split(" ")[1];
                String decodedValue = Base64.STANDARD.decode(encodedValue);
                logger.fine("Authorization decoded:" + decodedValue);
                String creds[] = decodedValue.split(":");
                if (creds.length != 2) {
                    logger.severe("Authorization must be of the form user:password");
                } else {
                    username = creds[0];
                    password = creds[1];
                    //logger.fine("username: " + username);
                    //logger.fine("password: " + password);
                    HttpSession session = req.getSession();
                    Object sessionUser = session.getAttribute("user");
                    if (sessionUser != null && username.equals(sessionUser)) {
                        logger.fine(String.format(
                                "session already validated for user %s. No addtional validateCredentials call needed",
                                username));
                        rc = true;
                    } else {
                        rc = iotus.validateCredentials(username, password);
                        if (rc) {
                            logger.info("Validation passed: for " + username);
                            session.setAttribute("user", username);
                            session.setMaxInactiveInterval(sessionExpire*60);
                        } else {
                            logger.warning("Validation failed for " + username);
                        }
                    }

                }
            } catch(Exception e) {
                logger.severe("Authorization can't be decoded: " + e);
            }
        } else {
            logger.fine("No Authorization header");
        }
        //logger.info("validateAuth end");
        return rc;
    }
  
    /**
       * Override onService parent, mainly to implement multi-tenant database that can switch between projects.
     */
    private void onService(String method, HttpServletRequest req, HttpServletResponse res) throws ServletException,
        IOException {
        /*
        dump requests, e.g.:
method      = GET
pathInfo    = /test-project/read
contextPath =
servletPath = /api
query       = filter=point and dis=="test pt 03" or dis=="test pt 02"
headers:
    Authorization = Basic ZnJhbmt...
    Accept = ...
    User-Agent = curl/7.49.1
    Host = localhost:1225

         */
        //dumpReq(req);
        HttpSession session = req.getSession();

        // if root, then redirect to {haystack}/about
        String path = req.getPathInfo();
        if (path == null || path.length() == 0 || path.equals("/"))
        {
          res.sendRedirect(req.getServletPath() + "/about");
          return;
        }
        String user = (String)session.getAttribute("user");
        String sessionId = session.getId();
        int maxInactiveInterval = session.getMaxInactiveInterval();
        logger.fine(String.format("session %s: user=%s, %d", sessionId, user, maxInactiveInterval));

        String opName = null;
        String project = null;
        // parse URI path into "/{opName}/...."
        int slash = path.indexOf('/', 1);
        if (slash < 0) slash = path.length();
        int slash2 = path.lastIndexOf('/');
        if (slash2 > 0) {
            project = path.substring(1, slash);
            opName = path.substring(slash2+1);
            logger.fine("path, opName, project: " + path + "; " + opName  + "; " + project);
            synchronized (projects) {
                if (!projects.contains(project)) {
                    // see if new project added since init
                    HGrid grid = iotus.projectByName(project).toGrid();
                    if (grid.numRows() > 0) {
                        // yes new project added, add it to our list of pid's
                        projects.add(project);
                    } else {
                        res.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                }
            }
            if (AUTH_ENABLED) {
                if (!validateAuth(req)) {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setHeader("WWW-Authenticate", "Basic realm=\"Haystack\"");
                    return;
                }
            }
            IMDatabase db = dbPool.checkOut();
            IMContext ctx = db.getContext();
            // create a new context if for this db context already exists
            if ( !(ctx != null && db.getContext().pid().equals(project))) {
                ctx = new IMContext(project, user);
                db.setContext(ctx);
            }
            // if this is an old context and a different author/user, we may need to set the context author
            if (ctx != null && ctx.getAuthor() != null && !ctx.getAuthor().equals(user)) {
                ctx.setAuthor(user);
            }


            /*
             TODO:
             * enable verification of project access in /api/demo/about
             * enable project specific db connector from db map if needed, or use pooled connector for all projects
            */
            HOp op = null;
            try {
                // resolve the op
                op = db.op(opName, false);
                if (op == null)
                {
                    res.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            } catch (Exception e) {
                dbPool.checkIn(db);
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // route to the op
            try
            {
                op.onService(db, req, res);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new ServletException(e);
            } finally {
                dbPool.checkIn(db);
            }
        } else {
            if (slash2 == 0) {
                // un-authenticate request
                opName = path.substring(1);
                logger.fine("path, opName (no project) - processing unauthenticated request: " + path + "; " + opName);
                // get db for any project
                IMDatabase db = dbPool.checkOut();
                // create a new context if for this db context already exists
                if ( db.getContext() == null) {
                    IMContext ctx = new IMContext("demo", user);
                    db.setContext(ctx);
                }
                HOp op = null;
                try {
                    if (UNAUTH_OPS.contains(opName)) {
                        // resolve the op
                        op = db.op(opName, false);
                        if (op == null)
                        {
                            res.sendError(HttpServletResponse.SC_NOT_FOUND);
                            return;
                        }
                    } else {
                        logger.severe("Only unauth operations permitted at non-project /api path");
                        res.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                } catch (Exception e) {
                    dbPool.checkIn(db);
                    res.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                // route to the op
                try
                {
                    op.onService(db, req, res);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    throw new ServletException(e);
                } finally {
                    dbPool.checkIn(db);
                }

            } else {
                //opName = path.substring(slash2+1);
                // TODO: add unauth db invocation with null project for /api/about etc.
                String msg = "Incorrect request: can't parse project and operation from path " + path;
                logger.severe("path, opName: " + path + "; " + opName);
                throw new ServletException(msg);
            }
        }

}

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

    void dumpReq(HttpServletRequest req) { dumpReq(req, null); }
    void dumpReq(HttpServletRequest req, PrintWriter out)
    {
        try
        {
            if (out == null) out = new PrintWriter(System.out);
            out.println("==========================================");
            out.println("method      = " + req.getMethod());
            out.println("pathInfo    = " + req.getPathInfo());
            out.println("contextPath = " + req.getContextPath());
            out.println("servletPath = " + req.getServletPath());
            out.println("query       = " + (req.getQueryString() == null ? "null" : URLDecoder.decode(req.getQueryString(), "UTF-8")));
            out.println("headers:");
            Enumeration e = req.getHeaderNames();
            while (e.hasMoreElements())
            {
                String key = (String)e.nextElement();
                String val = req.getHeader(key);
                out.println("  " + key + " = " + val);
            }
            out.flush();
        }
        catch (Exception e) { e.printStackTrace(); }
    }

}
