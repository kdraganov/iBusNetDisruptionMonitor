package lbsl

import java.sql.{Connection, PreparedStatement, SQLException}

import org.slf4j.LoggerFactory
import uk.me.jstott.jcoord.OSRef
import utility.DBConnectionPool

/**
 * Created by Konstantin on 26/01/2015.
 *
 * Class representing a BusStop in TFL bus
 * network. Currently this is unused, however if
 * required the functionality is ready to be used.
 *
 */
class BusStop(
               private val lbslCode: String,
               private val name: String,
               private val code: String,
               private val NaptanAtco: String,
               private val latitude: Double,
               private val longitude: Double) {


  def getLBSLCode(): String = lbslCode

  def getName(): String = name

  def getNaptanAtco(): String = NaptanAtco

  def getCode(): String = code

  def getLongitude(): Double = longitude

  def getLatitude(): Double = latitude

}

object BusStop {

  final val LBSLCode: Integer = 0
  final val Code: Integer = 1
  final val NaptanAtco: Integer = 2
  final val StopName: Integer = 3
  final val LocationEasting: Integer = 4
  final val LocationNorthing: Integer = 5
  final val Heading: Integer = 6
  final val StopArea: Integer = 7
  final val VirtualBusStop: Integer = 8

  /**
   * Static method for initialising a bus stop
   * from the database.
   * @param lbslCode String - the LBSL code of the bus stop
   * @return - initialised BusStop
   */
  def getBusStop(lbslCode: String): BusStop = {
    var busStop: BusStop = null
    var connection: Connection = null
    var preparedStatement: PreparedStatement = null
    val selectSQL = "SELECT code, \"naptanAtcoCode\", name, \"locationEasting\", \"locationNorthing\" FROM \"BusStops\" WHERE \"lbslCode\" = ?"
    try {
      connection = DBConnectionPool.getConnection()
      preparedStatement = connection.prepareStatement(selectSQL)
      preparedStatement.setString(1, lbslCode)
      val rs = preparedStatement.executeQuery()
      if (rs.next()) {
        val latLng = new OSRef(rs.getDouble("locationEasting"), rs.getDouble("locationNorthing")).toLatLng()
        latLng.toWGS84()
        busStop = new BusStop(lbslCode, rs.getString("name"), rs.getString("code"), rs.getString("naptanAtcoCode"), latLng.getLat, latLng.getLng)
      }
    }
    catch {
      case e: SQLException => LoggerFactory.getLogger(getClass().getSimpleName).error("Exception:", e)
    } finally {
      if (preparedStatement != null) {
        preparedStatement.close()
      }
      if (connection != null) {
        DBConnectionPool.returnConnection(connection)
      }
    }
    return busStop
  }
}