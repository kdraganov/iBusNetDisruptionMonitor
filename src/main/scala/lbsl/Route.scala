package lbsl

import java.util.Date

import org.slf4j.LoggerFactory
import utility.Configuration

import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.concurrent.duration._

/**
 * Created by Konstantin on 26/01/2015.
 * Direction either outbound (1) or inbound (2)
 */

class Route(private val contractRoute: String) extends Runnable {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private val busStopSequence: Array[ArrayBuffer[String]] = Array(new ArrayBuffer[String](), new ArrayBuffer[String]())
  private val busesOnRoute: HashMap[Integer, ArrayBuffer[Observation]] = new HashMap[Integer, ArrayBuffer[Observation]]()

  //Section WMA delay in minutes
  private var sectionWMADelays: Array[Array[Double]] = null
  private val sections: Array[Array[ArrayBuffer[Tuple2[Double, Date]]]] = new Array[Array[ArrayBuffer[Tuple2[Double, Date]]]](2)

  //private val totalDelay: Array[Double] = Array[Double](0, 0)
  private val disruptionList: Array[ArrayBuffer[Disruption]] = new Array[ArrayBuffer[Disruption]](2)

  private final val MinTimeLossSeconds: Integer = 90

  override
  def run(): Unit = {
    //MOVE THIS TO INIT METHOD
    disruptionList(0) = new ArrayBuffer[Disruption]
    disruptionList(1) = new ArrayBuffer[Disruption]

    clearSections()
    //we need at least two observations
    for ((busId, observationList) <- busesOnRoute if observationList.size > 1) {
      // logger.trace("Route {} observation list size = {} ", getContractRoute, observationList.size())
      var prevObservation = observationList(0)
      for (i <- 1 until observationList.size) {
        val observation = observationList(i)
        val scheduleDeviationDifference = observation.getScheduleDeviation - prevObservation.getScheduleDeviation
        //not interested if bus has gained time
        if (scheduleDeviationDifference > 0) {
          calculateChange(prevObservation, observation, scheduleDeviationDifference)
        }
        prevObservation = observation
      }
    }
    updateDisruptions()
    //    calculateWMASectionDelay()
    //    calculateTotalDisruption()
  }

  def getDisruptions(run: Integer): ArrayBuffer[Disruption] ={
    return disruptionList(getIndex(run))
  }
  /**
   *
   * @return boolean true if there are active (e.g. have received reading in the past 1h or so) buses on the route
   *         false otherwise
   */
  def isRouteActive(): Boolean = {
    updateObservations()
    !busesOnRoute.isEmpty
  }


  def generateSections(): Unit = {
    for (i <- 0 until sections.length) {
      //TODO:Init only the direction the route has - waste of space otherwise
      sections(i) = new Array[ArrayBuffer[Tuple2[Double, Date]]](Math.max(busStopSequence(i).size - 1, 0))
      for (x <- 0 until busStopSequence(i).size - 1) {
        sections(i)(x) = new ArrayBuffer[Tuple2[Double, Date]]()
      }
    }
  }

  def addObservation(observation: Observation): Unit = {
    val observationList = busesOnRoute.getOrElse(observation.getVehicleId, new ArrayBuffer[Observation]())
    observationList.append(observation)
    busesOnRoute.put(observation.getVehicleId, observationList)
  }

  def addBusStop(busStopCode: String, direction: Integer): Unit = {
    busStopSequence(direction - 1).append(busStopCode)
  }

  def addBusStop(busStopCode: String, direction: Integer, sequence: Integer): Unit = {
    val index = sequence - 1
    val directionIndex = direction - 1
    if (index > busStopSequence(directionIndex).size) {
      busStopSequence(directionIndex).insert(busStopSequence(directionIndex).size, busStopCode)
    } else if (index < 0) {
      busStopSequence(directionIndex).insert(0, busStopCode)
    } else {
      busStopSequence(directionIndex).insert(index, busStopCode)
    }
  }

  def getInboundStopSequence(): ArrayBuffer[String] = {
    return busStopSequence(getInboundIndex) //.toArray(new Array[String](busStopSequence(getInboundIndex).size))
  }

  def getOutboundStopSequence(): ArrayBuffer[String] = {
    return busStopSequence(getOutboundIndex) //.toArray(new Array[String](busStopSequence(getOutboundIndex).size))
  }

  def getContractRoute = contractRoute

  def getTotalDisruptionTime(direction: Integer): Integer = 99999 //totalDelay(getIndex(direction))

