# IoTan Haystack Server 

The iotan-hs provides a haystack 3.0 compliant reference server with database support
via IoTan core (currently Cassandra NonSQL database is used) 

It uses haystack client and server support library from haystack-java open source project.

### Pre-requisites ###

The open source haystack-java project must be installed in a parallel directory, 
by default named ../haystack-java.
(This can be adjusted by modifying gradle.settings)

For the server to work, IoTan core must have been setup and Cassandra 
database just have been setup and populated as, described in iotan-core/README. 

### Building ###


The project is built with [Gradle](http://gradle.org/). It makes uses of the [gradle wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) so that you don't have to actually install Gradle yourself. You should use the `gradlew` script to run all gradle tasks for this project.

After cloning the repository, run the following command to build and test the library.

    ./gradlew build # (Unix)
    ./gradlew.bat build REM (Windows)

It is highly recommended to enable the [gradle daemon](https://docs.gradle.org/current/userguide/gradle_daemon.html) so builds go faster.

### Java Server Notes

To run java server after building as above:

This copies .jar to WEB-INF/lib

    ./deploy.sh
    
This starts or restart the server

    ./restart.sh

The server listens on port 1225, that can be changed by modifying winstone.properties.

Test by navigating to:

http://localhost:1225/api/demo/about
http://localhost:1225/api/demo/read?filter=site

In the browser, you'll be prompted for username and password. Any combination will work.

You can also use curl tool with credentials, e.g. like this:

curl -u scott:tiger http://localhost:1225/demo/about

### More request/response examples

    curl http://localhost:1225/api/test-project/about
    
    ver:"2.0"
    vendorUri,productUri,tz,serverName,productName,haystackVersion,productVersion,serverTime,serverBootTime,vendorName
    `http://iotan.org`,`http://iotan.org`,"New_York","1da58d48064c","IoTan Haystack Server","2.0","0.1.01",2017-02-17T19:54:19.545-05:00 New_York,2017-02-17T19:54:01.954-05:00 New_York,"IoT/Analytics Micro Services Project"


    curl http://localhost:1225/api/test-project/read?filter=point
    ver:"2.0"
    id,equipRef,point,his,hisSize,tz,dis,kind
    @756716f7-2e54-4715-9f00-91dcbea6c102 "test pt 03",@756716f7-2e54-4715-9f01-91dcbea6c300,M,M,10,"Los_Angeles","test pt 03","Number"
    @756716f7-2e54-4715-9f00-91dcbea6c101 "test pt 02",@756716f7-2e54-4715-9f01-91dcbea6c300,M,M,10,"Los_Angeles","test pt 02","Number"
    @756716f7-2e54-4715-9f00-91dcbea6c100 "test 01",@756716f7-2e54-4715-9f01-91dcbea6c300,M,M,,"Los_Angeles","test 01","Number"

Other examples with more complex filters:

    curl 'http://localhost:1225/api/test-project/read?filter=point%20and%20dis=="test%20pt%2003"'
    ver:"2.0"
    id,equipRef,hisSize,tz,dis,kind
    @756716f7-2e54-4715-9f00-91dcbea6c102 "test pt 03",@756716f7-2e54-4715-9f01-91dcbea6c300,10,"Los_Angeles","test pt 03","Number"

    curl 'http://localhost:1225/api/test-project/read?filter=point%20and%20dis=="test%20pt%2003"%20or%20dis=="test%20pt%2002"'
    ver:"2.0"
    id,equipRef,hisSize,tz,dis,kind
    @756716f7-2e54-4715-9f00-91dcbea6c102 "test pt 03",@756716f7-2e54-4715-9f01-91dcbea6c300,10,"Los_Angeles","test pt 03","Number"
    @756716f7-2e54-4715-9f00-91dcbea6c101 "test pt 02",@756716f7-2e54-4715-9f01-91dcbea6c300,10,"Los_Angeles","test pt 02","Number"

    curl 'http://localhost:1225/api/test-project/read?filter=point%20and%20(dis%3C=%22test%2001%22%20or%20hisVal%3E73.1)'
    ver:"2.0"
    id,equipRef,hisSize,hisVal,tz,mybool,dis,kind
    @756716f7-2e54-4715-9f00-91dcbea6c100 "test 01",@756716f7-2e54-4715-9f01-91dcbea6c300,,,"Los_Angeles",,"test 01","Number"
    @756716f7-2e54-4715-9f00-91dcbea6c103 "test pt 04 (c103)",@756716f7-2e54-4715-9f01-91dcbea6c300,10,73.2,"Los_Angeles",,"test pt 04 (c103)","Number"
    @756716f7-2e54-4715-9f00-91dcbea6c104 "test pt 05 (c104)",@756716f7-2e54-4715-9f01-91dcbea6c300,10,73.2,"Los_Angeles",T,"test pt 05 (c104)","Number"

Other examples for filter without payload:

    http://localhost:1225/api/test-project/read?filter=point%20and%20(hisSize%3E=11%20or%20hisVal%3E73.1)
    http://localhost:1225/api/test-project/read?filter=point%20and%20(hisSize%3E=10%20or%20hisVal%3E73.1)
    http://localhost:1225/api/test-project/read?filter=point%20and%20(dis%3E=%22test%20pt%22%20or%20hisVal%3E73.1)


    curl http://localhost:1225/api/test-project/read?id=@756716f7-2e54-4715-9f00-91dcbea6c100
    
    ver:"2.0"
    dis,equipRef,id
    "test 01","756716f7-2e54-4715-9f01-91dcbea6c300","756716f7-2e54-4715-9f00-91dcbea6c100"

    curl  'localhost:1225/api/test-project/hisRead?id=@756716f7-2e54-4715-9f00-91dcbea6c100&range="2016-01-01,2017-01-01"'
    
    ver:"2.0" id:@756716f7-2e54-4715-9f00-91dcbea6c100 hisStart:2016-01-01T00:00:00-08:00 Los_Angeles hisEnd:2017-01-02T00:00:00-08:00 Los_Angeles
    ts,val
    2016-10-23T19:00:13.260-07:00 Los_Angeles,70.8
    2016-10-23T19:15:00-07:00 Los_Angeles,71.2
    2016-10-23T19:30:00-07:00 Los_Angeles,71.4
    2016-11-17T00:00:00-08:00 Los_Angeles,69.4
    2016-11-17T00:15:00-08:00 Los_Angeles,69.1
    2016-11-17T04:30:00-08:00 Los_Angeles,66.5

### Supported Operations


IoTus Haystack server supports the following operations of standard haystack:

* "about", "summary": "Summary information for server"},
* "ops", "summary": "Operations supported by this server"},
* "formats", "summary": "Grid data formats supported by this server"},
* "read", "summary": "Read entity records in database"},
* "nav", "summary": "Navigate record tree"},
* "hisRead", "summary": "Navigate record tree"},
* "hisWrite", "summary": "Write time series data to historian"},

