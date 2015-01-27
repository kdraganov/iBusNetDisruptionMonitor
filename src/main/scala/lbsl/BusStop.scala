package lbsl

/**
 * Created by Konstantin on 26/01/2015.
 */
class BusStop(//private var code: String,
              private var name: String, private var longitude: Double, private var latitude: Double) {

  //def getCode(): String = code

  def getName(): String = name

  def getLongitude(): Double = longitude

  def getLatitude(): Double = latitude

  //def setCode(code: String) {
  //  this.code = code
  //}

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
