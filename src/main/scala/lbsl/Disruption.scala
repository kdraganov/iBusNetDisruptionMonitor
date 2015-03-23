package lbsl

import java.util.{Calendar, Date}

/**
 * Created by Konstantin on 04/02/2015.
 */
class Disruption(private var sectionStartIndex: Integer,
                 private var sectionEndIndex: Integer,
                 private var sectionStart: String,
                 private var sectionEnd: String,
                 private var delaySeconds: Double,
                 private val timeFirstDetected: Date = Calendar.getInstance().getTime()) {

  private var clearedAt: Date = null
  private var trend: Integer = Disruption.TrendWorsening

  def getSectionStartBusStop: String = sectionStart

  def getSectionEndBusStop: String = sectionEnd

  def getDelay: Integer = {
    return delaySeconds.toInt
  }

  def getDelayInMinutes: Integer = {
    return getDelay / 60
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

  def update(newSectionStartIndex: Integer, newSectionEndIndex: Integer, newSectionStart: String, newSectionEnd: String, newDelaySeconds: Double): Unit = {
    //TODO: Consider the section size for the trend as well
    val oldSectionSize = this.sectionEndIndex - this.sectionStartIndex
    this.sectionStartIndex = newSectionStartIndex
    this.sectionEndIndex = newSectionEndIndex
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


  def equals(that: Disruption): Boolean = {
    if (this.sectionStartIndex == that.sectionStartIndex ||
      this.sectionEndIndex == that.sectionEndIndex) {
      return true
    }
    //    TODO:Extend this to capture all cases
    return false
  }

  def clear(): Unit = {
    //TODO: update in DB that this has cleared
  }

  def save(): Unit = {
    //TODO: sava or update in DB
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
