package scala.lbsl

import _root_.lbsl.BusStop
import _root_.utility.{DBConnectionPool, Environment}
import uk.me.jstott.jcoord.OSRef

import scala.main.UnitSpec

/**
 * Created by Konstantin on 12/03/2015.
 */
class BusStopTest extends UnitSpec {

  private val stopList: Array[String] = Array[String](
    "10024;55564;490012751W;ST JOSEPH'S CHURCH;521017;168169",
    "10258;47901;490010101S;THIRLMERE GARDENS;507950;191964",
    "1037;53342;490012521E;STAMFORD BROOK BUS GARAGE;521636;178587",
    "10343;75778;490010345W;NORTH COUNTESS ROAD;536782;190809",
    "10439;55159;490007093W1;WOOLWICH ROAD / GALLIONS ROAD;540897;178446",
    "1040;76767;490005423FF;CLIFTON GARDENS;520773;178494",
    "1041;77336;490012434HH;ST ANN'S ROAD;531763;188697",
    "58;53292;490013046N;SUSSEX GARDENS;527216;181547",
    "4797;76585;490013046M;EDGWARE ROAD / PRAED STREET;527173;181593",
    "28747;77640;490013046T;SUSSEX GARDENS;527213;181529",
    "29802;58110;490006135T;DORSET SQUARE / MARYLEBONE STATION <>;527719;182082",
    "4773;51725;490010713E;PADDINGTON GREEN POLICE STATION;526955;181719",
    "33761;51848;490010567H;OLD MARYLEBONE ROAD;527306;181770"
  )
  before {
    DBConnectionPool.createPool(dbConnectionSettingsPath)
    Environment.init()
  }

  after {
    DBConnectionPool.close()
  }

  test("GetBusStop") {
    for (line <- stopList) {
      val testStop = line.split(";(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)")
      val stop = BusStop.getBusStop(testStop(0))
      assert(stop.getLBSLCode() == testStop(0))
      assert(stop.getCode() == testStop(1))
      assert(stop.getNaptanAtco() == testStop(2))
      assert(stop.getName() == testStop(3))
      val latLng = new OSRef(testStop(4).toDouble, testStop(5).toDouble).toLatLng()
      latLng.toWGS84()
      assert(stop.getLongitude() == latLng.getLng)
      assert(stop.getLatitude() == latLng.getLat)
    }
  }

  test("NewBusStop") {
    for (line <- stopList) {
      val testStop = line.split(";(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)")
      val latLng = new OSRef(testStop(4).toDouble, testStop(5).toDouble).toLatLng()
      latLng.toWGS84()
      val stop = new BusStop(testStop(0), testStop(3), testStop(1), testStop(2), latLng.getLat, latLng.getLng)
      assert(stop.getLBSLCode() == testStop(0))
      assert(stop.getCode() == testStop(1))
      assert(stop.getNaptanAtco() == testStop(2))
      assert(stop.getName() == testStop(3))
      assert(stop.getLongitude() == latLng.getLng)
      assert(stop.getLatitude() == latLng.getLat)
    }
  }

}
