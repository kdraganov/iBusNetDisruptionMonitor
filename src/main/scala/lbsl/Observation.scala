package lbsl

import java.util.Date
/**
 * Created by Konstantin on 21/01/2015.
 */
class Observation(vehicleId: Integer, timeOfData: Date, tripType: Integer, route: String, lastStop: String, scheduleDeviation: Integer, longitude: Double, latitude: Double, eventId: Integer) {

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
