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

  def getLatestObservationTime(): Date = {
    isUptodate
    latestObservationDate
  }

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
    if (!isUptodate) {
      calculateDelay
    }
    //    if (update && observationList.size > 1) {
    //      observationList = observationList.sortBy(_._2)
    //      latestObservationDate = observationList.last._2
    //      calculateDelay
    //    }
    return delay
  }

  private def isUptodate(): Boolean = {
    if (update && observationList.size > 1) {
      observationList = observationList.sortBy(_._2)
      latestObservationDate = observationList.last._2
      return false
    } else {
      latestObservationDate = null
    }
    return true
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
    WMA()
    //    doubleExponentialSmoothing()
  }

  //Weighted moving average of the data
  private def WMA(windowSize: Integer = Environment.getMovingAverageWindowSize()): Unit = {
    var weightedSum: Double = 0
    var totalWeight: Double = 0
    observationList.remove(0, Math.max(observationList.length - windowSize, 0))
    for (i <- 0 until observationList.length) {
      val weight = getWeight(i)
      totalWeight += weight
      weightedSum += Math.max(observationList(i)._1 * weight, 0)
    }
    delay = 0
    if (totalWeight > 0) {
      delay = weightedSum / totalWeight
    }
  }

  private def getWeight(itemIndex: Integer): Double = {
    return itemIndex + 1
    //    return Math.pow(2, itemIndex + 1)
  }

  //Exponential moving average
  private def singleExponentialSmoothing(): Unit = {
    var forecast = observationList(0)._1
    for (i <- 1 until observationList.length) {
      forecast = (Section.ALPHA * observationList(i)._1) + ((1 - Section.ALPHA) * forecast)
    }
    delay = forecast
  }

  private def doubleExponentialSmoothing(): Unit = {
    val alpha = 0.6
    val beta = 0.8
    var prevConstant = observationList(0)._1
    var prevTrend = observationList(0)._1
    for (i <- 1 until observationList.length) {
      val constant = alpha * observationList(i)._1 + (1 - alpha) * (prevConstant + prevTrend)
      prevTrend = beta * (constant - prevConstant) + (1 - beta) * prevTrend
      prevConstant = constant
    }
    delay = prevConstant + prevTrend
    //    val smoothedConstant = alpha*currentVal + (1 - alpha) *(prevSmoothedConstant + prevSmoothedTrend)
    //    val smoothedTrend = beta*(smoothedConstant - prevSmoothedConstant) + (1 - beta)*prevSmoothedTrend
    //    val forecast = smoothedConstant + smoothedTrend
  }
}

object Section {
  private final val ALPHA = 0.65
}