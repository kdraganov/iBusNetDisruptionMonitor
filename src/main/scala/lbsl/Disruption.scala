package lbsl

import java.util.Date

/**
 * Created by Konstantin on 04/02/2015.
 */
class Disruption {

  val timeFirstDetected: Date = null
  val busRoute: String = null
  val delayDetected: Double = 0
  val sectionStart: String = null
  val sectionEnd: String = null
  val clearedAt: Date = null
  val trend: Integer = 0

}

object Disruption {

  //  NRT delay classifications:
  //  Moderate - 0 - 20 min
  //  Serious - 21 - 40 min
  //  Severe - 41 - 60 min

  final val SectionModerate: Integer = 10
  final val SectionSerious: Integer = 20
  final val SectionSevere: Integer = 40

  final val RouteSerious: Integer = 30
  //40
  final val RouteSevere: Integer = 50 //60

  final val TrendImproving = 1
  final val TrendStable = 0
  final val TrendWorsening = -1
}
