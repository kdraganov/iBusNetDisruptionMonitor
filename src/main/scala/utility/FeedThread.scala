package utility

import java.io.File
import java.nio.file.{FileSystems, Files, StandardCopyOption}

import org.slf4j.LoggerFactory

import scala.collection.mutable.{ArrayBuffer, Buffer}

/**
 * Created by Konstantin on 17/02/2015.
 */
class FeedThread(private val subDir: String, private val operator: String, private var sleepInterval: Long = 1500) extends Thread {

  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)

  private val feedDirectory: File = new File("E:\\Workspace\\iBusNetTestDirectory\\Feeds\\" + subDir)
  private val feedFilenameFilter = new CustomFilenameFilter("CC_", ".csv")
  private val operatorFilenameFilter = new CustomFilenameFilter("CC_", operator + "_YYYYMMDD_NNNNN")

  private val operatorBuffers: ArrayBuffer[Buffer[File]] = new ArrayBuffer[Buffer[File]]()

  def init(): Unit = {
    for (operatorDir: File <- feedDirectory.listFiles(operatorFilenameFilter) if operatorDir.isDirectory) {
      val temp = operatorDir.listFiles(feedFilenameFilter)
      scala.util.Sorting.quickSort(temp)
      operatorBuffers.append(temp.toBuffer)
    }
    for (buffer <- operatorBuffers) {
      logger.debug("{} feeds in buffer.", buffer.size)
    }
    logger.debug("Sleeping for 5 seconds before starting")
    try {
      Thread.sleep(5000)
    } catch {
      case e: InterruptedException => logger.error("Feed thread interrupted:", e)
    }

  }

  override
  def run(): Unit = {
    init()
    var terminate = false
    while (!terminate) {
      speedControl()
      terminate = true
      for (buffer <- operatorBuffers) {
        if (!buffer.isEmpty) {
          copy(buffer.remove(0))
        }
        if (!buffer.isEmpty) {
          terminate = false
        }
      }

      try {
        Thread.sleep(sleepInterval)
      } catch {
        case e: InterruptedException => logger.error("Feed thread interrupted:", e)
      }
    }
    logger.debug("All feed files have been copied.")
  }

  private def copy(file: File): Unit = {
    //logger.trace("Copying file [{}] to {}.", file.getName, Configuration.getFeedsDirectory().getName)
    val sourceFile = FileSystems.getDefault.getPath(file.getAbsolutePath)
    val destinationFile = FileSystems.getDefault.getPath(Configuration.getFeedsDirectory().getAbsolutePath, file.getName)
    Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING)
  }

  private def speedControl(): Unit = {
    var pause = true
    while (pause) {
      val speed = scala.io.Source.fromFile("E:\\Workspace\\iBusNetTestDirectory\\busNetwork\\speedControl.txt").mkString
      speed match {
        case "slow" => sleepInterval = 10000
          pause = false
        case "normal" => sleepInterval = 1500
          pause = false
        case "fast" => sleepInterval = 500
          pause = false
        case "pause" => pause = true
          Thread.sleep(5000)
        case default => sleepInterval = 2500
          pause = false
      }
    }

  }
}
