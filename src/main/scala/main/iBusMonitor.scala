package main

import java.io.File
import java.nio.file._

import lbsl.{Network, Observation}
import org.slf4j.LoggerFactory
import utility.Configuration

import scala.collection.JavaConversions._
import scala.io.Source

/**
 * Created by Konstantin on 20/01/2015.
 */
class iBusMonitor() extends Thread {

  private val busNetwork: Network = new Network
  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)

  override
  def run() {
    //initialize the bus network
    logger.trace("Starting {} initialisation.", Configuration.getTitle())
    logger.trace("****************************************************")
    busNetwork.init()
    logger.trace("Finished {} initialisation.", Configuration.getTitle())
    logger.trace("****************************************************")
    logger.trace("Start monitoring folder [{}] for new file feeds.", Configuration.getFeedsDirectory().getAbsolutePath)

    val watchService = FileSystems.getDefault.newWatchService()
    Paths.get(Configuration.getFeedsDirectory().getAbsolutePath).register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)

    while (true) {
      val key = watchService.take()
      val events = key.pollEvents()
      var update: Boolean = false;
      for (event <- events) {
        val event_path = event.context().asInstanceOf[Path]
        val fileName = event_path.toString()
        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
          logger.info("New file detected: {}", fileName)
          if (fileName.startsWith(Configuration.getFeedFileStartWith) && fileName.endsWith(Configuration.getFeedFileEndWith)) {
            val file: File = new File(Configuration.getFeedsDirectory().getAbsolutePath + "\\" + fileName)
            if (file.isFile && file.exists() && file.canRead() && file.canExecute) {
              logger.info("Processing file [{}].", file.getAbsolutePath)
              processFeed(file)
              update = true
            }
          } else {
            logger.info("File [{}] not for processing.", fileName)
          }
        }
      }
      key.reset()
      if (update) {
        busNetwork.calculateDisruptions()
      }
      Thread.sleep(Configuration.getMonitorThreadSleepInterval())
    }
  }

  def processFeed(file: File): Unit = {
    val source = Source.fromFile(file.getAbsolutePath)
    //Check whether to drop header
    for (line <- source.getLines().drop(1)) {
      val observation = new Observation()
      if (observation.init(line)) {
        busNetwork.addObservation(observation)
      }
    }
    source.close

    val sourceFile = FileSystems.getDefault.getPath(file.getAbsolutePath)
    val destinationFile = FileSystems.getDefault.getPath(Configuration.getProcessedDirectory().getAbsolutePath, file.getName)
    Files.move(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING)
  }

}
