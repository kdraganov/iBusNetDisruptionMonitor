package scala.utility

import _root_.lbsl.{Network, Observation}
import _root_.utility.{DBConnectionPool, Environment}
import org.scalatest.{BeforeAndAfter, FunSuite}

/**
 * Created by Konstantin on 30/03/2015.
 */
class TestSuiteOne extends FunSuite with BeforeAndAfter {

  val busNetwork: Network = new Network()

  before {
    DBConnectionPool.createPool("settings.xml")
    Environment.init()
    busNetwork.init()
  }

  after {
    DBConnectionPool.close()
  }

  test("Observation parsing") {
    val trueLine: Array[String] = Array[String] {
      "14713;ADE5;YX12FNL;2015/02/19 00:47:37;20150213;71314;629403;259;3;222;17655;-80;-0.45673;51.48959;0;;"
    }
    val falseLines: Array[String] = Array[String] {
      "14695;VLP23;PJ53OUW;2015/02/19 00:47:04;20150213;-2147483645;-2147483645;-2147483645;7;114;27858;-2147483645;-0.27314;51.61161;0;;"
    }
    for (line <- trueLine) {
      val observation = new Observation()
      assert(observation.init(line, "TEST") == true)
      println(observation.getContractRoute)
      println(observation.getLastStopShortDesc)
      println(observation.getTripType)
      println(observation.getTimeOfData)
      println(observation.getScheduleDeviation)
    }

    for (line <- falseLines) {
      val observation = new Observation()
      assert(observation.init(line, "TEST") == false)
    }

  }
//  test("Bus network size") {
//    assert(busNetwork.getRouteCount() == 680)
//  }
//
//  test("My first test") {
//    assert(Environment.getDataValidityTimeInMinutes == 90)
//  }


}
