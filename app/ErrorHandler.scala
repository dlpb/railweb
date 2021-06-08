import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent._
import javax.inject.Singleton

@Singleton
class ErrorHandler extends HttpErrorHandler {
  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    statusCode match {
      case 400 => Future.successful(Status(statusCode)(views.html.error.index("Bad Request", statusCode, message, "")))
      case 401 | 403 => Future.successful(Status(statusCode)(views.html.error.index("Unauthorized", statusCode, "Please go to the home page and log in", message)))
      case 404 => Future.successful(Status(statusCode)(views.html.error.index("Not Found", statusCode, message, s"Could not find ${request.path}")))
      case 405 => Future.successful(Status(statusCode)(views.html.error.index("Method not allowed", statusCode, message, "")))
      case _ => Future.successful(Status(statusCode)(views.html.error.index("Oops, there was something wrong with the request", statusCode, message, request.toString)))
    }

  }

  def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    Future.successful(InternalServerError(views.html.error.index(
      "Oops! Something went wrong!",
      500,
      exception.getMessage,
      exception.getClass.getName.split("\\.").last + "\n" + exception.getStackTrace.toList.map(_.toString).mkString("\n"))))

  }
}