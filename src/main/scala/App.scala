import Clock.TimeImage
import SignController.{Display, DisplayFailed, DisplaySucceeded}
import akka.actor.{Actor, ActorLogging, Props}

import scala.concurrent.duration._
import scala.language.postfixOps

object App {
  def props() = Props(new App)
}

class App extends Actor with ActorLogging {
  val size = (84, 7)
  val serializer = context.actorOf(Serializer.props("dev/tty.usbserial-0001"), "serializer")
  val clock = context.actorOf(Clock.props(size, 2 seconds), "clock")
  val signs = Map(
    "top" -> context.actorOf(Sign.props(1, size, serializer), "top"),
    "bottom" -> context.actorOf(Sign.props(2, size, serializer), "bottom"),
  )
  val signController = context.actorOf(SignController.props(signs, serializer), "controller")
  val displaySequencer = new Sequencer()

  override def receive: Receive = {
    case TimeImage(image) => signController ! Display(displaySequencer.nextSeq(), Map(
      "top" -> image,
      "bottom" -> image,
    ))
    case DisplaySucceeded(id) => log.info("Displayed {}", id)
    case DisplayFailed(id, message) => log.error(message)
  }
}
