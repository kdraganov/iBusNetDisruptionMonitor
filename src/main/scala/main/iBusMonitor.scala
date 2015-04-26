package main

import java.io.{File, FileNotFoundException, FilenameFilter}
import java.nio.file._

import lbsl.{Network, Observation}
import org.slf4j.LoggerFactory
import utility.{CustomFilenameFilter, Environment}

import scala.collection.JavaConversions._
import scala.io.Source

/**
 * Created by Konstantin on 20/01/2015.
 * Class which extends Thread and provides monitoring
 * functionality. It is responsible for monitoring a predefined
 * directory for new feed files.
 *
 */
class iBusMonitor() extends Thread {

  private val busNetwork: Network = new Network
  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private var updateNetwork: Boolean = false;
  private val feedFilenameFilter: FilenameFilter = new CustomFilenameFilter(Environment.getFeedFilePrefix, Environment.getFeedFileSuffix)

  /**
   * The run method of this class is expected to run continuously.
   * It listens for new files being created/updated in a predefine
   * directory. Once it detects a new file it checks if the file
   * is following the expected naming conventions and format. If
   * all holds the files is processed by extracting the observations.
   * After processing the file is moved to processed feed files directory.
   */
  override
  def run() {
    init()
    val watchService = FileSystems.getDefault.newWatchService()
    Paths.get(Environment.getFeedDirectory().getAbsolutePath).register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)
    while (true) {
      val key = watchService.take()
      val events = key.pollEvents()
      for (event <- events) {
        val event_path = event.context().asInstanceOf[Path]
        if (event_path != null) {
          val fileName = event_path.toString()
          if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
            if (fileName.startsWith(Environment.getFeedFilePrefix) && fileName.endsWith(Environment.getFeedFileSuffix)) {
              //              logger.info("New file detected: {}", fileName)
              processFile(new File(Environment.getFeedDirectory().getAbsolutePath + "\\" + fileName))
            }
          }
        }
      }
      key.reset()

      //checks for any unprocessed files
      for (file <- Environment.getFeedDirectory().listFiles(feedFilenameFilter) if file.isFile) {
        processFile(file)
      }
      //Only update network if valid observations have been processed
      if (updateNetwork) {
        try {
          busNetwork.update()
        } catch {
          case e: Exception => logger.error("TERMINATING - iBusMonitorThread interrupted:", e)
            System.exit(-1)
        }
        updateNetwork = false
      }
      nap()
    }
  }

  private def processFile(file: File): Unit = {
    if (file.isFile && file.exists() && file.canRead() && file.canExecute) {
      logger.info("Processing file [{}].", file.getAbsolutePath)
      while (file.exists()) {
        try {
          processFeed(file)
        } catch {
          case e: FileNotFoundException => nap(1000)
            logger.info("File {} not accessible - sleeping for 1s." , file.getAbsolutePath) //logger.warn("Exception:", e)
          case e: Exception => logger.error("TERMINATING - iBusMonitorThread interrupted:", e)
            System.exit(-1)
        }
      }
      updateNetwork = true
    }
  }

  private def nap(timeMilliSeconds: Long = Environment.getMonitorThreadSleepInterval()): Unit = {
    try {
      Thread.sleep(timeMilliSeconds)
    } catch {
      case e: InterruptedException => logger.error("iBusMonitorThread interrupted:", e)
    }
  }

  @throws(classOf[FileNotFoundException])
  private def processFeed(file: File): Unit = {
    val source = Source.fromFile(file.getAbsolutePath)
    //Check whether to drop header
    for (line <- source.getLines().drop(if (Environment.getFeedFileHeader) 1 else 0)) {
      val observation = new Observation()
      if (observation.init(line, file.getName)) {
        busNetwork.addObservation(observation)
      }
    }
    source.close
    val sourceFile = FileSystems.getDefault.getPath(file.getAbsolutePath)
    val destinationFile = FileSystems.getDefault.getPath(Environment.getProcessedDirectory().getAbsolutePath, file.getName)
    Files.move(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING)
  }

  private def init(): Unit = {
    logger.trace("Starting iBusMonitor initialisation.")
    logger.trace("****************************************************")
    val start = System.nanoTime()
    busNetwork.init()
    val elapsedTime = System.nanoTime() - start
    logger.trace("Finished iBusMonitor initialisation in {} seconds.", (elapsedTime / 1000000000.0))
    logger.trace("****************************************************")
    logger.trace("Start monitoring folder [{}] for new file feeds.", Environment.getFeedDirectory().getAbsolutePath)
  }

}
