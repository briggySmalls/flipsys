package controllers

import play.api._
import play.api.mvc._
import services.ApplicationService

import javax.inject.Inject

class HomeController @Inject() (cc: ControllerComponents, app: ApplicationService) extends AbstractController(cc) {
  def start(mode: String) = Action { implicit request: Request[AnyContent] =>
    app.start(mode) match {
      case Left(msg) => BadRequest(msg)
      case Right(_) => Ok("Success!")
    }
  }
}
