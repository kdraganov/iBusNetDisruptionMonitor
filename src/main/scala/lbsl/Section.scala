package lbsl

import java.sql.{PreparedStatement, SQLException, Timestamp}
import java.util.Date

import _root_.utility.Environment
import org.slf4j.LoggerFactory

import scala.collection.mutable.{ArrayBuffer, HashMap}

/**
 * Created by Konstantin on 22/03/2015.
 *
 * Class representation of a section (a consecutive
 * pair of stops along a given route and run).
 */
class Section(private val id: Integer, private val sequence: Integer, private val fromStop: String, private val toStop: String) {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private var delay: Double = 0
  private var update: Boolean = false
  private var latestObservationDate: Date = null

  private var observationList: ArrayBuffer[Tuple2[Double, Date]] = new ArrayBuffer[Tuple2[Double, Date]]()
  private val busObservationMap: HashMap[Integer, Integer] = new HashMap[Integer, Integer]()

  def getLatestObservationTime(): Date = {
    isUptodate
    latestObservationDate
  }

  /**
   *
   * @param vehicleId Integer - the unique vehicle id
   * @param lostTime Double - the lost time
   * @param date Date - the date time of the later observation
   */
  def addObservation(vehicleId: Integer, lostTime: Double, date: Date): Unit = {
    val index = busObservationMap.getOrElse(vehicleId, null)
    if (index == null) {
      busObservationMap.put(vehicleId, observationList.length)
      observationList.append(Tuple2[Double, Date](lostTime, date))
    } else {
      val temp = observationList(index)
      if (date.after(temp._2)) {
        observationList(index) = Tuple2[Double, Date](temp._1 + lostTime, date)
      } else {
        observationList(index) = Tuple2[Double, Date](temp._1 + lostTime, temp._2)
      }
    }
    update = true
  }

  def clear(): Unit = {
    busObservationMap.clear()
    observationList.clear()
    update = true
    delay = 0
  }

  def getDelay(): Double = {
    if (!isUptodate) {
      calculateDelay
    }
    return delay
  }

  private def isUptodate(): Boolean = {
    if (update && observationList.size > 0) {
      observationList = observationList.sortBy(_._2)
      latestObservationDate = observationList.last._2
      return false
    } else {
      latestObservationDate = null
    }
    return true
  }

  /**
   * Saving the section time loss for the section in the database.
   * @param date Date
   */
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
    //Here we can use Exponentially Weighted Moving Average instead
    //    doubleExponentialSmoothing()
  }

  /**
   * Calculates the WMA for the time series data
   * @param windowSize Integer - the window size of the moving average
   */
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

  /**
   *
   * @param itemIndex Integer - the index representing the sequence of the data point
   * @return Double - the weight for the given index
   */
  private def getWeight(itemIndex: Integer): Double = {
    return itemIndex + 1
    //    return Math.pow(2, itemIndex + 1)
  }

  //Bellow two methods used only for testing and comparison
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