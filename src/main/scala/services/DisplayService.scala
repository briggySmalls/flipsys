package services

import akka.actor.Cancellable
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import services.StreamTypes.DisplayPayload

import scala.util.chaining.scalaUtilChainingOps



class DisplayService(val sources: Map[String, Source[DisplayPayload, Cancellable]], val sink: Sink[DisplayPayload, _])(implicit materializer: Materializer) {
  var stream: Option[Cancellable] = None

  def start(source: String): Either[String, Unit] = {
    stream.map(_.cancel())
    sources
      .get(source)
      .toRight(s"Source $source not recognised")
      .map(cancellableStream)
      .tap(e => stream = e.toOption)
      .map(_ => ())
  }

  private def cancellableStream(source: Source[DisplayPayload, Cancellable]): Cancellable =
    source
    .to(sink)
    .run()
}
