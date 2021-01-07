package models

import cats.implicits._

sealed trait ParseError {
  def message: String
}

object ParseError extends ParseHandling {

  final case class DestinationEmpty(message: String)                extends ParseError
  final case class DepartureEmpty(message: String)                  extends ParseError

  def sequenceErrors[A](input: Seq[ParseHandler[A]]): ParseHandler[Seq[A]] = {
    input.toList.sequence.map { _.toSeq }
  }

}