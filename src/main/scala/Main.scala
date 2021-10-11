import akka.actor.ActorSystem

object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("flipsys")
    system.actorOf(App.props())
  }
}
