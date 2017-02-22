package org.iotus.hs;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import org.projecthaystack.*;
import org.projecthaystack.io.HZincWriter;
import org.projecthaystack.server.HOp;
import org.projecthaystack.server.HServer;
import org.projecthaystack.server.HStdOps;
import iotus.core.IMContext;
import iotus.core.IMFrame;

/**
 * HProvides actual haystack request processing
 */
final class IMDatabase extends HServer {

    private IMContext context;
    static final HDateTime bootTime = HDateTime.now();
    IMDatabase() {
    }

    private static final Logger logger = Logger.getLogger("org.iotus");

    @Override
    public HOp[] ops() {
        return new HOp[] {
          aboutOp,
          HStdOps.ops,
          HStdOps.formats,
          HStdOps.read,
          HStdOps.nav,
          //HStdOps.pointWrite,
          HStdOps.hisRead,
          // override hisWrite with our own
          // special custom hisWriteOp with onHisWriteEx
          hisWriteOp,
          //HStdOps.invokeAction,
          evalOp
        };
    }


    // override op so that we can provide our own hisWriteOp that doesn't do
    //   unnecessary parsing and reconstruction of grid parameter
    @Override
    public HOp op(String name, boolean checked) {
        if (name.equals("hisWrite")) {
            return hisWriteOp;
        } else {
            return super.op(name, checked);
        }
    }

    @Override
    protected HDict onAbout() {
        return about;
    }

    private final HDict about = new HDictBuilder()
        .add("serverName", hostName())
        .add("vendorName", Constants.VENDOR_NAME)
        .add("vendorUri", HUri.make(Constants.VENDOR_URL))
        .add("productName", Constants.PRODUCT_NAME)
        .add("productVersion", Constants.PRODUCT_VERSION)
        .add("productUri", HUri.make(Constants.VENDOR_URL))
            //.add("tz", HTimeZone.DEFAULT.name)
            //.add("tz", "New_York")

        .toDict();

    private static String hostName()
    {
      try { return InetAddress.getLocalHost().getHostName(); }
      catch (Exception e) { return "Unknown"; }
    }


    @Override
    protected Iterator iterator() {
        return null;
    }

    @Override
    protected HGrid onNav(String navId) {
        return null;
    }

    @Override
    protected HDict onNavReadByUri(HUri uri) {
        return null;
    }

    @Override
    protected HWatch onWatchOpen(String dis, HNum lease) {
        return null;
    }

    @Override
    protected HWatch[] onWatches() {
        return new HWatch[0];
    }

    @Override
    protected HWatch onWatch(String id) {
        return null;
    }

    @Override
    protected HGrid onPointWriteArray(HDict rec) {
        return null;
    }

    @Override
    protected void onPointWrite(HDict rec, int level, HVal val, String who, HNum dur, HDict opts) {
    }

    @Override
    protected HHisItem[] onHisRead(HDict rec, HDateTimeRange range) {
        String id = rec.id().toCode();
        logger.info("onHisRead: " + id + ", " + rec.id());

        HGrid grid = context.hisReadJava(range, id).toGrid();
        logger.info("onHisRead: grid=" + HZincWriter.gridToString(grid));

        // build HHisItem[] response from the grid of ts/id
        // TODO: change onHisRead to return Grid directly
        // but that would require changing the opensource project
        //  HServer class function hisRead:   public final HGrid hisRead(HRef id, Object range)

        /*
        ts,id
        2016-10-23T19:00:13.260-07:00 Los_Angeles,70.8
        2016-10-23T19:15:00-07:00 Los_Angeles,71.2
         */
        ArrayList<HHisItem> acc = new ArrayList<>();
        for (int i=0; i < grid.numRows(); i++) {
            HDateTime ts = (HDateTime) grid.row(i).get("ts");
            HVal val = grid.row(i).get("val");
            acc.add(HHisItem.make(ts, val));
        }
        return acc.toArray(new HHisItem[acc.size()]);
    }

    @Override
    protected void onHisWrite(HDict rec, HHisItem[] items) {
        // noop: We will not override onHisWrite, but onHisWriteEx instead
        //IMFrame frame = IMFrame$.MODULE$.apply(grid, false);
        //context.hisWrite(frame);
    }

    // We will not override onHisWrite, but rather introduce onhisWriteEx which takes grid
    // as parameter which is more convinent for our Iotus context without a need for parsing
    // and recomposing grid for HHisItems
    protected void onHisWriteEx(HGrid grid) {
        IMFrame frame = IMFrame.apply(grid, false);
        logger.info("onHisWriteEx: grid=" + HZincWriter.gridToString(grid));
        context.hisWrite(frame);
        logger.info("onHisWriteEx: done");
    }

    @Override
    protected HGrid onInvokeAction(HDict rec, String action, HDict args) {
        return null;
    }

    @Override
    protected HDict onReadById(HRef id) {
        logger.info("onReadById: " + id.toZinc());
        HDict res = context.readById(id.toZinc()).toDict();
        //HGrid grid = context.toGrid();
        //logger.info("onReadById: grid=" + HZincWriter.gridToString(grid));
        logger.info("onReadById: res=" + res.toZinc());
        return res;
    }

    @Override
    protected HGrid onReadAll(String filter, int limit) {
        return context.readAll(filter, limit).toGrid();
    }


    // ********************************************
    // **************** Accessors *****************
    // ********************************************

    public IMContext getContext() {
        return context;
    }

    void setContext(IMContext context) {
        this.context = context;
    }

    // override hisWrite op
    private static final class MyHisWriteOp extends HOp
    {
        public String name() { return "hisWrite"; }
        public String summary() { return "Write time series data to historian"; }
        public HGrid onService(HServer db, HGrid req) throws Exception
        {
            if (req.isEmpty()) throw new Exception("Request has no rows");
            //HRef id = valToId(db, req.meta().get("id"));
            //HHisItem[] items = HHisItem.gridToItems(req);
            ((IMDatabase)db).onHisWriteEx(req);
            return HGrid.EMPTY;
        }
    }
    private static final HOp hisWriteOp = new MyHisWriteOp();

    // override AboutOp
    private static final class MyAboutOp extends HOp
    {
        public String name() { return "about"; }
        public String summary() { return "Summary information for server"; }
        public HGrid onService(HServer db, HGrid req) throws Exception
        {
            IMContext ctx = ((IMDatabase)db).getContext();
            // create new boottime with correct timezone
            HDateTime boottime = HDateTime.make(((IMDatabase)db).bootTime.millis(),ctx.tzhs()) ;
            HDict dict = new HDictBuilder()
                    .add(db.about())
                    // use timezone from context/project
                    .add("tz", ctx.tzhs().name)
                    .add("serverTime", HDateTime.now(ctx.tzhs()))
                    .add("serverBootTime", boottime)
                    .toDict();
            return HGridBuilder.dictToGrid(dict);
        }
    }
    private static final HOp aboutOp = new MyAboutOp();

    private static final class MyEvalOp extends HOp
    {
        public String name() { return "eval"; }
        public String summary() { return "Evaluate server side function"; }
        public HGrid onService(HServer db, HGrid req) throws Exception
        {
            IMContext ctx = ((IMDatabase)db).getContext();
            ctx.clear();
            if (req.col("expr", false) == null) {
                throw new Exception("Request is missing expr column");
            }
            if (req.numRows() != 1) {
                throw new Exception("Request needs to have example one row");
            }
            HRow row = req.row(0);
            String expr = row.get("expr").toString();
            ctx.eval(expr);
            return ctx.toGrid();
        }
    }
    private static final HOp evalOp = new MyEvalOp();
}
