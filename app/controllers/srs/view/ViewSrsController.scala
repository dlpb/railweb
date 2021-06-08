package controllers.srs.view

import javax.inject.Inject
import models.srs.SrsService
import play.api.mvc.{AbstractController, ControllerComponents, _}

class ViewSrsController @Inject()(
                                             cc: ControllerComponents,
                                             srsService: SrsService
                                     ) extends AbstractController(cc){

  def index(id: String) = Action {implicit request =>
      val srs = srsService.get(id)

    if(srs.isDefined){

      val srss = srs.get
      Ok(views.html.srs.view.index(srss.id, srss.name, srss.route, srss.region, srss.colour.foregroundColour, srss.colour.backgroundColour, srss.colour.opacity))

    }
    else {
      NotFound(s"could not fid srs with ID ${id}")
    }

  }
}
