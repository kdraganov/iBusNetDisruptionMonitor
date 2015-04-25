package lbsl

import java.util.Date

import utility.Environment

/**
 * Created by Konstantin on 21/01/2015.
 *
 * Class representing an observation of the
 * bus for a given point in time. It is based
 * on the data from the feed files which in turn
 * are based on the transmitted AVL data.
 */
class Observation() extends Ordered[Observation] {

  private var vehicleId: Integer = 0
  private var timeOfData: Date = null
  private var tripType: Integer = 0
  private var tripId: Integer = 0
  private var contractRoute: String = null
  private var lastStopShortDesc: String = null
  private var scheduleDeviation: Integer = 0
  private var longitude: Double = 0
  private var latitude: Double = 0
  private var eventId: Integer = 0
  private var operator: String = null

  def getOperator: String = operator

  def getVehicleId: Integer = vehicleId

  def getTimeOfData: Date = timeOfData

  def getTripId: Integer = tripId

  def getTripType: Integer = tripType

  def getContractRoute: String = contractRoute

  def getLastStopShortDesc: String = lastStopShortDesc

  def getScheduleDeviation: Integer = scheduleDeviation

  def getLongitude: Double = longitude

  def getLatitude: Double = latitude

  def getEventId: Integer = eventId

  /**
   *
   * @return Boolean - true if the observation has valid
   *         values for the parameters, false otherwise
   */
  def isValid: Boolean = {
    if (scheduleDeviation == Observation.NegativeIntegerError) {
      return false
    }
    return true
  }

  /**
   *
   * @param feed String - the line representing the observation in the feed file.
   * @param companyOperator String - the bus operator company
   * @return Boolean - true if the observation is valid, meaning it can
   *         be used by the system, otherwise false
   */
  def init(feed: String, companyOperator: String): Boolean = {
    val tokens: Array[String] = feed.split(Environment.getFeedFileRegex)
    scheduleDeviation = Integer.parseInt(tokens(Observation.ScheduleDeviation))
    tripType = Integer.parseInt(tokens(Observation.TripType))

    if (scheduleDeviation == Observation.NegativeIntegerError || !lbsl.TripType.isActiveTrip(tripType)) {
      return false
    }

    vehicleId = Integer.parseInt(tokens(Observation.VehicleId))
    longitude = tokens(Observation.Longitude).toDouble
    latitude = tokens(Observation.Latitude).toDouble
    eventId = Integer.parseInt(tokens(Observation.EventId))
    timeOfData = Environment.getDateFormat().parse(tokens(Observation.TimeOfData))
    contractRoute = tokens(Observation.ContractRoute)
    lastStopShortDesc = tokens(Observation.LastStopShortDesc)
    operator = companyOperator
    tripId = Integer.parseInt(tokens(Observation.TripId))
    return true
  }

  def compare(that: Observation) = this.getTimeOfData compareTo that.getTimeOfData
}

object Observation {

  final val VehicleId: Integer = 0
  final val BonnetCode: Integer = 1
  final val RegistrationNumber: Integer = 2
  final val TimeOfData: Integer = 3
  final val BaseVersion: Integer = 4
  final val BlockNumber: Integer = 5
  final val TripId: Integer = 6
  final val LBSLTripNumber: Integer = 7
  final val TripType: Integer = 8
  final val ContractRoute: Integer = 9
  final val LastStopShortDesc: Integer = 10
  final val ScheduleDeviation: Integer = 11
  final val Longitude: Integer = 12
  final val Latitude: Integer = 13
  final val EventId: Integer = 14
  final val Duration: Integer = 15

  final val NegativeIntegerError: Integer = -2147483645
}