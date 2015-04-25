package scala.lbsl

import java.util.Calendar

import _root_.lbsl.Disruption
import _root_.utility.{DBConnectionPool, Environment}

import scala.main.UnitSpec

/**
 * Created by Konstantin on 12/03/2015.
 */
class DisruptionTest extends UnitSpec {

  private val disruptionTestList: Array[String] = Array[String](
    "1;6;29844;2593;360;600;0",
    "23;27;34554;BP2028;1200;1600;0"
  )
  before {
    DBConnectionPool.createPool(dbConnectionSettingsPath)
    Environment.init()
  }

  after {
    DBConnectionPool.close()
  }

  test("DisruptionTest") {
    for (line <- disruptionTestList) {
      val tokens = line.split(";")
      val calendar = Calendar.getInstance()
      calendar.add(Calendar.MINUTE, tokens(6).toInt)
      val disruption = new Disruption(tokens(0).toInt, tokens(1).toInt, tokens(2), tokens(3), tokens(4).toDouble, tokens(5).toDouble, calendar.getTime)

      assert(disruption.getSectionStartIndex == tokens(0).toInt)
      assert(disruption.getSectionEndIndex == tokens(1).toInt)
      assert(disruption.getSectionStartBusStop == tokens(2))
      assert(disruption.getSectionEndBusStop == tokens(3))
      assert(disruption.getDelay == tokens(4).toDouble)
      assert(disruption.getDelayInMinutes == (tokens(4).toDouble / 60).toInt)
      assert(disruption.getTotalDelay == tokens(5).toDouble)
      assert(disruption.getTotalDelayInMinutes == (tokens(5).toDouble / 60).toInt)
      assert(disruption.getTimeFirstDetected == calendar.getTime)
      assert(disruption.getTrend == -1)

      //Trend stable
      disruption.update(tokens(4).toDouble, tokens(5).toDouble)
      assert(disruption.getDelay == tokens(4).toDouble)
      assert(disruption.getDelayInMinutes == (tokens(4).toDouble / 60).toInt)
      assert(disruption.getTotalDelay == tokens(5).toDouble)
      assert(disruption.getTotalDelayInMinutes == (tokens(5).toDouble / 60).toInt)
      assert(disruption.getTrend == 0)

      for (i <- 0 to 1) {
        val newDelay = 10000 * i
        val newTotalDelay = 15000 * i
        disruption.update(newDelay, newTotalDelay)
        assert(disruption.getDelay == newDelay)
        assert(disruption.getDelayInMinutes == (newDelay / 60))
        assert(disruption.getTotalDelay == newTotalDelay)
        assert(disruption.getTotalDelayInMinutes == (newTotalDelay / 60))
        if (i == 1) {
          assert(disruption.getTrend == -1)
        } else {
          assert(disruption.getTrend == 1)
        }
      }

      disruption.update(tokens(0).toInt, tokens(1).toInt - 2, tokens(2), tokens(3), tokens(4).toDouble, tokens(5).toDouble)
      assert(disruption.getTrend == 1)

      disruption.update(tokens(0).toInt, tokens(1).toInt + 2, tokens(2), tokens(3), tokens(4).toDouble, tokens(5).toDouble)
      assert(disruption.getTrend == -1)
    }
  }

  test("Equality") {
    val disruption1 = new Disruption(2, 5, "34554", "BP2028", 650, 980, Calendar.getInstance().getTime)
    val disruption2 = new Disruption(2, 8, "34554", "BP2028", 650, 980, Calendar.getInstance().getTime)
    val disruption3 = new Disruption(3, 5, "34554", "BP2028", 650, 980, Calendar.getInstance().getTime)
    val disruption4 = new Disruption(0, 3, "34554", "BP2028", 650, 980, Calendar.getInstance().getTime)

    val disruption5 = new Disruption(8, 9, "34554", "BP2028", 650, 980, Calendar.getInstance().getTime)
    val disruption6 = new Disruption(9, 11, "34554", "BP2028", 650, 980, Calendar.getInstance().getTime)

    assert(disruption1.equals(disruption2))
    assert(disruption1.equals(disruption3))
    assert(disruption1.equals(disruption4))
    assert(!disruption1.equals(disruption5))

    assert(disruption2.equals(disruption3))
    assert(disruption2.equals(disruption4))
    assert(disruption2.equals(disruption5))
    assert(!disruption2.equals(disruption6))

    assert(disruption3.equals(disruption4))
    assert(!disruption3.equals(disruption5))
    assert(!disruption3.equals(disruption6))
    assert(!disruption3.equals(disruption6))

    assert(!disruption4.equals(disruption5))
    assert(!disruption4.equals(disruption6))

    assert(disruption5.equals(disruption6))
  }

}
