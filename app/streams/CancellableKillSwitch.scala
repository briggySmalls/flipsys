package streams

import akka.actor.Cancellable
import akka.stream.{KillSwitch, KillSwitches}
import akka.stream.scaladsl.{Flow, Keep}

class CancellableKillSwitch(private val killSwitch: KillSwitch) extends Cancellable {
  private var isCancelledState = false

  override def cancel: Boolean = {
    killSwitch.shutdown()
    isCancelledState = true
    isCancelledState
  }

  override def isCancelled: Boolean = isCancelledState
}

