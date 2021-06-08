package models.plan.timetable.reader

import java.io.InputStream
import java.net.URL
import java.util.zip.GZIPInputStream

class WebZipInputStream extends Reader {
  override def getInputStream(url: String): InputStream = {
    val is = new URL(url).openStream()
    new GZIPInputStream(is)
  }
}

trait Reader {
  def getInputStream(url: String): InputStream
}