  /**
   * Sort observation and remove old elements and remove observation list from map if no observations
   */
  private def updateObservations(): Unit = {
    for ((busId, observationList) <- busesOnRoute) {
      val sortedObservationList = observationList.sortBy(x => x.getTimeOfData)
      //Collections.sort(observationList)
      // difference in MILLISECONDS
      var timeDiff = Duration(Configuration.getLatestFeedTime - sortedObservationList(0).getTimeOfData.getTime, MILLISECONDS)
      while (timeDiff.toHours > Configuration.getDataValidityTimeInHours && sortedObservationList.size > 0) {
        timeDiff = Duration(Configuration.getLatestFeedTime - sortedObservationList.remove(0).getTimeOfData.getTime, MILLISECONDS)
      }
      if (sortedObservationList.isEmpty) {
        //logger.debug("Bus with id {} has not been active on route {} in the last hour.", busId, contractRoute)
        busesOnRoute.remove(busId)
      } else {
        busesOnRoute.put(busId, sortedObservationList)
      }
    }
  }

  private def getDirectionAndStopIndexes(prevLastStop: String, lastStop: String): Tuple3[Integer, Integer, Integer] = {
    //TODO: need to add code to handle the scenario of when the stops are from two different directions
    //TODO: what if the stops are the same - which direction?
    // need to get start and end of section and distribute the difference
    for (i <- 0 until busStopSequence.length) {
      val prevLastStopIndex = busStopSequence(i).indexOf(prevLastStop)
      val lastStopIndex = busStopSequence(i).indexOf(lastStop)
      if (lastStopIndex >= prevLastStopIndex && prevLastStopIndex > -1) {
        return new Triple[Integer, Integer, Integer](i, prevLastStopIndex, lastStopIndex)
      }
    }
    //logger.debug("Bus stop with LBSL id {} or {} cannot be found for route {}.", Array[Object](prevLastStop, lastStop, getContractRoute))

    return null
  }

  private def calculateChange(prevObservation: Observation, observation: Observation, scheduleDeviationDifference: Double): Unit = {
    //JUST FOR TESTING
    //    if (busStopSequence(0).indexOf(prevObservation.getLastStopShortDesc) == -1 && busStopSequence(1).indexOf(prevObservation.getLastStopShortDesc) == -1) {
    //      logger.debug("Missing stop {} from file {} ", prevObservation.getLastStopShortDesc, prevObservation.getOperator)
    //      //MissingData.addMissingStop(prevObservation.getLastStopShortDesc, prevObservation.getContractRoute, prevObservation.getOperator)
    //    }
    //    if (busStopSequence(0).indexOf(observation.getLastStopShortDesc) == -1 && busStopSequence(1).indexOf(observation.getLastStopShortDesc) == -1) {
    //      logger.debug("Missing stop {} from file {} ", observation.getLastStopShortDesc, observation.getOperator)
    //      //MissingData.addMissingStop(observation.getLastStopShortDesc, observation.getContractRoute, observation.getOperator)
    //    }
    //END TESTING SECTION

    val temp = getDirectionAndStopIndexes(prevObservation.getLastStopShortDesc, observation.getLastStopShortDesc)
    var direction: Integer = 0
    var prevLastStopIndex: Integer = 0
    var lastStopIndex: Integer = 0
    if (temp != null) {
      direction = temp._1
      prevLastStopIndex = temp._2
      lastStopIndex = temp._3
    }
    // TODO: consider readings not from the same direction
    if ((lastStopIndex - prevLastStopIndex - 1) > 0) {
      val scheduleDeviationSectionDistribution = scheduleDeviationDifference / (lastStopIndex - prevLastStopIndex + 1)
      for (i <- prevLastStopIndex.intValue() to Math.min(lastStopIndex, busStopSequence(direction).size - 2)) {
        sections(direction)(i).append((scheduleDeviationSectionDistribution, observation.getTimeOfData))
      }
    }

  }

  private def clearSections(): Unit = {
    for (section <- sections) {
      for (buffer <- section) {
        buffer.clear()
      }
    }
  }

  //  private def calculateTotalDisruption(): Unit = {
  //    for (i <- 0 until totalDelay.length) {
  //      var sum: Double = 0
  //      for (section: Integer <- sectionWMADelays(i)) {
  //        sum += section
  //      }
  //      totalDelay(i) = sum
  //    }
  //  }

