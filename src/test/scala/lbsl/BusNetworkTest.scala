package scala.lbsl

import java.sql.PreparedStatement

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
    cleanDBTables()
  }

  after {
    cleanDBTables()
    DBConnectionPool.close()
  }

  test("BusNetworkTest") {
    val busNetwork: Network = new Network()
    busNetwork.init()
    assert(busNetwork.getRouteCount() == 680)
    check()
    busNetwork.update()
    check()
  }

  private def check() {
    var preparedStatement: PreparedStatement = null
    val connection = DBConnectionPool.getConnection()
    var query = "SELECT * FROM \"Disruptions\""
    preparedStatement = connection.prepareStatement(query)
    var rs = preparedStatement.executeQuery()
    assert(!rs.next())
    preparedStatement.close()

    query = "SELECT * FROM \"SectionsLostTime\" "
    preparedStatement = connection.prepareStatement(query)
    rs = preparedStatement.executeQuery()
    assert(!rs.next())
    preparedStatement.close()
    DBConnectionPool.returnConnection(connection)
  }

}

