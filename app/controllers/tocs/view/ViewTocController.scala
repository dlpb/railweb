package controllers.tocs.view

import javax.inject.Inject
import models.toc.TocService
import play.api.mvc.{AbstractController, ControllerComponents, _}

class ViewTocController @Inject()(
                                             cc: ControllerComponents,
                                             tocService: TocService
                                     ) extends AbstractController(cc){

  def index(id: String) = Action {implicit request =>
      val toc = tocService.get(id)

    if(toc.isDefined){

      val tocs = toc.get
      Ok(views.html.toc.view.index(tocs.id, tocs.name, tocs.colour.foregroundColour, tocs.colour.backgroundColour))

    }
    else {
      NotFound(s"could not fid toc with ID ${id}")
    }

  }
}
