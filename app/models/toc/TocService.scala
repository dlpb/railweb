package models.toc

import java.io.InputStream

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import models.location.LocationsService
import models.route.RoutesService
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.io.Source

@Singleton
class TocService @Inject()(locationService: LocationsService,
                           routesService: RoutesService,
                           config: Config) {

  private var toc: Set[Operator] = makeToc(readTocFromFile)

  def get(id: String): Option[Operator] = toc.find(_.id.equalsIgnoreCase(id))

  def getAll: Set[Operator] = toc

  def readTocFromFile: String = {
    val path = "/data/static/toc.json"
    val data: InputStream = getClass().getResourceAsStream(path)
    Source.fromInputStream(data).mkString  }

  def makeToc(toc: String): Set[Operator] = {
    implicit val formats = DefaultFormats
    parse(toc).extract[Set[Operator]]
  }

}