  private def updateDisruptions() {
    calculateWMASectionDelay()
    for (run <- 0 until sectionWMADelays.length) {

      //SIMPLE TEST FOR DIVERSIONS
//      if(busesOnRoute.size > 5){
//        for (i <- 4 until sectionWMADelays(run).length) {
//          val one = sectionWMADelays(run)(i - 4)
//          val two = sectionWMADelays(run)(i - 3)
//          val three = sectionWMADelays(run)(i - 2)
//          val four = sectionWMADelays(run)(i - 1)
//          if (one.equals(two) && three.equals(four) && one.equals(three)) {
//            logger.debug("POSSIBLE DIVERSION route {} between {} and {}", Array[Object](getContractRoute, busStopSequence(run)(i - 4), busStopSequence(run)(i - 1)))
//          }
//        }
//      }
      var sectionStartStopIndex: Integer = null
      var disruptionSeconds: Double = 0
      for (i <- 0 until sectionWMADelays(run).length) {
        if (sectionWMADelays(run)(i) > MinTimeLossSeconds) {
          //continue or start of disrupted section
          if (sectionStartStopIndex == null) {
            sectionStartStopIndex = i
          } else {
            // large section - possible diversion
            if (i - sectionStartStopIndex > Configuration.getMaxSectionLength) {
              if (disruptionSeconds < Configuration.getSectionMediumThreshold) {
                //not interested - shift start of the section
                disruptionSeconds -= sectionWMADelays(run)(sectionStartStopIndex)
                sectionStartStopIndex += 1
              } else {
                addDisruption(run, sectionStartStopIndex, i, disruptionSeconds)
                sectionStartStopIndex = null
                disruptionSeconds = 0
              }
            }
          }
          disruptionSeconds += sectionWMADelays(run)(i)
        } else {
          if (sectionStartStopIndex != null) {
            // end of sectionDisruption
            if (disruptionSeconds >= Configuration.getSectionMediumThreshold) {
              addDisruption(run, sectionStartStopIndex, i, disruptionSeconds)
            }
            sectionStartStopIndex = null
            disruptionSeconds = 0
          }
        }
      }
    }
    //check and get disrupted section
    //for each detected disruption check if it sub/super set of previously detected disruptions
  }

  private def addDisruption(run: Integer, sectionStartStopIndex: Integer, sectionEndStopIndex: Integer, delaySeconds: Double): Unit = {
    val disruption: Disruption = new Disruption(busStopSequence(run)(sectionStartStopIndex), busStopSequence(run)(sectionEndStopIndex), delaySeconds)
    disruptionList(run).append(disruption)
  }

  private def calculateWMASectionDelay(): Unit = {
    sectionWMADelays = Array(new Array[Double](sections(getOutboundIndex).size), new Array[Double](sections(getInboundIndex).size))
    for (direction <- 0 until sectionWMADelays.length) {
      //for each direction
      for (segmentIndex <- 0 until sections(direction).length) {
        val segment = sections(direction)(segmentIndex)
        if (segment != null) {
          // need to sort it by time
          val segmentSorted = segment.sortBy(_._2)
          var weightedSum: Double = 0
          var totalWeight: Double = 0
          for (i <- 0 until segmentSorted.length) {
            val weight = getWeight(i)
            totalWeight += weight
            weightedSum += segmentSorted(i)._1 * weight
          }
          sectionWMADelays(direction)(segmentIndex) = 0
          if (totalWeight > 0) {
            sectionWMADelays(direction)(segmentIndex) = weightedSum / totalWeight
          }
        }
      }
    }
  }

  private def getWeight(itemOrder: Integer): Double = {
    return itemOrder + 1
  }

  //  def getDisruptedSections(direction: Integer): ArrayBuffer[Tuple3[String, String, Double]] = {
  //    val disruptedSections = new ArrayBuffer[Tuple3[String, String, Double]]()
  //    var stopA: String = null
  //    var stopB: String = null
  //    var disruption: Double = 0
  //    //TODO: consider only small sections e.g. 3-5 stops - probably best to link it to the feed interval
  //    for (i <- 0 until sectionWMADelays(getIndex(direction)).length) {
  //      if (sectionWMADelays(getIndex(direction))(i) > 60 * 2) {
  //        //continue or start of disrupted section
  //          stopA = busStopSequence(getIndex(direction))(i)
  //        }
  //        disruption += sectionWMADelays(getIndex(direction))(i)
  //      } else if (stopA != null) {
  //        // end of sectionDisruption
  //        stopB = busStopSequence(getIndex(direction))(i)
  //        if (disruption > 10 * 60) {
  //          disruptedSections.append((stopA, stopB, disruption))
  //        }
  //        stopA = null
  //        disruption = 0
  //      }
  //    }
  //    return disruptedSections
  //  }

  private def getIndex(direction: Integer): Integer = {
    if (direction == Route.Inbound) {
      return getInboundIndex
    }
    return getOutboundIndex
  }

  private def getInboundIndex = Route.Inbound - 1

  private def getOutboundIndex = Route.Outbound - 1


}

object Route {

  final val Outbound = 1
  final val Inbound = 2

  final val Route: Integer = 0
  final val Run: Integer = 1
  final val Sequence: Integer = 2
  final val StopCodeLBSL: Integer = 3
  final val BusStopCode: Integer = 4
  final val NaptanAtco: Integer = 5
  final val StopName: Integer = 6
  final val LocationEasting: Integer = 7
  final val LocationNorthing: Integer = 8
  final val BusStopHeading: Integer = 9
  final val VirtualBusStop: Integer = 10

  final val NumberOfFields: Integer = 11

  def getDirectionString(direction: Integer): String = {
    if (direction == Inbound) {
      return "Inbound"
    }
    return "Outbound"
  }
}