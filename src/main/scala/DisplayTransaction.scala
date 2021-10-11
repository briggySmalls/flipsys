import Sign.{ShowImage, ShowImageFailed, ShowImageSucceeded}
import SignController.Display
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object DisplayTransaction {
  case class DisplayComplete(requester: ActorRef, request: Display)

  def props(requester: ActorRef, request: Display, images: Map[ActorRef, ShowImage]) =
    Props(new DisplayTransaction(requester, request, images))
}

class DisplayTransaction(val requester: ActorRef, val request: Display, val images: Map[ActorRef, ShowImage]) extends Actor with ActorLogging {
  import DisplayTransaction._

  images.map({case (sign, request) => sign ! request})

  var completed = Map[ActorRef, Boolean]()

  override def receive: Receive = {
    case ShowImageSucceeded(id) => markComplete(sender())
    case ShowImageFailed(id, message) => {
      log.warning(message)
      markComplete(sender())
    }
  }

  private def markComplete(sign: ActorRef) = {
    completed += sign -> true
    if (completed.values.forall(p => p)) finish()
  }

  private def finish() = {
    context.parent ! DisplayComplete(requester, request)
    context.stop(self)
  }
}
