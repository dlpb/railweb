package controllers.tocs.all

import javax.inject.Inject
import models.location.LocationsService
import models.toc.{PresentationTocData, TocService}
import play.api.mvc.{AbstractController, ControllerComponents}

class AllTocController @Inject()(
                                             cc: ControllerComponents,
                                             locationsService: LocationsService,
                                             tocService: TocService
                                           ) extends AbstractController(cc){

  def index(query: String) = Action {implicit request =>
    val allToc = tocService.getAll.toList.sortBy(_.id)

    val allTocs = query.toUpperCase match {
      case "" => allToc
      case q => allToc.filter { t =>
        t.id.toUpperCase().contains(q) ||
        t.name.toUpperCase().contains(q)
      }
    }

    Ok(views.html.toc.all.index(query, allTocs))
  }

  def js = Action {implicit request =>
    val jsTocs = PresentationTocData(tocService.getAll.toList)

    import org.json4s._
    import org.json4s.jackson.Serialization.writePretty
    implicit val formats = DefaultFormats
    val json = writePretty(jsTocs)

    def jsStr =
      s"""
        |const tocs = $json
        |

        |function findTocData(toc) {
        |    if (undefined === tocs[toc]) {
        |        return tocs['_DEFAULT']
        |    }
        |    return tocs[toc]
        |}
        |
        |function allTocs() {
        |    return tocs;
        |}
        |""".stripMargin

    Ok(jsStr).as("text/javascript")
  }
}
