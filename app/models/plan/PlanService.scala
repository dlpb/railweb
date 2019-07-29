package models.plan

import java.io.{BufferedReader, FileNotFoundException, InputStreamReader}
import java.net.URL
import java.util.zip.{GZIPInputStream, ZipInputStream}

import models.timetable.{JsonFormats, SimpleTimetable}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse

import scala.io.Source

class PlanService {
  def getTrainsForLocation(loc: String): List[SimpleTimetable] = {
   readTimetable(loc).toList
  }

  def readTimetable(loc: String): Seq[SimpleTimetable] = {
    implicit val formats = DefaultFormats ++ JsonFormats.allFormats

    try {
      val is = new URL(s"http://railweb-timetables.herokuapp.com/timetables/location/${loc}").openStream()

      val zis = new GZIPInputStream(is)

      val string = Source.fromInputStream(zis).mkString
      parse(string).extract[Seq[SimpleTimetable]]    }
    catch {
      case f: FileNotFoundException => println(s"No timetable for location $loc")
        Seq.empty
      case e: Exception => println(s"Something went wrong: ${e.getMessage}")
        Seq.empty
    }
  }

}
