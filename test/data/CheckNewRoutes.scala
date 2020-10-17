package data

import java.io.InputStream

import models.route.{Route, RoutePoint}
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.io.Source

class CheckNewRoutes extends AnyFlatSpec with Matchers {
  it should "work" in {
    def makeRoutes(routes: String) = {
      implicit val formats = DefaultFormats
      parse(routes).extract[Set[Route]]
    }

    def readRoutesFromFile = {
      val path = "/data/static/routes.json"
      val data: InputStream = getClass().getResourceAsStream(path)
      Source.fromInputStream(data).mkString
    }

    val routes = makeRoutes(readRoutesFromFile)


    routes map {
      r =>
        s"""${r.from.id},${r.to.id},${r.toc},${r.singleTrack},${r.electrification},${r.speed},${r.srsCode},,${r.`type`}"""
    } foreach println

  }

  case class OptionalRoute(
                    from: RoutePoint,
                    to: RoutePoint,
                    toc: Option[String],
                    singleTrack: Option[String],
                    electrification: Option[String],
                    speed: Option[String],
                    srsCode: Option[String],
                    `type`: Option[String],
                    distance: Long = 0
                  )

}
