package models

import akka.actor.Cancellable
import akka.stream.KillSwitch

class CancellableKillSwitch(killSwitch: KillSwitch) extends Cancellable {
  private var cancelledState = false

  override def cancel: Boolean = {
    killSwitch.shutdown()
    cancelledState = true
    cancelledState
  }

  override def isCancelled: Boolean = cancelledState
}
