package lbsl

import java.sql.{PreparedStatement, SQLException, Timestamp}
import java.util.Date

import org.slf4j.LoggerFactory
import utility.Environment

import scala.collection.mutable.ArrayBuffer

/**
 * Created by Konstantin on 22/03/2015.
 */
class Section(private val id: Integer, private val sequence: Integer, private val fromStop: String, private val toStop: String) {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private var delay: Double = 0
  private var update: Boolean = false

  private var observationList: ArrayBuffer[Tuple2[Double, Date]] = new ArrayBuffer[Tuple2[Double, Date]]()

  def addObservation(observation: Tuple2[Double, Date]): Unit = {
    observationList.append(observation)
    update = true
  }

  def clear(): Unit = {
    observationList.clear()
    update = true
  }

  def getDelay(): Double = {
    if (update) {
      calculateDelay
    }
    return delay
  }

  def save(): Unit = {

    var preparedStatement: PreparedStatement = null
    val query = "INSERT INTO \"SectionsLostTime\" (\"sectionId\", \"lostTimeInSeconds\", \"timestamp\") VALUES (?, ?, ?);"
    try {
      preparedStatement = Network.connection.prepareStatement(query)
      preparedStatement.setInt(1, id)
      preparedStatement.setDouble(2, delay)
      preparedStatement.setTimestamp(3, new Timestamp(Environment.getLatestFeedDateTime))
      preparedStatement.executeUpdate()
    } catch {
      case e: SQLException => logger.error("Exception:", e)
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close()
      }
    }
  }


  private def calculateDelay(): Unit = {
    observationList = observationList.sortBy(_._2)
    var weightedSum: Double = 0
    var totalWeight: Double = 0
    for (i <- 0 until observationList.length) {
      val weight = getWeight(i)
      totalWeight += weight
      weightedSum += observationList(i)._1 * weight
    }
    delay = 0
    if (totalWeight > 0) {
      delay = weightedSum / totalWeight
    }
  }

  private def getWeight(itemOrder: Integer): Double = {
    return itemOrder + 1
  }
}
