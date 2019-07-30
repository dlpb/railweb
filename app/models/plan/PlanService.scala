package models.plan

import java.io.{BufferedReader, FileNotFoundException, InputStreamReader}
import java.net.URL
import java.util.zip.{GZIPInputStream, ZipInputStream}

import models.timetable.{JsonFormats, SimpleTimetable}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse

import scala.io.Source

class PlanService {
  def getTrainsForLocation(loc: String,
                           year: Int,
                           month: Int,
                           day: Int,
                           from: Int,
                           to: Int
                          ): List[SimpleTimetable] = {
    readTimetable(loc, year, month, day, from, to).toList
  }

  def readTimetable(loc: String,
                    year: Int,
                    month: Int,
                    day: Int,
                    from: Int,
                    to: Int
                   ): Seq[SimpleTimetable] = {
    implicit val formats = DefaultFormats ++ JsonFormats.allFormats

    try {
      val m = if(month < 1) "01" else if (month < 10) s"0$month" else if (month > 12) "12" else s"$month"
      val d = if(day < 1) "01" else if (day < 10) s"0$day" else if (day > 31) "31" else s"$day"
      val f = if(from < 0) "0000" else if (from < 10) s"000$from" else if(from < 100) s"00$from" else if (from < 1000) s"0$from" else if (from > 2400) "2400" else s"$from"
      val t = if(to < 0) "0000" else if (to < 10) s"000$to" else if(to < 100) s"00$to" else if (to < 1000) s"0$to" else if (to > 2400) "2400" else s"$to"
      val url = s"http://railweb-timetables.herokuapp.com/timetables/location/$loc?year=$year&month=$m&day=$d&from=$f&to=$t"
      val is = new URL(url).openStream()

      val zis = new GZIPInputStream(is)

      val string = Source.fromInputStream(zis).mkString
      parse(string).extract[Seq[SimpleTimetable]]
    }
    catch {
      case f: FileNotFoundException => println(s"No timetable for location $loc")
        Seq.empty
      case e: Exception => println(s"Something went wrong: ${e.getMessage}")
        Seq.empty
    }
  }

}