It also support eval as an extension, which is not standard haystack but is supported by skyspark.

Note: pointWrite and invokeAction are not supported

Here is an ops response that specifies details of all supported operations::

```bash
curl  http://localhost:1225/api/test-project/ops
    
ver:"2.0"
name,summary
"about","Summary information for server"
"ops","Operations supported by this server"
"formats","Grid data formats supported by this server"
"read","Read entity records in database"
"nav","Navigate record tree"
"hisRead","Read time series from historian"
```
    
It supports the following formats, some of them only in responses:

    text/zinc
    text/csv
    application/json
    
    
Here is a formats response that specifies details of which read/write mimes are supported:
    
```bash
curl  http://localhost:1225/api/test-project/formats
ver:"2.0"
mime,read,write
"text/zinc",M,M
"application/json",,M
"text/csv",,M
"text/plain",M,M
```


Examples of haystack standard operations are at:
  [the haystack project website](http://project-haystack.org/doc)

### Unauthenticated Operations

URLs without project in thepath can be used that don't need authentication.
Unauthenticated/non-project operations are:

    about
    formats
    ops

  
Therefore, these unauthenticated requests with work:

    curl  http://localhost:1225/api/about
    curl  http://localhost:1225/api/formats
    curl  http://localhost:1225/api/ops

But this operation will return "NOT FOUND" error:

    curl http://localhost:1225/api/read?filter=point

  
## License
  
  Apache 2.0
