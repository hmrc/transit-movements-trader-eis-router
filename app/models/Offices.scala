package models

trait Office {
  def value: String
}

final case class DepartureOffice(value: String) extends Office(value: String)
final case class DestinationOffice(value: String) extends Office(value: String)
