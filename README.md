iBus Disruption Monitor Engine
===========

Author: Konstantin Draganov
===========

### About
This is a prototypical tool for Real-time visualisation of bus delays in London.
It has been developed as part of my final year MSci Computer Science project supervised by Dr. Steffen Zschaler (http://steffen-zschaler.de/) and it is in collaboration with TFL.

###Installation

The disruption engine installation consists of the following steps:

1. Obtain the jar package of the tool. Alternatively download source and compile JDK 8 would be required in that case.
2. Obtain and have all of the above dependencies in the CLASSPATH of
the system.
3. Set up a database using the provided database script or dump. Need to also configure the Engine inside the EngineConfiguration table.
4. Create XML file which to contain the connection settings to the respective
database.

###Dependencies
Below is the list of the required libraries and dependencies:

* JRE 8 - the library and documentation can be found on http://www.
oracle.com/technetwork/java/javase/documentation/index.html.
* Scala Library 2.11.5 - http://www.scala-lang.org/news/2.11.5.
* Scala XML 2.11-1.0.2 - is used for working with XML files.
* JDBC PostgreSQL - PostgreSQL 9.0 JDBC3 and PostgreSQL 9.4 JDBC4
are both used which can be found in https://jdbc.postgresql.org/
download.html.
* SLF4J 1.6.4 - is used for logging. Documentation and the library can
be found here http://www.slf4j.org/. It also requires the bellow two
libraries in order to work.
* Logback 1.0.1 - is used for logging http://logback.qos.ch/. The engine
makes use of the classic and core packages both version 1.0.1.
* JCoord-1.0 - is used for converting easting/northing locations to the
respective longitude/latitude values. The library was obtained from
http://www.jstott.me.uk/jcoord/.
* ScalaTest 2.2.4 - library was used for testing. Instructions and the library
files can be found in http://www.scalatest.org/download.

###Execution

In order to run the application you simply need to execute the iBusDisruptionMonitor.
jar with the following command from the command line:

<tt>java −jar iBusDisruptionMonitor . jar [ path ]</tt>

In the above command you need to substitute [path] with the path to an XML
file containing the connection settings to a database. The XML file should
have the following structure:
```xml
<?xml version=”1.0” encoding=”UTF−8” ?></tt>
<connection>
<host>[HOST]</ host>
<port>[PORT]</ port>
<database>[Database name]</ database>
<user>[USERNAME]</ user>
<password>[PASSWORD]</password>
<maxPoolSize>5</maxPoolSize>
</ connection>
```
In this XML file you need to substitute everything between the square brackets
with the respective values for your configuration.
Executing the above command will start the tool, but do make sure you
have set up the right configuration settings in the database before running the
application.

For an up to date list of bus stops and bus routes please check http://www.tfl.gov.uk/info-for/open-data-users/our-feeds?intcmp=3671

###Simulation

An executable (iBusMonitorFeedSimulation.jar) which enables the feed file simulation can be found the repository.
To run it simply execute it by providing as an argument the path to your XML db connection (as for the Engine itself)
settings file. You also need to set up the proper source directories in the database.

For any questions or problems related to this repository, please feel free to email on konstantinvld@gmail.com
