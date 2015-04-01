package scala.utility

import java.sql.{Connection, SQLException}

import _root_.utility.DBConnectionPool
import org.slf4j.LoggerFactory

/**
 * Created by Konstantin on 01/04/2015.
 */
class DBTransaction {

  var connection: Connection = null
  val logger = LoggerFactory.getLogger(getClass().getSimpleName)

  def getConnection = connection

  def begin(): Unit = {
    try {
      connection = DBConnectionPool.getConnection()
      connection.setAutoCommit(false)
    } catch {
      case e: SQLException => LoggerFactory.getLogger(getClass().getSimpleName).error("Exception:", e)
        logger.error("Terminating application.")
    }
  }

  def commit(): Unit = {
    try {
      connection.commit()
    } catch {
      case e: SQLException => logger.error("Exception:", e)
        logger.error("Terminating application.")
        connection.rollback()
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
    connection = null
  }

  def rollback(): Unit = {
    try {
      connection.rollback()
    } catch {
      case e: SQLException => logger.error("Exception:", e)
        logger.error("Terminating application.")
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
    connection = null
  }

}
