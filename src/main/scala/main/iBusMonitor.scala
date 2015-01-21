package main

import java.io.{File}
import java.nio.file._
import java.text.SimpleDateFormat
import environment.Observation
import java.util.Date
import scala.io.Source

import collection.JavaConversions._

/**
 * Created by Konstantin on 20/01/2015.
 */
class iBusMonitor(directoryToMonitor: String, dateFormat: String) extends Thread {

  val dateFormatter: SimpleDateFormat = new SimpleDateFormat(dateFormat)

  val folder: File = new File(directoryToMonitor)

  override
  def run() {
    println(folder.getAbsolutePath)
    val watchService = FileSystems.getDefault.newWatchService()
    Paths.get(folder.getAbsolutePath).register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)

    while (true) {
      val key = watchService.take()
      val events = key.pollEvents()
      for (event <- events) {
        val event_path = event.context().asInstanceOf[Path]
        val fileName = event_path.toString()
        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
          println("Entry created: " + fileName)
          //          TODO:Process new file
          if(fileName.startsWith("CC_") && fileName.endsWith(".csv")){
            val file: File = new File(folder.getAbsolutePath + "\\" + fileName)
            if(file.isFile && file.exists() && file.canRead() && file.canExecute) {
              processFile(file)
            }
          }

        }
      }
      key.reset()
      Thread.sleep(1000)
    }
  }

  def processFile(file: File): Unit ={
    readFile(file)
    Files.move(FileSystems.getDefault.getPath(file.getAbsolutePath), FileSystems.getDefault.getPath(directoryToMonitor, "Processed",file.getName), StandardCopyOption.REPLACE_EXISTING)
  }

  def readFile(file: File) {
      var ignore = true
      var source = Source.fromFile(file.getAbsolutePath)
      for (line <- source.getLines()) {
        if (ignore) ignore = false else println(file.getAbsolutePath + " => has been read") //parseLine(line)
      }
      source.close

  }

  def parseLine(line: String){
    val tokens: Array[String] = line.split(";")
    val vehicleId: Integer = Integer.parseInt(tokens(0))
    val tripType: Integer = Integer.parseInt(tokens(8))
    val scheduleDeviation: Integer = Integer.parseInt(tokens(11))
    val longitude: Double = tokens(12).toDouble
    val latitude: Double = tokens(13)toDouble
    val eventId: Integer = Integer.parseInt(tokens(14))

    val timeOfData: Date = dateFormatter.parse(tokens(3))

    val observation: Observation = new Observation(vehicleId, timeOfData, tripType, tokens(9),tokens(10),scheduleDeviation, longitude, latitude, eventId)
    print(observation.getVehicleId + "|")
    print(observation.getEventId + "|")
    print(observation.getLastStop + "|")
    print(observation.getLatitude + "|")
    print(observation.getLongitude + "|")
    print(observation.getRoute + "|")
    print(observation.getScheduleDeviation + "|")
    print(dateFormatter.format(observation.getTimeOfData) + "|")
    print(observation.getTripType + "|")

//    print(tokens.length + " => ")
//    for (cell <- tokens){
//      print(cell + "|")
//    }
    println()
  }
}
