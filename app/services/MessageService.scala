package services

import akka.stream.scaladsl.Source
import config.SignConfig
import models.Image
import services.StreamTypes.DisplayPayload

object MessageService {
  def messageSource(
      signs: Seq[SignConfig],
      sender: String,
      message: String
  ): Source[DisplayPayload, _] = {
    val sizes = signs.map(_.size)
    require(sizes.forall(_ == sizes.head))
    val frames = for {
      initialFrames <- Image.frames(sizes.head, s"From: $sender")
      messageFrames <- Image.frames(sizes.head, message)
    } yield {
      (initialFrames ++ messageFrames).zipWithIndex.map { case (img, i) =>
        (signs(i % signs.length).name, img)
      }
    }
    frames match {
      case Left(msg)    => throw new RuntimeException(msg)
      case Right(items) => Source(items)
    }
  }
}
