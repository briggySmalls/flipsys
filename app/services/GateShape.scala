package services

import akka.stream.{Inlet, Outlet, Shape}

import scala.collection.immutable

case class GateShape[Flow, Status](
    in: Inlet[Flow],
    out: Outlet[Flow],
    status: Outlet[Status]
) extends Shape {

  // It is important to provide the list of all input and output
  // ports with a stable order. Duplicates are not allowed.
  override val inlets: immutable.Seq[Inlet[_]] =
    in :: Nil
  override val outlets: immutable.Seq[Outlet[_]] =
    out :: status :: Nil

  // A Shape must be able to create a copy of itself. Basically
  // it means a new instance with copies of the ports
  override def deepCopy() =
    GateShape(
      in.carbonCopy(),
      out.carbonCopy(),
      status.carbonCopy()
    )

}
