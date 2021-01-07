package models

trait ParseHandling {

  type ParseHandler[A] = Either[ParseError, A]

}
