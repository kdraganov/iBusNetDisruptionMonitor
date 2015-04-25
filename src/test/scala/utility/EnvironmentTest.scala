package scala.utility

import java.io.File
import java.sql.PreparedStatement
import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import _root_.utility.{DBConnectionPool, Environment}

import scala.main.UnitSpec


/**
 * Created by Konstantin on 12/03/2015.
 */
class EnvironmentTest extends UnitSpec {

  before {
    DBConnectionPool.createPool(dbConnectionSettingsPath)
    Environment.init()
    Environment.setLatestFeedTimeOfData(new Date(0))
  }

  after {
    DBConnectionPool.close()
  }

  //Below values may change - check db
  test("Getters") {
    assert(Environment.getMovingAverageWindowSize == 5)
    assert(Environment.isSystemMonitorActive == true)
    assert(Environment.getLatestFeedTimeOfData == new Date(0))
    assert(Environment.getFeedDirectory == new File("E:\\Workspace\\iBusNetTestDirectory"))
    assert(Environment.getFeedFilePrefix == "CC_")
    assert(Environment.getFeedFileSuffix == ".csv")
    assert(Environment.getFeedFileDelimiter == ";")
    assert(Environment.getFeedFileHeader == true)
    assert(Environment.getDataValidityTimeInMinutes == 90)
    assert(Environment.getProcessedDirectory == new File("E:\\Workspace\\iBusNetTestDirectory\\ProcessedFiles"))
    assert(Environment.getMonitorThreadSleepInterval == 500)
    assert(Environment.getSectionMediumThreshold == 1000)
    assert(Environment.getSectionSeriousThreshold == 2400)
    assert(Environment.getSectionSevereThreshold == 3600)
    assert(Environment.getSectionMinThreshold == 180)
    assert(Environment.getRouteSevereThreshold == 3600)
    assert(Environment.getRouteSeriousThreshold == 2400)
    assert(Environment.getDateFormat == new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"))
  }

  test("LatestFeedTimeOfData") {
    assert(Environment.getLatestFeedTimeOfData == new Date(0))
    val calendar = Calendar.getInstance()

    calendar.add(Calendar.DATE, -1)
    Environment.setLatestFeedTimeOfData(calendar.getTime)
    assert(Environment.getLatestFeedTimeOfData == calendar.getTime)

    calendar.add(Calendar.DATE, +1)
    Environment.setLatestFeedTimeOfData(calendar.getTime)
    assert(Environment.getLatestFeedTimeOfData == calendar.getTime)
  }

  test("RegEx") {
    val testColumnValuesArrays: Array[Array[String]] = Array[Array[String]](
      Array[String]("ColumnA", "ColumnB", "ColumnC", "ColumnD", "ColumnE", "ColumnF", "ColumnG"),
      Array[String]("ColumnA", "ColumnB", "ColumnC", "\"ColumnD;\"", "ColumnE", "ColumnF", "ColumnG"),
      Array[String]("\";ColumnA;\"", "ColumnB", "\"Col;umnC\"", "\";ColumnD;\"", "ColumnE", ",ColumnF,", "\"Co;lumn;G\""),
      Array[String]("ColumnA,", "ColumnB", "Column,C", "ColumnD4", "ColumnE", "ColumnF", "ColumnG2"),
      Array[String]("ColumnA32,", "ColumnB!", "ColumnC?", "ColumnD4", "ColumnE", "ColumnF", "ColumnG#")
    )
    for (columnValues <- testColumnValuesArrays) {
      val testString = columnValues.mkString(Environment.getFeedFileDelimiter)
      val testColumnValues = testString.split(Environment.getFeedFileRegex)
      assert(columnValues.length == testColumnValues.length)
      for (i <- 0 until columnValues.length) {
        assert(columnValues(i) == testColumnValues(i))
      }
    }
  }

  test("Update") {
    val oldDataValidityTimeInMinutes = Environment.getDataValidityTimeInMinutes
    val newDataValidityTimeInMinutes = 120
    var preparedStatement: PreparedStatement = null
    val connection = DBConnectionPool.getConnection()
    val query = "UPDATE \"EngineConfigurations\" SET value=? WHERE key=?"
    preparedStatement = connection.prepareStatement(query)
    preparedStatement.setInt(1, newDataValidityTimeInMinutes)
    preparedStatement.setString(2, "dataValidityTimeInMinutes")
    preparedStatement.executeUpdate()
    preparedStatement.close()

    assert(Environment.getDataValidityTimeInMinutes == oldDataValidityTimeInMinutes)
    Environment.update()
    assert(Environment.getDataValidityTimeInMinutes == newDataValidityTimeInMinutes)

    preparedStatement = connection.prepareStatement(query)
    preparedStatement.setInt(1, oldDataValidityTimeInMinutes)
    preparedStatement.setString(2, "dataValidityTimeInMinutes")
    preparedStatement.executeUpdate()
    preparedStatement.close()
    DBConnectionPool.returnConnection(connection)

    assert(Environment.getDataValidityTimeInMinutes == newDataValidityTimeInMinutes)
    Environment.update()
    assert(Environment.getDataValidityTimeInMinutes == oldDataValidityTimeInMinutes)
  }

}