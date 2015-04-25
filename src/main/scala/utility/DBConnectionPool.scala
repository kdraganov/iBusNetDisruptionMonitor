package utility

import java.io.{File, FileNotFoundException}
import java.sql.Connection

import org.postgresql.jdbc3.Jdbc3PoolingDataSource

import scala.xml.XML


/**
 * Created by Konstantin on 17/03/2015.
 *
 * Class representing a Database connection pool.
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

  /**
   *
   * @return Connection - a connection from the pool.
   */
  def getConnection(): Connection = {
    return pool.getConnection()
  }

  /**
   *
   * @param maxConnections Integer - the max number of simultaneous connections
   */
  def setMaxConnections(maxConnections: Integer): Unit = {
    pool.setMaxConnections(maxConnections)
  }

  /**
   *
   * @return Integer - the max number of simultaneous connections
   */
  def getMaxConnections(): Integer = pool.getMaxConnections

  /**
   * Closes the pool.
   */
  def close(): Unit = {
    pool.close()
  }
}

/**
 * Static class representing a Database pool used accross the system.
 */
object DBConnectionPool {

  private var sourcePool: DBConnectionPool = null
  var host = ""
  var port = 0
  var db = ""
  var user = ""
  var password = ""
  var maxPoolSize = 5

  /**
   * Creates the pool
   * @param host String - the host address
   * @param port Integer - the port used to connect to the database
   * @param db String - the database name
   * @param user String - the username
   * @param password String - the password
   * @param name String - name for the pool
   */
  def createPool(host: String,
                 port: Integer,
                 db: String,
                 user: String,
                 password: String,
                 name: String = "DBConnectionPool"
                  ): Unit = {
    sourcePool = new DBConnectionPool(host, port, db, user, password)
  }

  /**
   *
   * @param connectionSettingsPath String - the path to the file with the settings for connecting to the database
   */
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

  def setMaxPoolSize(poolSize: Integer): Unit = {
    sourcePool.setMaxConnections(poolSize)
  }

  def getMaxPoolSize(): Integer = {
    sourcePool.getMaxConnections()
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