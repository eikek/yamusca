package yamusca.parser

import yamusca.data._

object mustache extends Parsers {
  def tag(d: Delim): Parser[(Delim, String)] =
    (consume(d.start) ~ nonEmpty(consumeUntil(d.end)) ~ consume(d.end)).map {
      case ((_, name), _) => (d, name)
    }

  val parseTag: Parser[(Delim, String)] = { in =>
    if (in.delim != Delim.default) tag(in.delim)(in)
    else tag(Delim.triple).or(tag(in.delim))(in)
  }

  def parseTag(p: Char => Boolean): Parser[(Delim, Char, String)] = { in =>
    val d = in.delim
    (consume(d.start) ~ nextChar(p) ~ nonEmpty(consumeUntil(d.end)) ~ consume(d.end))
      .map { case (((_, c), name), _) => (d, c, name) }(in)
  }

  val parseVariable: Parser[Variable] =
    parseTag.map {
      case (d, name) if d == Delim.triple =>
        Variable(name.trim, true)
      case (_, name) =>
        if (name.charAt(0) == '&')
          Variable(name.substring(1).trim, true)
        else
          Variable(name.trim, false)
    }

  def standalone[A](p: Parser[A]): Parser[A] = {
    val withWs = (ignoreWs ~ p ~ ignoreWs ~ newLine.or(atEnd).map(_ => ())).map {
      case (((_, a), _), _) => a
    }

    in =>
      if (in.standaloneStart.isDefined) withWs(in)
      else Left((in, "Not standalone"))
  }

  def standaloneOr[A](p: Parser[A]): Parser[A] =
    standalone(p).or(p)

  val parseSetDelimiter: Parser[Unit] = { in =>
    tag(Delim(in.delim.start + "=", "=" + in.delim.end))(in) match {
      case Right(next, (_, name)) =>
        name.split(' ').map(_.trim).filter(_.nonEmpty).toList match {
          case s :: e :: Nil =>
            Right((next.copy(delim = Delim(s, e)), ()))
          case _ =>
            Left(in -> s"Invalid set delimiters: $name")
        }
      case Left(_, msg) => Left(in -> msg)
    }
  }

  val parseLiteral: Parser[Literal] = {
    val stag = peek(
      standalone(parseTag(c => c == '#' || c == '^' || c == '/' || c == '!' || c == '='))
    )
    val text: Parser[String] = { in =>
      in.moveRight(in.delim.start.length).splitAtNext(in.delim.start) match {
        case Some(left, right) =>
          right.standaloneStart match {
            case Some(idx) if stag(right).isRight =>
              Right((right.copy(end = in.end), left.copy(end = idx + 1).current))
            case _ =>
              Right((right.copy(end = in.end), left.current))
          }

        case None =>
          Right(in.copy(pos = in.end) -> in.current)
      }
    }

    nonEmpty(text).map(Literal.apply)
  }

  val parseComment: Parser[Comment] =
    standaloneOr(parseTag(_ == '!').cut).map { case (_, _, msg) =>
      Comment(msg)
    }

  val parseStartSection: Parser[(Boolean, String)] =
    standaloneOr(parseTag(c => c == '#' || c == '^')).map {
      case (_, c, name) if c == '^' =>
        (true, name.trim)
      case (_, _, name) =>
        (false, name.trim)
    }

  val parseEndSection: Parser[String] =
    standaloneOr(parseTag(_ == '/')).map { case (_, _, name) => name.trim }

  def consumeUntilEndSection(name: String): Parser[ParseInput] = { in =>
    val delim = in.delim.start + "/"
    val stop: ParseInput => Boolean = in =>
      ignoreWs ~ consume(name) ~ ignoreWs ~ consume(in.delim.end) (in).isRight
    @annotation.tailrec
    def go(pin: ParseInput): Option[(ParseInput, ParseInput)] =
      pin.splitAtNext(delim) match {
        case Some(left, right) if stop(right.dropLeft(delim.length)) =>
          standaloneOr(parseEndSection)(right) match {
            case Right(next, _) =>
              Some((in.copy(end = left.end), next))

            case _ =>
              go(pin.dropLeft(delim.length))
          }
        case Some(_) =>
          go(pin.dropLeft(delim.length))
        case _ =>
          None
      }

    go(in) match {
      case Some(l, r) => Right(r -> l)
      case None       => Left(in -> s"Cannot find end section: $name")
    }
  }

  val parseSection: Parser[Section] =
    parseStartSection.cut.flatMap { case (inverse, name) =>
      consumeUntilEndSection(name)
        .emap(in => parseTemplate(in).left.map(_._2).map(_._2))
        .map(t => Section(name, t.els, inverse))
    }

  lazy val parseElement: Parser[Element] =
    parseSetDelimiter
      .map(_ => Literal(""))
      .or(parseSection)
      .or(parseComment)
      .or(parseLiteral)
      .or(parseVariable)

  lazy val parseTemplate: Parser[Template] =
    repeat(parseElement).map(Template.apply)
}
