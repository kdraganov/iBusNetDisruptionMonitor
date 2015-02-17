package main

import java.io.{File, FileNotFoundException, FilenameFilter}
import java.nio.file._

import lbsl.{Network, Observation}
import org.slf4j.LoggerFactory
import utility.{Configuration, CustomFilenameFilter}

import scala.collection.JavaConversions._
import scala.io.Source

/**
 * Created by Konstantin on 20/01/2015.
 */
class iBusMonitor() extends Thread {

  private val busNetwork: Network = new Network
  private val logger = LoggerFactory.getLogger(getClass().getSimpleName)
  private var updateNetwork: Boolean = false;
  private val feedFilenameFilter: FilenameFilter = new CustomFilenameFilter(Configuration.getFeedFileStartWith, Configuration.getFeedFileEndWith)

  override
  def run() {
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
      for (event <- events) {
        val event_path = event.context().asInstanceOf[Path]
        if (event_path != null) {
          val fileName = event_path.toString()
          if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
            if (fileName.startsWith(Configuration.getFeedFileStartWith) && fileName.endsWith(Configuration.getFeedFileEndWith)) {
              logger.info("New file detected: {}", fileName)
              processFile(new File(Configuration.getFeedsDirectory().getAbsolutePath + "\\" + fileName))
            }
          }
        }
      }
      key.reset()
      //check for any unprocessed files
      for (file <- Configuration.getFeedsDirectory().listFiles(feedFilenameFilter) if file.isFile) {
        processFile(file)
      }

      if (updateNetwork) {
        busNetwork.updateStatus()
        updateNetwork = false
      }

      try {
        Thread.sleep(Configuration.getMonitorThreadSleepInterval())
      } catch {
        case e: InterruptedException => logger.error("iBusMonitorThread interrupted:", e)
      }

    }
  }

  private def processFile(file: File): Unit = {
//    if (file.getName.startsWith(Configuration.getFeedFileStartWith) && file.getName.endsWith(Configuration.getFeedFileEndWith)) {
      if (file.isFile && file.exists() && file.canRead() && file.canExecute) {
        logger.info("Processing file [{}].", file.getAbsolutePath)
        while (file.exists()) {
          try {
            processFeed(file)
          } catch {
            case e: FileNotFoundException => logger.error("Exception:", e)
          }
        }
        updateNetwork = true
      }
//    } else {
    //      logger.trace("File [{}] not for processing.", file.getName)
    //    }
  }

  @throws(classOf[FileNotFoundException])
  private def processFeed(file: File): Unit = {
    val source = Source.fromFile(file.getAbsolutePath)
    //Check whether to drop header
    for (line <- source.getLines().drop(if (Configuration.getFeedFileHeader) 1 else 0)) {
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
