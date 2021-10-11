import Serializer.Write
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import data.Image
import data.Packet.DrawImage

import scala.concurrent.duration._
import scala.language.postfixOps

object Sign {
  trait ImageCommand {
    def id: Long
  }
  case class ShowImage(id: Long, image: Image) extends ImageCommand
  case class ShowImageSucceeded(id: Long) extends ImageCommand
  case class ShowImageFailed(id: Long, message: String) extends ImageCommand

  object Ready

  def props(address: Int, size: (Int, Int), serializer: ActorRef) = Props(new Sign(address, size, serializer))
}

class Sign(val address: Int, val size: (Int, Int), val serializer: ActorRef) extends Actor with ActorLogging {
  import context.dispatcher
  import Sign._
  private val minWaitDuration = 0 seconds

  override def receive: Receive = LoggingReceive {
    case ShowImage(id, image) => {
      size match {
        case (width, _) if (width != image.columns) => sender() ! ShowImageFailed(id, s"Incompatible image width ($image.columns, should be ${width})")
        case (_, height) if (height != image.rows) => sender() ! ShowImageFailed(id, s"Incompatible image width ($image.rows, should be ${height})")
        case _ => {
          serializer ! Write(DrawImage(address, image).bytes)
          sender() ! ShowImageSucceeded(id)
          context.system.scheduler.scheduleOnce(minWaitDuration, context.self, Ready)
          context.become(cool)
        }
      }
    }
  }

  def cool: Receive = LoggingReceive {
    case Ready => context.become(receive)
    case _: ShowImage => log.warning("Cannot draw image when in \"cool\" state")
  }
}
