package lbsl

import java.util.Date

import scala.collection.mutable.ArrayBuffer

/**
 * Created by Konstantin on 22/03/2015.
 */
class Section(private val fromStop: String, private val toStop: String) {

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

  private def calculateDelay(): Unit = {
    observationList = observationList.sortBy(_._2)
    var weightedSum: Double = 0
    var totalWeight: Double = 0
    for (i <- 0 until observationList.length) {
      val weight = getWeight(i)
      totalWeight += weight
      weightedSum += observationList(i)._1 * weight
    }
    if (totalWeight > 0) {
      delay = weightedSum / totalWeight
    } else {
      delay = 0
    }
  }

  private def getWeight(itemOrder: Integer): Double = {
    return itemOrder + 1
  }
}
