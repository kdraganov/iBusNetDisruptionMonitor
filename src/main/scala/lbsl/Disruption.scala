package lbsl

import java.util.{Calendar, Date}

/**
 * Created by Konstantin on 04/02/2015.
 */
class Disruption(var sectionStart: String, var sectionEnd: String, var delaySeconds: Double, private val timeFirstDetected: Date = Calendar.getInstance().getTime()) {

  private var clearedAt: Date = null
  private var trend: Integer = Disruption.TrendWorsening

  def getSectionStartBusStop: String = sectionStart

  def getSectionEndBusStop: String = sectionEnd

  def getDelayInMinutes: Integer = {
    return (delaySeconds / 60).toInt
  }

  def getTimeFirstDetected: Date = timeFirstDetected

  def getTrend: Integer = trend

  def isCleared: Boolean = {
    if (clearedAt != null) {
      return true
    }
    return false
  }

  def getClearedTime: Date = clearedAt

  def setClearTime(date: Date): Unit = {
    clearedAt = date
  }

  def update(newSectionStart: String, newSectionEnd: String, newDelaySeconds: Double): Unit = {
    this.sectionStart = sectionStart
    this.sectionEnd = sectionEnd
    if (newDelaySeconds > delaySeconds) {
      trend = Disruption.TrendWorsening
    } else if (newDelaySeconds < delaySeconds) {
      trend = Disruption.TrendImproving
    } else {
      trend = Disruption.TrendStable
    }
    delaySeconds = newDelaySeconds
  }

}

object Disruption {

  //  NRT delay classifications:
  //  Moderate - 0 - 20 min
  //  Serious - 21 - 40 min
  //  Severe - 41 - 60 min

  //  final val SectionModerate: Integer = 10
  //  final val SectionSerious: Integer = 20
  //  final val SectionSevere: Integer = 40
  //
  //  final val RouteSerious: Integer = 30
  //  //40
  //  final val RouteSevere: Integer = 50 //60

  final val TrendImproving = 1
  final val TrendStable = 0
  final val TrendWorsening = -1
}
