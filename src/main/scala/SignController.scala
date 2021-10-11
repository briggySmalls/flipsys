import Sign.ShowImageSucceeded
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import data.Image

object SignController {
  trait ImageCommand {
    def id: Long
  }
  case class Display(id: Long, images: Map[String, Image]) extends ImageCommand
  case class DisplaySucceeded(id: Long) extends ImageCommand
  case class DisplayFailed(id: Long, message: String) extends ImageCommand

  def props(signs: Map[String, ActorRef], serializer: ActorRef) =
    Props(new SignController(signs, serializer))
}

class SignController(private val signs: Map[String, ActorRef], private val serializer: ActorRef) extends Actor with ActorLogging {
  import Sign.{ShowImage, ShowImageFailed, props}
  import SignController._
  import DisplayTransaction._

  val transactionSequencer = new Sequencer()
  val imageSequencer = new Sequencer()

  def receive = {
    case request @ Display(requestId, images) => {
      val idToSign = images.map({case (id, _) => id -> signs.get(id)})
      val invalid = idToSign.toSeq.find({ case (_, sign) => sign.isEmpty})
      if (invalid.isDefined) sender() ! DisplayFailed(requestId, s"Unrecognised sign: ${invalid.get._1}")
      else
        context.actorOf(DisplayTransaction.props(
          sender(),
          request,
          images.map({case (signId, image) => signs(signId) -> ShowImage(imageSequencer.nextSeq(), image)})
        ), s"transaction-${transactionSequencer.nextSeq()}")
    }
    case DisplayComplete(requester, Display(id, _)) => requester ! DisplaySucceeded(id)
  }
}
