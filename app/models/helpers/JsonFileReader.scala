package models.helpers

import java.io.InputStream

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.{parse => json4sParse}

import scala.io.Source

class JsonFileReader {
  def readAndParse[T](path: String)(implicit m: Manifest[T]): T = {
    parse(read(path))
  }

  def read(path: String): String = {
    val data: InputStream = getClass().getResourceAsStream(path)
    Source.fromInputStream(data).mkString
  }


  def parse[T](jsonData: String)(implicit m: Manifest[T]): T = {
    implicit val formats = DefaultFormats
    json4sParse(jsonData).extract[T]
  }

}
