package lbsl

import java.sql.{PreparedStatement, SQLException, Timestamp}
import java.util.Date

import _root_.utility.Environment
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
 * Created by Konstantin on 22/03/2015.
 */
class Section(private val id: Integer, private val sequence: Integer, private val fromStop: String, private val toStop: String) {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private var delay: Double = 0
  private var update: Boolean = false
  private var latestObservationDate: Date = null

  private var observationList: ArrayBuffer[Tuple2[Double, Date]] = new ArrayBuffer[Tuple2[Double, Date]]()

  def getLatestObservationTime(): Date = latestObservationDate

  def addObservation(observation: Tuple2[Double, Date]): Unit = {
    observationList.append(observation)
    update = true
  }

  def clear(): Unit = {
    observationList.clear()
    update = true
    delay = 0
  }

  def getDelay(): Double = {
    if (update && observationList.size > 0) {
      observationList = observationList.sortBy(_._2)
      latestObservationDate = observationList.last._2
      calculateDelay
    }
    return delay
  }


  def save(date: Date): Unit = {
    val timestamp = new Timestamp(date.getTime)
    var preparedStatement: PreparedStatement = null
    val query = "INSERT INTO \"SectionsLostTime\" (\"sectionId\", \"lostTimeInSeconds\", \"timestamp\", \"numberOfObservations\") VALUES (?, ?, ?, ?);"
    try {
      preparedStatement = Environment.getDBTransaction.connection.prepareStatement(query)
      preparedStatement.setInt(1, id)
      preparedStatement.setDouble(2, delay)
      preparedStatement.setTimestamp(3, timestamp)
      preparedStatement.setInt(4, observationList.size)
      preparedStatement.executeUpdate()
    } catch {
      case e: SQLException => logger.error("Exception: with query ({}) ", preparedStatement.toString, e)
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close()
      }
    }
  }

  private def calculateDelay(): Unit = {
    EMA()
  }

  private def calculateObservationValue(value: Double, weight: Double): Double = {
    return value * weight
  }

  private def getWeight(itemIndex: Integer): Double = {
    return itemIndex + 1
  }

  //Weighted moving average of the data
  private def WMA(): Unit = {
    var weightedSum: Double = 0
    var totalWeight: Double = 0
    for (i <- 0 until observationList.length) {
      //      TODO: REMOVE  logger.debug("Observation index {}, time {} and time loss {}", Array[Object](i.toString, Environment.getDateFormat().format(observationList(i)._2), observationList(i)._1.toString))
      val weight = getWeight(i)
      totalWeight += weight
      weightedSum += calculateObservationValue(observationList(i)._1, weight)
    }
    //    TODO: REMOVE logger.debug("TotalWeight is {} and weightedSum is {}.", totalWeight, weightedSum)
    delay = 0
    if (totalWeight > 0) {
      delay = weightedSum / totalWeight
    }
  }

  //Exponential moving average
  private def EMA(): Unit = {
    var forecast = observationList(0)._1
    for (i <- 1 until observationList.length) {
      forecast = (Section.ALPHA * observationList(i)._1) + ((1 - Section.ALPHA) * forecast)
    }
    delay = forecast
  }

}

object Section {
  private final val ALPHA = 0.5
  private final val WMA_NUMBER_OF_OBSERVATIONS = 10
}