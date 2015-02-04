package lbsl

/**
 * Created by Konstantin on 26/01/2015.
 */
class BusStop(
               private var name: String,
               private var longitude: Double,
               private var latitude: Double) {


  def getName(): String = name

  def getLongitude(): Double = longitude

  def getLatitude(): Double = latitude

  def setName(name: String) {
    this.name = name
  }

  def setLongitude(longitude: Double) {
    this.longitude = longitude
  }

  def setLatitude(latitude: Double) {
    this.latitude = latitude
  }
}

object BusStop {

  final val StopCodeLBSL: Integer = 0
  final val BusStopCode: Integer = 1
  final val NaptanAtco: Integer = 2
  final val StopName: Integer = 3
  final val LocationEasting: Integer = 4
  final val LocationNorthing: Integer = 5
  final val Heading: Integer = 6
  final val StopArea: Integer = 7
  final val VirtualBusStop: Integer = 8
  final val Latitude: Integer = 9
  final val Longitude: Integer = 10
}