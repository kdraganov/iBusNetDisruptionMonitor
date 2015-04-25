package utility

import java.io.{File, FileNotFoundException}
import java.nio.file.{FileSystems, Files, StandardCopyOption}
import java.sql.{Connection, PreparedStatement, SQLException}

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
 * Created by Konstantin on 17/02/2015.
 *
 * Used for simulation, however it has been moved as a separate application.
 */
class FeedThread(private val subDir: String, private val operator: String, private var sleepInterval: Long = 5000) extends Thread {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val feedDirectory: File = new File("E:\\Workspace\\iBusNetTestDirectory\\Feeds\\" + subDir)
  private val feedFilenameFilter = new CustomFilenameFilter("CC_", ".csv")
  private val operatorFilenameFilter = new CustomFilenameFilter("CC_", operator + "_YYYYMMDD_NNNNN")
  private val feedsBuffer: ArrayBuffer[File] = new ArrayBuffer[File]()

  def init(): Unit = {
    val tempBuffer = new ArrayBuffer[File]()
    for (operatorDir: File <- feedDirectory.listFiles(operatorFilenameFilter) if operatorDir.isDirectory) {
      tempBuffer.appendAll(operatorDir.listFiles(feedFilenameFilter))
    }
    feedsBuffer.appendAll(tempBuffer.sortBy(f => (f.getName.substring(f.getName.indexOf("_", 3) + 1))))
    logger.debug("{} feed files loaded in buffer.", feedsBuffer.size)
  }

  override
  def run(): Unit = {
    init()
    var terminate = false
    val seen: ArrayBuffer[String] = new ArrayBuffer[String]()
    while (!terminate) {
      seen.clear()
      speedControl()
      var seenAll = false
      try {
        while (!seenAll && !terminate) {
          val fileOperator = feedsBuffer(0).getName.substring(3, feedsBuffer(0).getName.indexOf("_", 3))
          if (seen.contains(fileOperator)) {
            seenAll = true
          } else {
            seen.append(fileOperator)
            copy(feedsBuffer.remove(0))
          }
          terminate = feedsBuffer.isEmpty
        }
      } catch {
        case e: FileNotFoundException => logger.error("File {} generated FileNotFoundException. File is being ignored.", feedsBuffer.remove(0).getAbsolutePath)
        case e: Exception => logger.error("TERMINATING - FeedsThread interrupted:", e)
          System.exit(-1)
      }

      try {
        Thread.sleep(sleepInterval)
      } catch {
        case e: InterruptedException => logger.error("Feed thread interrupted:", e)
      }
    }
    logger.debug("All feed files have been copied.")
    logger.debug("Feed thread completed!")
  }

  @throws(classOf[FileNotFoundException])
  private def copy(file: File): Unit = {
    logger.trace("Copying file [{}] to {}.", file.getName, Environment.getFeedDirectory().getName)
    val sourceFile = FileSystems.getDefault.getPath(file.getAbsolutePath)
    val destinationFile = FileSystems.getDefault.getPath(Environment.getFeedDirectory().getAbsolutePath, file.getName)
    Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING)
  }

  private def speedControl(): Unit = {
    var pause = true
    while (pause) {
      var connection: Connection = null
      var preparedStatement: PreparedStatement = null
      val selectSQL = "SELECT * FROM \"EngineConfigurations\" WHERE key like 'feedThread%'"
      try {
        connection = DBConnectionPool.getConnection()
        preparedStatement = connection.prepareStatement(selectSQL)
        val rs = preparedStatement.executeQuery()
        while (rs.next()) {
          if (rs.getString("key") == "feedThreadPaused") {
            pause = rs.getBoolean("value")
          } else if (rs.getString("key") == "feedThreadSpeedInMilliSeconds") {
            sleepInterval = rs.getLong("value")
          }
        }
      }
      catch {
        case e: SQLException => logger.error("Exception:", e)
      } finally {
        if (preparedStatement != null) {
          preparedStatement.close()
        }
        if (connection != null) {
          DBConnectionPool.returnConnection(connection)
        }
      }
      if (pause) {
        logger.debug("Feeds thread is paused.")
        Thread.sleep(5000)
      } else {
        logger.debug("Feeds thread resuming ({}ms sleep interval).", sleepInterval)
      }
    }
  }

}
