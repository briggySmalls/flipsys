package controllers

import play.api._
import play.api.mvc._
import services.ApplicationService

import javax.inject.Inject

class HomeController @Inject() (
    cc: ControllerComponents,
    app: ApplicationService
) extends AbstractController(cc)
    with Logging {
  def message(sender: String, message: String) = Action {
    implicit request: Request[AnyContent] =>
      logger.info(s"Message request (sender: '$sender', message: '$message')")
      app.message(sender, message)
      Ok("Success!")
  }
}
