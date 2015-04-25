package scala.utility

import _root_.utility.DBConnectionPool

import scala.main.UnitSpec

/**
 * Created by Konstantin on 12/03/2015.
 */
class DBConnectionPoolTest extends UnitSpec {

  after {
    DBConnectionPool.close()
  }

  test("Pool") {
    DBConnectionPool.createPool(dbConnectionSettingsPath)
    DBConnectionPool.setMaxPoolSize(5)
    assert(DBConnectionPool.getMaxPoolSize() == 5)
    DBConnectionPool.setMaxPoolSize(2)
    assert(DBConnectionPool.getMaxPoolSize() == 2)

    val connection1 = DBConnectionPool.getConnection()
    val connection2 = DBConnectionPool.getConnection()

    var threadExecuted = false
    val thread1 = new Thread(new Runnable {
      override def run(): Unit = {
        val connection = DBConnectionPool.getConnection()
        threadExecuted = true
        connection.close()
      }
    })
    thread1.start()

    Thread.sleep(10000)
    assert(thread1.isAlive)
    assert(!threadExecuted)

    connection1.close()
    connection2.close()

    Thread.sleep(10000)
    assert(!thread1.isAlive)
    assert(threadExecuted)

  }
}
