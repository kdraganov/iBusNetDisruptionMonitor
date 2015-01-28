package lbsl

import java.util.Date
/**
 * Created by Konstantin on 21/01/2015.
 */
class Observation(
                   private val vehicleId: Integer,
                   private val timeOfData: Date,
                   private val tripType: Integer,
                   private val route: String,
                   private val lastStop: String,
                   private val scheduleDeviation: Integer,
                   private val longitude: Double,
                   private val latitude: Double,
                   private val eventId: Integer) {

  def getVehicleId : Integer = vehicleId
  def getTimeOfData : Date = timeOfData
  def getTripType : Integer = tripType
  def getRoute : String = route
  def getLastStop : String = lastStop
  def getScheduleDeviation : Integer = scheduleDeviation
  def getLongitude : Double = longitude
  def getLatitude : Double = latitude
  def getEventId : Integer = eventId

}
