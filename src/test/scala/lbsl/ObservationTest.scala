package scala.lbsl

import _root_.lbsl.Observation
import _root_.utility.{DBConnectionPool, Environment}

import scala.main.UnitSpec

/**
 * Created by Konstantin on 12/03/2015.
 */
class ObservationTest extends UnitSpec {

  private val trueLines: Array[String] = Array[String](
    "9534;SE93;YX11CPZ;2015/02/18 20:29:54;20150213;122076;508640;207;3;100;34190;260;-0.07433;51.51468;0;;",
    "8977;E181;SN61BHU;2015/02/18 20:30:29;20150213;34327;1013459;239;3;468;R0430;0;-0.11082;51.48500;0;;",
    "7892;LT292;LTZ1292;2015/02/18 20:30:26;20150213;122034;848054;309;3;453;1582;230;-0.15023;51.52316;0;;",
    "9795;WVL360;LX60DWL;2015/02/18 20:30:40;20150213;10056;212183;171;2;229;BP5396;-510; 0.09941;51.42034;0;;",
    "8978;E182;SN61BHV;2015/02/18 20:30:23;20150213;34061;1017904;224;3;68;34686;0;-0.09694;51.47346;0;;",
    "8990;EH18;SN61DCE;2015/02/18 20:30:34;20150213;80305;1009030;255;3;436;532;-1500;-0.11194;51.48170;0;;",
    "8446;E166;SN61BGU;2015/02/18 20:29:53;20150213;34123;1011339;221;3;185;13380;-270;-0.07903;51.46144;0;;"
  )

  private val falseLines: Array[String] = Array[String](
    "7624;WVN19;LK59FDM;2015/02/18 20:30:06;20150213;-2147483645;-2147483645;-2147483645;7;259;29899;-2147483645;-0.08194;51.57863;0;;",
    "7955;WVL190;LX05EZV;2015/02/18 20:29:48;20150213;-2147483645;-2147483645;-2147483645;7;191;26975;-2147483645;-0.05149;51.60018;0;;",
    "7958;WVL193;LX05EZK;2015/02/18 20:30:10;20150213;-2147483645;-2147483645;-2147483645;7;257;BP4267;-2147483645;-0.04075;51.58688;0;;",
    "8806;WVL389;LX11CVO;2015/02/18 20:27:22;20150213;-2147483645;-2147483645;-2147483645;7;171;NX;-2147483645;-0.04388;51.47298;0;;",
    "9345;PVL397;LX54GZH;2015/02/18 20:30:32;20150213;-2147483645;-2147483645;-2147483645;7;280;4529;-2147483645;-0.19030;51.41574;0;;",
    "7626;WVN21;LK59FDO;2015/02/18 20:28:26;20150213;-2147483645;-2147483645;-2147483645;7;259;2780;-2147483645;-0.05060;51.60030;0;;"
  )

  before {
    DBConnectionPool.createPool(dbConnectionSettingsPath)
    Environment.init()
  }

  after {
    DBConnectionPool.close()
  }

  test("ValidObservations") {
    for (line <- trueLines) {
      val tokens: Array[String] = line.split(";(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)")
      val observation = new Observation()
      assert(observation.init(line, "TEST"))
      assert(observation.getOperator == "TEST")
      assert(observation.getVehicleId == tokens(0).toInt)
      assert(observation.getTimeOfData == Environment.getDateFormat().parse(tokens(3)))
      assert(observation.getTripId == tokens(6).toInt)
      assert(observation.getTripType == tokens(8).toInt)
      assert(observation.getContractRoute == tokens(9))
      assert(observation.getLastStopShortDesc == tokens(10))
      assert(observation.getScheduleDeviation == tokens(11).toInt)
      assert(observation.getLongitude == tokens(12).toDouble)
      assert(observation.getLatitude == tokens(13).toDouble)
      assert(observation.getEventId == tokens(14).toInt)
      assert(observation.isValid)
    }
  }

  test("InvalidObservations") {
    for (line <- falseLines) {
      val tokens: Array[String] = line.split(";(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)")
      val observation = new Observation()
      assert(!observation.init(line, "TEST"))
      assert(observation.getTripType == tokens(8).toInt)
      assert(observation.getScheduleDeviation == tokens(11).toInt)
      assert(!observation.isValid)
      assert(observation.getOperator == null)
      assert(observation.getVehicleId == 0)
      assert(observation.getTimeOfData == null)
      assert(observation.getTripId == 0)
      assert(observation.getContractRoute == null)
      assert(observation.getLastStopShortDesc == null)
      assert(observation.getLongitude == 0)
      assert(observation.getLatitude == 0)
      assert(observation.getEventId == 0)
    }
  }

  test("Comparison") {
    val line1 = "9534;SE93;YX11CPZ;2015/02/18 20:29:54;20150213;122076;508640;207;3;100;34190;260;-0.07433;51.51468;0;;"
    val line2 = "9534;SE93;YX11CPZ;2015/02/18 22:29:54;20150213;122076;508640;207;3;100;34190;260;-0.07433;51.51468;0;;"
    val observation1 = new Observation()
    assert(observation1.init(line1, "TEST"))
    val observation2 = new Observation()
    assert(observation2.init(line1, "TEST"))
    val observation3 = new Observation()
    assert(observation3.init(line2, "TEST"))

    assert(observation1.compareTo(observation2) == 0)

    assert(observation1.compareTo(observation3) < 0)
    assert(observation2.compareTo(observation3) < 0)

    assert(observation3.compareTo(observation1) > 0)
    assert(observation3.compareTo(observation2) > 0)
  }

}