package scala.lbsl

import java.sql.PreparedStatement
import java.util.Calendar

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
    Environment.setLatestFeedTimeOfData(Calendar.getInstance().getTime)
  }

  after {
    DBConnectionPool.close()
  }

  test("Run") {
    cleanDBTables()
    val input = Array[String](
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;BP3385;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:02:00;20150419;55;1;1;3;RV1;29985;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:04:00;20150419;55;1;1;3;RV1;1835;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:06:00;20150419;55;1;1;3;RV1;33432;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:08:00;20150419;55;1;1;3;RV1;BP3400;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:10:00;20150419;55;1;1;3;RV1;BP3394;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:12:00;20150419;55;1;1;3;RV1;BP3395;0;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:14:00;20150419;55;1;1;3;RV1;226;0;300;0;0;0",
      "1234;1234;1234;2015/04/15 08:16:00;20150419;55;1;1;3;RV1;25940;900;0;0;0;0", //Disrupted
      "1234;1234;1234;2015/04/15 08:09:00;20150419;55;1;1;3;RV1;25938;1500;0;0;0;0", //Disrupted
      "1234;1234;1234;2015/04/15 08:09:00;20150419;55;1;1;3;RV1;25938;2000;0;0;0;0", //Disrupted
      "1234;1234;1234;2015/04/15 08:0:00;20150419;55;1;1;3;RV1;1520;2300;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;8316;2300;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;2198;2300;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;33599;2300;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;R0049;2300;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;BP4909;2300;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;BP3446;2300;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;BP3693;2300;0;0;0;0",
      "1234;1234;1234;2015/04/15 08:00:00;20150419;55;1;1;3;RV1;BP3453;2300;0;0;0;0"
    )
    val observationList: Array[Observation] = new Array[Observation](20)
    for (i <- 0 until input.length) {
      val observation = new Observation
      observation.init(input(i), "TestOperator")
      observationList(i) = observation
    }
    val route = "RV1"
    val run = new Run(route, 1)
    run.init()
    for (i <- 1 until observationList.length) {
      run.checkStops(observationList(i - 1), observationList(i))
    }

    Environment.getDBTransaction.begin()
    run.detectDisruptions()
    Environment.getDBTransaction.commit()

    var preparedStatement: PreparedStatement = null
    val connection = DBConnectionPool.getConnection()
    var query = "SELECT * FROM \"Disruptions\" WHERE route = ?"
    preparedStatement = connection.prepareStatement(query)
    preparedStatement.setString(1, route)
    var rs = preparedStatement.executeQuery()
    if (rs.next()) {
      assert(rs.getString("fromStopLBSLCode") == "226")
      assert(rs.getString("toStopLBSLCode") == "1520")
      assert(rs.getString("route") == "RV1")
      assert(rs.getInt("run") == 1)
      assert(rs.getInt("delayInSeconds") == 2150)
      assert(rs.getInt("routeTotalDelayInSeconds") == 2300)
      assert(rs.getInt("trend") == -1)
    }
    preparedStatement.close()

    query = "SELECT \"SectionsLostTime\".* FROM \"SectionsLostTime\" LEFT JOIN \"Sections\" ON \"SectionsLostTime\".\"sectionId\" = \"Sections\".id WHERE route = ?"
    preparedStatement = connection.prepareStatement(query)
    preparedStatement.setString(1, route)
    rs = preparedStatement.executeQuery()
    while (rs.next()) {
      if (rs.getInt("sectionId") < 51382 || rs.getInt("sectionId") > 51385) {
        assert(rs.getInt("lostTimeInSeconds") == 0)
      } else {
        rs.getInt("sectionId") match {
          case 51382 => assert(rs.getInt("lostTimeInSeconds") == 450)
          case 51383 => assert(rs.getInt("lostTimeInSeconds") == 750)
          case 51384 => assert(rs.getInt("lostTimeInSeconds") == 950)
          case 51385 => assert(rs.getInt("lostTimeInSeconds") == 150)
        }
      }
      assert(rs.getInt("numberOfObservations") == 1)
    }
    preparedStatement.close()
    DBConnectionPool.returnConnection(connection)

    cleanDBTables()
  }


}
