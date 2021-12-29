package controllers

import play.api._
import play.api.mvc._
import services.ApplicationService

import javax.inject.Inject

class HomeController @Inject() (cc: ControllerComponents, app: ApplicationService) extends AbstractController(cc) with Logging {
  def clock() = Action { implicit request: Request[AnyContent] =>
    logger.info("Clock request")
    app.clock()
    Ok("Success!")
  }

  def gameOfLife() = Action { implicit request: Request[AnyContent] =>
    logger.info("Clock request")
    app.gameOfLife()
    Ok("Success!")
  }

  def message(sender: String, message: String) = Action { implicit request: Request[AnyContent] =>
    logger.info(s"Message request (sender: '$sender', message: '$message')")
    app.message(sender, message)
    Ok("Success!")
  }
}
