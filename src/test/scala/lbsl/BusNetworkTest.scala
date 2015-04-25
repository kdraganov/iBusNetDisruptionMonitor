package scala.lbsl

import _root_.lbsl.Network
import _root_.utility.{DBConnectionPool, Environment}

import scala.main.UnitSpec

/**
 * Created by Konstantin on 12/03/2015.
 */
class BusNetworkTest extends UnitSpec {

  before {
    DBConnectionPool.createPool(dbConnectionSettingsPath)
    Environment.init()
  }

  after {
    DBConnectionPool.close()
  }

  test("BusNetworkTest") {
    val busNetwork: Network = new Network()
    // busNetwork.init()
    //add observation
    //check DB state
    //clean DB
  }


}

