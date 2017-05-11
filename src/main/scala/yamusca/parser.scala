package yamusca

import yamusca.data._

object parser {

  case class ParseErr(message: String, index: Int)
  type ParseResult = Either[ParseErr, Template]

  sealed trait Token {
    def index: Int
  }

  object Token {
    case class Text(s: String, index: Int) extends Token
    sealed trait Open extends Token
    case class OpenSection(index: Int) extends Open
    case class OpenInverse(index: Int) extends Open
    case class OpenEndSection(index: Int) extends Open
    case class OpenVariable(index: Int) extends Open
    case class OpenUnescape(index: Int) extends Open
    case class Close(index: Int) extends Token

    val openKinds = Map[String, Int => Token](
      "{{#" -> OpenSection.apply
        , "{{^" -> OpenInverse.apply
        , "{{/" -> OpenEndSection.apply
        , "{{" -> OpenVariable.apply
        , "{{&" -> OpenUnescape.apply
    )

    val openTokens: List[String] = openKinds.keys.toList.sortBy(-_.length)
    val closeTokens: List[String] = List("}}")
  }

  private def tokenize1(s: String, pos: Int, open: Boolean): Stream[Token] = {
    def findToken(delim: String): Option[(Int, String)] =
      s.indexOf(delim, pos) match {
        case -1 => None
        case n => Some((n, delim))
      }

    def sort(in: List[Option[(Int,String)]]): List[(Int, String)] =
      in.collect({case Some(x) => x}).sortBy(_._1)

    val next = sort {
      if (!open) Token.openTokens.map(findToken)
      else Token.closeTokens.map(findToken)
    }

    next.headOption match {
      case Some((n, t)) =>
        val delim = if (open) Token.Close(n) else Token.openKinds(t)(n)
        val rest = delim #:: tokenize1(s, n+t.length, !open)
        val prefix = s.substring(pos, n)
        if (prefix.isEmpty) rest
        else Token.Text(prefix, pos) #:: rest
      case None =>
        val last = s.substring(pos)
        if (last.isEmpty) Stream.empty
        else Stream(Token.Text(last, pos))
    }
  }

  def tokenize(s: String): Stream[Token] =
    tokenize1(s, 0, false)



  private def err[A](msg: String, index: Int): Either[ParseErr, A] = Left(ParseErr(msg, index))


  def parse(s: String): ParseResult = {
    import Token._

    def loop(tokens: Stream[Token], result: Vector[Element], name: List[String]): Either[ParseErr, (Vector[Element], Stream[Token])] = {
      tokens match {
        case a #:: b #:: c #:: rest =>
          a match {
            case Text(t, _) =>
              loop(b #:: c #:: rest, result :+ Literal(t), name)

            case Close(idx) =>
              err("Expected literal or {{, but got }}", idx)

            case OpenVariable(_) =>
              (b, c) match {
                case (Text(t, _), Close(_)) =>
                  loop(rest, result :+ Variable(t.trim), name)
                case _ =>
                  err(s"Expected text after {{, but got $b$c", b.index)
              }

            case OpenUnescape(_) =>
              (b, c) match {
                case (Text(t, _), Close(_)) =>
                  loop(rest, result :+ Variable(t.trim, true), name)
                case _ =>
                  err(s"Expected text after {{&, but got $b$c", b.index)
              }

            case OpenEndSection(_) =>
              (b, c) match {
                case (Text(t, _), Close(_)) =>
                  if (Some(t) == name.headOption) Right((result, rest))
                  else err(s"Expected end-section named ${name.headOption}, but got endsection ${Some(t)}", b.index)
                case _ =>
                  err(s"Expected text after {{/, but got $b$c", b.index)
              }
            case OpenSection(_) =>
              (b, c) match {
                case (Text(t, _), Close(_)) =>
                  loop(rest, Vector.empty, t :: name) match {
                    case Right((inner, more)) =>
                      loop(more, result :+ Section(t.trim, inner.toSeq), name)
                    case Left(e) => Left(e)
                  }
                case _ =>
                  err(s"Expected text after {{#, but got $b$c", b.index)
              }
            case OpenInverse(_) =>
              (b, c) match {
                case (Text(t, _), Close(_)) =>
                  loop(rest, Vector.empty, t :: name) match {
                    case Right((inner, more)) =>
                      loop(more, result :+ Section(t.trim, inner.toSeq, true), name)
                    case Left(e) => Left(e)
                  }
                case _ =>
                  err(s"Expected text after {{^, but got $b$c", b.index)
              }
          }

        case a #:: b #:: Stream.Empty =>
          err(s"Invalid template: $a$b. Maybe a closing }} missing?", a.index)

        case a #:: Stream.Empty =>
          a match {
            case Text(t, _) => Right((result :+ Literal(t), Stream.empty))
            case o: Open => err(s"Expected text but got $a", o.index)
            case Close(idx) => err(s"Expected text but got }}", idx)
          }
        case _ =>
          Right((result, Stream.empty))
      }
    }

    loop(tokenize(s), Vector.empty, Nil) match {
      case Right((els, Stream.Empty)) => Right(Template(els))
      case Right((els, rest)) => err(s"Unbalanced template: ${rest.toList}", rest.head.index)
      case Left(err) => Left(err)
    }
  }
}
