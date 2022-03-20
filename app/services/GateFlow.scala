package services

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, QueueOfferResult}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class GateFlow[T](bufferSize: Int)(implicit materializer: Materializer) {
  private val (sourceQueue, source) =
    Source.queue[T](bufferSize).preMaterialize()
  private val (sinkQueue, sink) = Sink.queue[T].preMaterialize()

  val flow: Flow[T, T, NotUsed] = Flow.fromSinkAndSource(sink, source)

  def latch(): Boolean = {
    val t = Await.result(sinkQueue.pull(), 1 millisecond)
    t.map(sourceQueue.offer) match {
      case Some(QueueOfferResult.Enqueued) => true
      case Some(QueueOfferResult.Dropped)  => false
      case None                            => false
    }
  }
}
