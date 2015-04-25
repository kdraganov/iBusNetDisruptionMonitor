package scala.lbsl

import java.sql.PreparedStatement

import _root_.lbsl.{Observation, Run}
import _root_.utility.{DBConnectionPool, Environment}

import scala.main.UnitSpec

/**
 * Created by Konstantin on 12/03/2015.
 */
class RunTest extends UnitSpec {

  before {
    DBConnectionPool.createPool(dbConnectionSettingsPath)
    Environment.init()
  }

  after {
    DBConnectionPool.close()
  }

  test("Run") {
    val input = Array[String](
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;BP3385;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:02:00;20150419;55;1;1;3;RV1;29985;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:04:00;20150419;55;1;1;3;RV1;1835;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:06:00;20150419;55;1;1;3;RV1;33432;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:08:00;20150419;55;1;1;3;RV1;BP3400;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:10:00;20150419;55;1;1;3;RV1;BP3394;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:12:00;20150419;55;1;1;3;RV1;BP3395;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:14:00;20150419;55;1;1;3;RV1;226;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:16:00;20150419;55;1;1;3;RV1;25940;0;0;0;0;0", //Disrupted
      "1234;1234;1234;2015/04/15 08:09:00;20150419;55;1;1;3;RV1;25938;0;0;0;0;0", //Disrupted
      "1234;1234;1234;2015/04/15 08:09:00;20150419;55;1;1;3;RV1;25938;0;0;0;0;0", //Disrupted
      "1234;1234;1234;2015/04/15 08:0:00;20150419;55;1;1;3;RV1;1520;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;8316;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;2198;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;33599;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;R0049;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;BP4909;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;BP3446;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;BP3693;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;BP3453;0;0;0;0;0"
    )
    val observationList: Array[Observation] = new Array[Observation](20)
    for (i <- 0 until input.length) {
      val observation = new Observation
      observation.init(input(i), "TestOperator")
      observationList(i) = observation
    }
    val run = new Run("RV1", 1)
    run.init()
    for (i <- 1 until observationList.length) {
      run.checkStops(observationList(i - 1), observationList(i))
    }
    run.detectDisruptions()

    //check database
    
//    var preparedStatement: PreparedStatement = null
//    val connection = DBConnectionPool.getConnection()
//    val query = "UPDATE \"EngineConfigurations\" SET value=? WHERE key=?"
//    preparedStatement = connection.prepareStatement(query)
//    preparedStatement.setInt(1, newDataValidityTimeInMinutes)
//    preparedStatement.setString(2, "dataValidityTimeInMinutes")
//    preparedStatement.executeUpdate()
//    preparedStatement.close()
//    DBConnectionPool.returnConnection(connection)

    //clean database
  }

}
