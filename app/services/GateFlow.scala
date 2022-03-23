package services

import akka.NotUsed
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink, Source}
import akka.stream.{Graph, Materializer, QueueOfferResult}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class GateFlow[T](bufferSize: Int)(implicit materializer: Materializer) {
  import GateFlow._

  private val (sourceQueue, source) =
    Source.queue[T](bufferSize).preMaterialize()
  private val (sinkQueue, sink) = Sink.queue[T]().preMaterialize()

  val graph: Graph[GateShape[T, Status], NotUsed] = GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val in = builder.add(sink)
      val src = builder.add(source)
      val increment = builder.add(Flow[T].map(_ => +1))
      val decrement = builder.add(Flow[T].map(_ => -1))
      val counter = builder.add(queueCounter)
      val toStatus = builder.add(countToStatus)
      val merge = builder.add(Merge[Int](2))
      val bCastInput = builder.add(Broadcast[T](2))
      val bCastOutput = builder.add(Broadcast[T](2))

      // Main flow
      bCastInput.out(0) ~> in
      src ~> bCastOutput.in

      // Status
      bCastInput.out(1) ~> increment ~> merge.in(0)
      bCastOutput.out(1) ~> decrement ~> merge.in(1)
      merge.out ~> counter ~> toStatus

      GateShape(
        bCastInput.in,
        bCastOutput.out(0),
        toStatus.out
      )
  }

  def latch(): Boolean = {
    val t = Await.result(sinkQueue.pull(), 1 millisecond)
    t.map(sourceQueue.offer) match {
      case Some(QueueOfferResult.Enqueued) => true
      case Some(QueueOfferResult.Dropped)  => false
      case None                            => false
    }
  }

  private def countToStatus = Flow[Int].statefulMapConcat { () =>
    var status: Status = Empty

    (c: Int) => {
      val newStatus = {
        if (c == 0) Empty
        else NonEmpty
      }
      if (newStatus != status) {
        status = newStatus
        status :: Nil
      } else
        Nil
    }
  }

  private def queueCounter =
    Flow[Int]
      .statefulMapConcat { () =>
        var count: Int = 0

        (i: Int) => {
          count += i
          count :: Nil
        }
      }
}

object GateFlow {
  trait Status
  object Empty extends Status
  object NonEmpty extends Status
}
