package scala.lbsl

import java.util.{Calendar, Date}

import _root_.lbsl.Section
import _root_.utility.{DBConnectionPool, Environment}

import scala.main.UnitSpec

/**
 * Created by Konstantin on 12/03/2015.
 */
class SectionTest extends UnitSpec {

  //Need to update the data if window size and weights change - currently assumed window size = 5 and normal sequential weights
  private val observations: Array[Tuple3[Integer, Double, Date]] = new Array[Tuple3[Integer, Double, Date]](5)
  private val id = 1
  private val sequence = 1
  private val fromStop = "14456"
  private val toStop = "29844"
  //    1;"1";1;"14456";"29844";1
  private var section: Section = null

  before {
    DBConnectionPool.createPool(dbConnectionSettingsPath)
    Environment.init()
    val observationValues: Array[Double] = Array[Double](89, 856, 1349, 533, 579) //expected WMA 725
    val calendar = Calendar.getInstance()
    for (i <- 0 until observations.length) {
      observations(i) = new Tuple3(i, observationValues(i), calendar.getTime)
      calendar.add(Calendar.MINUTE, +5)
    }

    val temp = observations(3)
    observations(3) = observations(0)
    observations(0) = temp
    section = new Section(id, sequence, fromStop, toStop)
  }

  after {
    DBConnectionPool.close()
  }

  test("GetDelay") {
    assert(section.getDelay() == 0)
    for (observation <- observations) {
      section.addObservation(observation._1, observation._2, observation._3)
    }
    assert(section.getDelay() == 725)
  }

  test("LatestObservationTime") {
    assert(section.getLatestObservationTime() == null)
    for (observation <- observations) {
      section.addObservation(observation._1, observation._2, observation._3)
    }
    assert(section.getLatestObservationTime() == observations(4)._3)
  }

  test("Clear") {
    assert(section.getLatestObservationTime() == null)
    for (observation <- observations) {
      section.addObservation(observation._1, observation._2, observation._3)
    }
    section.clear()
    assert(section.getLatestObservationTime() == null)
  }

  //  test("Save") {
  //    assert(section.getLatestObservationTime() == null)
  //    assert(section.getDelay() == 0)
  //    for (observation <- observations) {
  //      section.addObservation(observation)
  //    }
  //    assert(section.getDelay() == 725)
  //    section.save(Calendar.getInstance().getTime)
  //  }
}
