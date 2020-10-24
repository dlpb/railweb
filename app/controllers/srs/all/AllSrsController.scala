package controllers.srs.all

import javax.inject.Inject
import models.srs.{PresentationSrsData, SrsService}
import play.api.mvc.{AbstractController, ControllerComponents}

class AllSrsController @Inject()(
                                             cc: ControllerComponents,
                                             srsService: SrsService
                                           ) extends AbstractController(cc){

  def index(query: String) = Action {implicit request =>
    val allSrss = srsService.getAll.toList.sortBy(_.id)

    val allSrs = query.toUpperCase match {
      case "" => allSrss
      case q => allSrss.filter { s =>
        s.id.toUpperCase().contains(q) ||
        s.name.toUpperCase().contains(q) ||
        s.region.toUpperCase().contains(q) ||
        s.route.toUpperCase().contains(q)
      }
    }

    Ok(views.html.srs.all.index(query, allSrs))
  }

  def js = Action {implicit request =>
    val jsSrss = PresentationSrsData.toPresentationSrsMap(srsService.getAll.toList)

    import org.json4s._
    import org.json4s.jackson.Serialization.writePretty
    implicit val formats = DefaultFormats
    val json = writePretty(jsSrss)

    def jsStr =
      s"""
        |const srss = $json
        |
        |function findSrsData(srs) {
        |    if (undefined === srss[srs]) {
        |        return srss['_DEFAULT']
        |    }
        |    return srss[srs]
        |}
        |
        |function allSrss() {
        |    return srss;
        |}
        |""".stripMargin

    Ok(jsStr).as("text/javascript")
  }
}
