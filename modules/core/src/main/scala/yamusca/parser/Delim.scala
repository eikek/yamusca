package yamusca.parser

final case class Delim(start: String, end: String) {
  val length = start.length + end.length
}

object Delim {
  val default = Delim("{{", "}}")
  val triple  = Delim("{{{", "}}}")
}
