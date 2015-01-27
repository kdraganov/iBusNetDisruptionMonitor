package lbsl

/**
 * Created by Konstantin on 26/01/2015.
 */
class Bus(private val id: Integer) {

  private var tripType: Integer = 7
  private var scheduleDeviation: Double = 0
  private var lastStop: String = null

  def getId = id

  def getTripType = tripType

  def getScheduleDeviation = scheduleDeviation

  def getLastStop = lastStop

  def setTripType(tripType: Integer): Unit = {
    this.tripType = tripType
  }

  def calculateScheduleDeviation(): Unit = {
    //TODO: Update schedule deviation value
    scheduleDeviation = 0
  }

  def setLastStop(stop: String): Unit = {
    this.lastStop = stop
  }

}
