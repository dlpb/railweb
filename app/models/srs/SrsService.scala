package models.srs

import java.io.InputStream

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.location.LocationsService
import models.route.RoutesService
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.io.Source

@Singleton
class SrsService @Inject()(locationService: LocationsService,
                           routesService: RoutesService,
                           config: Config) {


  private var srs: Set[Srs] = makeSrs(readSrsFromFile)

  def get(id: String): Option[Srs] = srs.find(_.id.equalsIgnoreCase(id))

  def getAll: Set[Srs] = srs

  def readSrsFromFile: String = {
    val path = "/data/static/srs.json"
    val data: InputStream = getClass().getResourceAsStream(path)
    Source.fromInputStream(data).mkString  }

  def makeSrs(srs: String): Set[Srs] = {
    implicit val formats = DefaultFormats
    parse(srs).extract[Set[Srs]]
  }

  
}
