package services

import akka.stream.{Inlet, Outlet, Shape}

import scala.collection.immutable

case class MessageSchedulerShape[Flow](
    pressed: Inlet[Boolean],
    message: Outlet[Flow],
    indicator: Outlet[Boolean]
) extends Shape {

  // It is important to provide the list of all input and output
  // ports with a stable order. Duplicates are not allowed.
  override val inlets: immutable.Seq[Inlet[_]] =
    pressed :: Nil
  override val outlets: immutable.Seq[Outlet[_]] =
    message :: indicator :: Nil

  // A Shape must be able to create a copy of itself. Basically
  // it means a new instance with copies of the ports
  override def deepCopy() =
    MessageSchedulerShape(
      pressed.carbonCopy(),
      message.carbonCopy(),
      indicator.carbonCopy()
    )

}
