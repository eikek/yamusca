package yamusca.parser

trait Parsers {
  val nextChar: Parser[Char] = { in =>
    in.charAt(0) match {
      case Some(c) => Right((in.dropLeft(1), c))
      case None    => Left((in, "Input exhausted"))
    }
  }

  def nextChar(p: Char => Boolean): Parser[Char] = { in =>
    in.charAt(0) match {
      case Some(c) if p(c) => Right((in.dropLeft(1), c))
      case Some(c)         => Left((in, "Character not expected"))
      case None            => Left((in, "Input exhausted"))
    }
  }

  def consume(s: String): Parser[String] = { in =>
    if (in.takeLeft(s.length).current == s) Right(in.dropLeft(s.length) -> s)
    else Left(in                                                        -> s"Expected '$s'")
  }

  def consume(c: Char): Parser[Char] = { in =>
    in.charAt(0) match {
      case Some(`c`) => Right((in.dropLeft(1), c))
      case _         => Left((in, s"Char is not $c"))
    }
  }

  def peek[A](p: Parser[A]): Parser[A] = { in =>
    val widen = in.copy(end = in.raw.length)
    p(widen) match {
      case Right((_, a))  => Right((in, a))
      case Left((_, err)) => Left((in, err))
    }
  }

  val newLine: Parser[String] = (consume('\r').opt ~ consume('\n')).map { case (r, n) =>
    if (r.isDefined) "\r\n" else "\n"
  }

  def consumeUntil(s: String): Parser[String] = { in =>
    in.splitAtNext(s) match {
      case Some((left, right)) => Right(right -> left.current)
      case None                => Left(in -> s"Expected string not found: $s")
    }
  }

  def consumeWhile(p: Char => Boolean): Parser[Unit] = { in =>
    @annotation.tailrec
    def go(index: Int): Int =
      if (index >= in.end || !p(in.raw.charAt(index))) index
      else go(index + 1)

    val idx = go(in.pos)
    Right((in.copy(pos = idx), ()))
  }

  val ignoreWs: Parser[Unit] = consumeWhile(c => c == ' ' || c == '\t')

  val atEnd: Parser[Unit] = { in =>
    if (in.exhausted) Right((in, ()))
    else Left(in -> "input is not exhausted")
  }

  def nonEmpty(p: Parser[String]): Parser[String] =
    p.emap(s => if (s.isEmpty) Left("Empty string") else Right(s))

  def repeat[A](p: Parser[A]): Parser[Seq[A]] = { in =>
    @annotation.tailrec
    def go(in: ParseInput, result: Vector[A]): ParseResult[Vector[A]] =
      if (in.exhausted) Right(in -> result)
      else
        p(in) match {
          case Right((next, a)) =>
            go(next, result :+ a)
          case Left((next, err)) =>
            Right(next -> result)
        }

    go(in, Vector.empty)
  }
}
