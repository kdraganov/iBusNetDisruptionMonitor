package lbsl

import java.util.Date

import utility.Configuration

/**
 * Created by Konstantin on 21/01/2015.
 */
class Observation() {

  private var vehicleId: Integer = null
  private var timeOfData: Date = null
  private var tripType: Integer = null
  private var contractRoute: String = null
  private var lastStopShortDesc: String = null
  private var scheduleDeviation: Integer = null
  private var longitude: Double = null
  private var latitude: Double = null
  private var eventId: Integer = null

  def getVehicleId: Integer = vehicleId

  def getTimeOfData: Date = timeOfData

  def getTripType: Integer = tripType

  def getContractRoute: String = contractRoute

  def getLastStopShortDesc: String = lastStopShortDesc

  def getScheduleDeviation: Integer = scheduleDeviation

  def getLongitude: Double = longitude

  def getLatitude: Double = latitude

  def getEventId: Integer = eventId


  def init(feed: String): Boolean = {
    val tokens: Array[String] = feed.split(Configuration.getFeedFileDelimiter)
    scheduleDeviation = Integer.parseInt(tokens(Observation.ScheduleDeviation))
    tripType = Integer.parseInt(tokens(Observation.TripType))

    //TODO: REVISE THIS CONDITION
    if (scheduleDeviation == -2147483645 || !lbsl.TripType.isActiveTrip(tripType)) {
      return false
    }
    vehicleId = Integer.parseInt(tokens(Observation.VehicleId))
    longitude = tokens(Observation.Longitude).toDouble
    latitude = tokens(Observation.Latitude).toDouble
    eventId = Integer.parseInt(tokens(Observation.EventId))
    timeOfData = Configuration.getDateFormat().parse(tokens(Observation.TimeOfData))
    contractRoute = tokens(Observation.ContractRoute)
    lastStopShortDesc = tokens(Observation.LastStopShortDesc)
    return true
  }
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

}