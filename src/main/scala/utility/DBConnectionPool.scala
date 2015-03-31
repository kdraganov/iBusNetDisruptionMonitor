package utility

import java.io.{File, FileNotFoundException}
import java.sql.Connection

import org.postgresql.jdbc3.Jdbc3PoolingDataSource

import scala.xml.XML


/**
 * Created by Konstantin on 17/03/2015.
 */
class DBConnectionPool(private val host: String,
                       private val port: Integer,
                       private val db: String,
                       private val user: String,
                       private val password: String,
                       private val name: String = "DBConnectionPool") {

  private val pool = new Jdbc3PoolingDataSource()
  pool.setDataSourceName(name)
  pool.setServerName(host)
  pool.setDatabaseName(db)
  pool.setUser(user)
  pool.setPassword(password)
  pool.setMaxConnections(5)
  pool.setInitialConnections(2)
  pool.setPortNumber(port)

  def getConnection(): Connection = {
    return pool.getConnection()
  }

  def setMaxConnections(maxConnections: Integer): Unit = {
    pool.setMaxConnections(maxConnections)
  }

  def close(): Unit = {
    pool.close()
  }

  //  val driver = "org.postgresql.Driver"
  //  //"org.postgresql.jdbc.Driver" //
  //  val url = "jdbc:postgresql://localhost:5432";
  //  val port = "5432"
  //  val user = "postgres"
  //  val password = "299188"

  //  Class.forName(driver);
  //  var connection: Connection = DriverManager.getConnection(url, user, password)

  //  private def setPool(): Jdbc3PoolingDataSource = {
  //    val source = new Jdbc3PoolingDataSource()
  //    source.setDataSourceName(name)
  //    source.setServerName(host)
  //    source.setDatabaseName(db)
  //    source.setUser(user)
  //    source.setPassword(password)
  //    source.setMaxConnections(5)
  //    return source
  //  }

  //  def init(): PooledConnection = {
  //    val props = new Properties()
  //    props.put(Context.INITIAL_CONTEXT_FACTORY, "org.postgresql.jdbc3.Jdbc3PoolingDataSource")
  //    props.setProperty(Context.SECURITY_PRINCIPAL, user);
  //    props.setProperty(Context.SECURITY_CREDENTIALS, password);
  //    //    props.setProperty("ssl", "true");
  //    props.put(Context.PROVIDER_URL, url);
  //    val context = new InitialContext(props)
  //    val dataSource: ConnectionPoolDataSource = context.lookup("iBusDisruption").asInstanceOf[ConnectionPoolDataSource]
  //    dataSource.getPooledConnection
  //  }

  //  def closeConnection: Unit = {
  //    connection.close()
  //  }

}

object DBConnectionPool {

  private var sourcePool: DBConnectionPool = null
  var host = ""
  var port = 0
  var db = ""
  var user = ""
  var password = ""
  var maxPoolSize = 5

  def createPool(host: String,
                 port: Integer,
                 db: String,
                 user: String,
                 password: String,
                 name: String = "DBConnectionPool"
                  ): Unit = {
    sourcePool = new DBConnectionPool(host, port, db, user, password)
  }

  def createPool(connectionSettingsPath: String): Unit = {
    val file = new File(connectionSettingsPath)
    if (!file.exists() || !file.isFile || !file.canRead) {
      throw new FileNotFoundException("Settings file [" + connectionSettingsPath + "] is missing or cannot be accessed.")
    }
    val settingsXML = XML.loadFile(file)
    host = (settingsXML \\ "connection" \\ "host").text
    port = Integer.parseInt((settingsXML \\ "connection" \\ "port").text)
    db = (settingsXML \\ "connection" \\ "database").text
    user = (settingsXML \\ "connection" \\ "user").text
    password = (settingsXML \\ "connection" \\ "password").text
    maxPoolSize = Integer.parseInt((settingsXML \\ "connection" \\ "password").text)
    sourcePool = new DBConnectionPool(host, port, db, user, password)
  }

  def setPool(pool: DBConnectionPool): Unit = {
    if (sourcePool != null) {
      sourcePool.close
    }
    sourcePool = pool
  }

  def getConnection(): Connection = {
    sourcePool.getConnection()
  }

  def returnConnection(connection: Connection): Unit = {
    connection.close()
  }

  def close(): Unit = {
    sourcePool.close()

  }
}