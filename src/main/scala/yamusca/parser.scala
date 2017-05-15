package yamusca

import yamusca.data._

object parser {

  case class ParseErr(message: String, index: Int)
  type ParseResult = Either[ParseErr, Template]

  sealed trait Token {
    def index: Int
    def isStandaloneToken: Boolean = this match {
      case _: Token.SectionStart => true
      case _: Token.SectionEnd => true
      case _: Token.Comment => true
      case _ => false
    }
  }

  object Token {
    case class Text(s: String, index: Int) extends Token
    case class SectionStart(index: Int, name: String, inverted: Boolean) extends Token
    case class SectionEnd(index: Int, name: String) extends Token
    case class Variable(index: Int, name: String, unescape: Boolean) extends Token
    case class Comment(index: Int, value: String) extends Token
  }

  private case class InternToken(index: Int, value: String, kind: InternToken.Type = InternToken.Text)
  private object InternToken {
    sealed trait Type
    case object OpenSection extends Type
    case object OpenInverse extends Type
    case object OpenEndSection extends Type
    case object OpenVariable extends Type
    case object OpenUnescape extends Type
    case object OpenComment extends Type
    case object Close extends Type
    case object Text extends Type

    val types = Map[String, Type](
      "{{#" -> OpenSection
        , "{{^" -> OpenInverse
        , "{{/" -> OpenEndSection
        , "{{" -> OpenVariable
        , "{{&" -> OpenUnescape
        , "{{{" -> OpenUnescape
        , "{{!" -> OpenComment
        , "}}" -> Close
        , "}}}" -> Close
    ).withDefaultValue(Text)

    val openTokens = types.keys.filterNot(Set("}}", "}}}").contains).toList.sortBy(-_.length)
    val closeTokens = List("}}}", "}}")
  }

  private def tokenize1(s: String, pos: Int, open: Boolean): Stream[InternToken] = {
    def findToken(delim: String): Option[InternToken] =
      s.indexOf(delim, pos) match {
        case -1 => None
        case n => Some(InternToken(n, delim))
      }

    def sort(in: List[Option[InternToken]]): List[InternToken] =
      in.collect({case Some(x) => x}).sortBy(_.index)

    val next = sort {
      if (!open) InternToken.openTokens.map(findToken)
      else InternToken.closeTokens.map(findToken)
    }

    next.headOption match {
      case Some(token@InternToken(n, t, _)) =>
        val delim = if (open) InternToken(n,t, InternToken.Close) else InternToken(n, t, InternToken.types(t))
        val rest = delim #:: tokenize1(s, n+t.length, !open)
        val prefix = s.substring(pos, n)
        if (prefix.isEmpty) rest
        else InternToken(pos, prefix) #:: rest
      case None =>
        val last = s.substring(pos)
        if (last.isEmpty) Stream.empty
        else Stream(InternToken(pos, last))
    }
  }

  def tokenize(s: String): Stream[Token] = {
    import InternToken._

    def extract(a: InternToken, b: InternToken)(f: String => Token) =
      (a, b) match {
        case (InternToken(_, value, Text), InternToken(_, _, Close)) =>
          f(value)
        case _ =>
          // TODO raise error ?
          Token.Text(a.value + b.value, a.index)
      }

    def loop(ts: Stream[InternToken]): Stream[Token] =
      ts match {
        case a #:: b #:: c #:: rest =>
          a match {
            case InternToken(idx, _, OpenSection) =>
              extract(b,c)(Token.SectionStart(idx, _, false)) #:: loop(rest)

            case InternToken(idx, _, OpenInverse) =>
              extract(b,c)(Token.SectionStart(idx, _, true)) #:: loop(rest)

            case InternToken(idx, _, OpenEndSection) =>
              extract(b,c)(Token.SectionEnd(idx, _)) #:: loop(rest)

            case InternToken(idx, _, OpenVariable) =>
              extract(b,c)(Token.Variable(idx, _, false)) #:: loop(rest)

            case InternToken(idx, _, OpenUnescape) =>
              extract(b,c)(Token.Variable(idx, _, true)) #:: loop(rest)

            case InternToken(idx, value, OpenComment) =>
              extract(b,c)(Token.Comment(idx, _)) #:: loop(rest)

            case InternToken(idx, value, Close) =>
              // ignore the type and return as text
              // TODO maye throw unbalanced error here
              Token.Text(value, idx) #:: loop(b #:: c #:: rest)
            case InternToken(idx, value, Text) =>
              Token.Text(value, idx) #:: loop(b #:: c #:: rest)
          }
        case a #:: b #:: Stream.Empty =>
          // ignore types and pass as text
          Stream(Token.Text(a.value + b.value, a.index))
        case a #:: Stream.Empty =>
          // ignore all other types, pass as text
          Stream(Token.Text(a.value, a.index))
        case Stream.Empty =>
          Stream.Empty
      }

    loop(tokenize1(s, 0, false))
  }



  private def err[A](msg: String, index: Int): Either[ParseErr, A] = Left(ParseErr(msg, index))

  private[yamusca] def handleWS(tokens: Stream[Token]): Stream[Token] = {
    import Token._
    tokens match {
      case (a@Text(t1, idx1)) #:: b #:: (c@Text(t2, idx2)) #:: rest =>
        if (b.isStandaloneToken && util.isStandalone(t1, t2)) {
          Text(util.removeEndingWS(t1), idx1) #:: handleWS(b #:: Text(util.removeStartingWS(t2), idx2) #:: rest)
        } else {
          a #:: handleWS(b #:: c #:: rest)
        }

      case Text(t, idx) #:: b #:: Stream.Empty if b.isStandaloneToken =>
        // scala 2.11
        val first: Token = Text(util.removeEndingWS(t), idx)
        first #:: b #:: Stream.Empty

      case a #:: Text(t, idx) #:: Stream.Empty if a.isStandaloneToken =>
        // scala 2.11
        val second: Token = Text(util.removeEndingWS(t), idx)
        a #:: second  #:: Stream.Empty

      case a #:: rest =>
        a #:: handleWS(rest)

      case Stream.Empty =>
        Stream.Empty
    }
  }

  def parse(s: String): ParseResult = {
    import Token._

    def loop(tokens: Stream[Token], result: Vector[Element], name: List[String]): Either[ParseErr, (Vector[Element], Stream[Token])] =
      tokens match {
        case Text(t, _) #:: rest =>
          loop(rest, result :+ Literal(t), name)
        case SectionStart(idx, t, inverted) #:: rest =>
          loop(rest, Vector.empty, t :: name) match {
            case Right((inner, more)) =>
              loop(more, result :+ Section(t.trim, inner.toSeq, inverted), name)
            case Left(e) => Left(e)
          }
        case SectionEnd(idx, t) #:: rest =>
          if (Some(t) == name.headOption) Right((result, rest))
          else err(s"Expected end-section named ${name.headOption}, but got endsection ${Some(t)}", idx)

        case Variable(idx, t, unescape) #:: rest =>
          loop(rest, result :+ data.Variable(t.trim, unescape), name)

        case Comment(idx, value) #:: rest =>
          loop(rest, result :+ data.Comment(value), name)

        case Stream.Empty =>
          Right((result, Stream.empty))
      }


    loop(handleWS(tokenize(s)), Vector.empty, Nil) match {
      case Right((els, Stream.Empty)) => Right(Template(els))
      case Right((els, rest)) => err(s"Unbalanced template: ${rest.toList}", rest.head.index)
      case Left(err) => Left(err)
    }
  }
}
