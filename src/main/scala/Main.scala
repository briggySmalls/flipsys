import akka.actor.ActorSystem
import akka.stream.scaladsl.RunnableGraph

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("flipsys")
    RunnableGraph.fromGraph(App.graph).run()
  }
}
