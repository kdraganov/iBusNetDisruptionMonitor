package main

import java.io.File
import java.nio.file._
import java.text.SimpleDateFormat
import java.util.Date

import lbsl.{Network, Observation}

import scala.collection.JavaConversions._
import scala.io.Source

/**
 * Created by Konstantin on 20/01/2015.
 */
class iBusMonitor(
                   private val directoryToMonitor: String,
                   private val dateFormat: String,
                   private val busStopsFile: String,
                   private val routesFile: String,
                   private val feedDelimiter: Char,
                   private val sleepInterval: Long) extends Thread {

  val dateFormatter: SimpleDateFormat = new SimpleDateFormat(dateFormat)
  val busNetwork: Network = new Network
  val folder: File = new File(directoryToMonitor)
  val processedDirectory: File = new File(directoryToMonitor + "\\ProcessedFiles");

  override
  def run() {
    //initialize the bus network
    println("\n****************************************************\n")
    busNetwork.init(busStopsFile, routesFile)
    println("\n****************************************************\n")
    println("Start monitoring folder " + folder.getAbsolutePath + " for new file feeds.\n")

    val watchService = FileSystems.getDefault.newWatchService()
    Paths.get(folder.getAbsolutePath).register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)

    while (true) {
      val key = watchService.take()
      val events = key.pollEvents()
      var update: Boolean = false;
      for (event <- events) {
        val event_path = event.context().asInstanceOf[Path]
        val fileName = event_path.toString()
        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
          println("New file detected: " + fileName)
          if (fileName.startsWith("CC_") && fileName.endsWith(".csv")) {
            val file: File = new File(folder.getAbsolutePath + "\\" + fileName)
            if (file.isFile && file.exists() && file.canRead() && file.canExecute) {
              println("Processing file " + fileName + ".")
              processFile(file)
              update = true
            }
          } else {
            println("File " + fileName + " not for processing.")
          }

        }
      }
      key.reset()
      if (update) {
        busNetwork.calculateDisruptions()
      }
      Thread.sleep(sleepInterval)
    }
  }

  def processFile(file: File): Unit = {
    val source = Source.fromFile(file.getAbsolutePath)
    for (line <- source.getLines().drop(1)) {
      processFeed(line)
    }
    source.close

    if (!processedDirectory.exists()) {
      println("Creating directory: " + processedDirectory)
      processedDirectory.mkdir()
    }
    val sourceFile = FileSystems.getDefault.getPath(file.getAbsolutePath)
    val destinationFile = FileSystems.getDefault.getPath(processedDirectory.getAbsolutePath, file.getName)
    Files.move(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING)
  }

  def processFeed(feed: String): Unit = {
    val tokens: Array[String] = feed.split(feedDelimiter)
    val scheduleDeviation: Integer = Integer.parseInt(tokens(11))
    val tripType: Integer = Integer.parseInt(tokens(8))
    // ignore invalid readings
    if (scheduleDeviation != -2147483645 && lbsl.TripType.isActiveTrip(tripType)) {
      val vehicleId: Integer = Integer.parseInt(tokens(0))
      val longitude: Double = tokens(12).toDouble
      val latitude: Double = tokens(13) toDouble
      val eventId: Integer = Integer.parseInt(tokens(14))
      val timeOfData: Date = dateFormatter.parse(tokens(3))
      val routeNumber = tokens(9)
      val lastStop = tokens(10)
      val observation: Observation = new Observation(vehicleId, timeOfData, tripType, routeNumber, lastStop, scheduleDeviation, longitude, latitude, eventId)
      //TODO: PROBLEM - WHAT TO DO HERE AND WHERE TO STORE THE INFO SHOULD WE DISCARD OLD INFO
      val route = busNetwork.getRoute(routeNumber)
      if (route != null) {
        route.addObservation(observation)
      }
    }



    //    print(observation.getVehicleId + "|")
    //    print(observation.getEventId + "|")
    //    print(observation.getLastStop + "|")
    //    print(observation.getLatitude + "|")
    //    print(observation.getLongitude + "|")
    //    print(observation.getRoute + "|")
    //    print(observation.getScheduleDeviation + "|")
    //    print(dateFormatter.format(observation.getTimeOfData) + "|")
    //    println(observation.getTripType + "|")
    //
    //    print(tokens.length + " => ")
    //    for (cell <- tokens) {
    //      print(cell + "|")
    //    }
  }

}
