
package scala.main

import _root_.utility.DBConnectionPool
import org.scalatest.{BeforeAndAfter, FunSuite}

/**
 * Created by Konstantin on 18/03/2015.
 */
class UnitSpec extends FunSuite with BeforeAndAfter {

  val dbConnectionSettingsPath: String = "settings-test.xml"

  def cleanDBTables(): Unit = {
    val connection = DBConnectionPool.getConnection()
    var updateStatement = connection.createStatement()
    updateStatement.execute("DELETE FROM \"SectionsLostTime\"")
    updateStatement.close()

    updateStatement = connection.createStatement()
    updateStatement.execute("DELETE FROM \"DisruptionComments\"")
    updateStatement.close()

    updateStatement = connection.createStatement()
    updateStatement.execute("DELETE FROM \"Disruptions\"")
    updateStatement.close()
    DBConnectionPool.returnConnection(connection)
  }
}
