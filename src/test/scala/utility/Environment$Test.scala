package scala.utility

import _root_.utility.{DBConnectionPool, Environment}
import org.scalatest.FunSuite

/**
 * Created by Konstantin on 30/03/2015.
 */
class Environment$Test extends FunSuite {

  test("My first test") {
    DBConnectionPool.createPool("settings.xml")
    Environment.init()
    assert(Environment.getDataValidityTimeInMinutes == 90)
  }


}
