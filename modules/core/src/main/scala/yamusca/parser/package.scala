package yamusca

import yamusca.data.Template

package object parser {

  type ParseResult[A] = Either[(ParseInput, String), (ParseInput, A)]
  type Parser[A]      = ParseInput => ParseResult[A]

  final def parse(s: String): Either[(ParseInput, String), Template] =
    mustache.parseTemplate(ParseInput(s)) match {
      case Right((in, t)) =>
        if (in.exhausted) Right(t)
        else Left((in, s"Error in template near ${in.pos}: ${in.current}"))
      case l @ Left(_) => l.asInstanceOf[Either[(ParseInput, String), Template]]
    }

  implicit final class ParserOps[A](val p: Parser[A]) extends AnyVal {
    def flatMap[B](f: A => Parser[B]): Parser[B] = { in =>
      p(in) match {
        case Right((next, a)) => f(a)(next)
        case l @ Left(_)      => l.asInstanceOf[ParseResult[B]]
      }
    }

    def map[B](f: A => B): Parser[B] =
      flatMap(a => in => Right(in -> f(a)))

    def emap[B](f: A => Either[String, B]): Parser[B] =
      flatMap(a =>
        in =>
          f(a) match {
            case Right(b)  => Right(in -> b)
            case Left(err) => Left(in -> err)
          }
      )

    def ~[B](next: Parser[B]): Parser[(A, B)] =
      flatMap(a => next.map(b => (a, b)))

    def or[B >: A](other: Parser[B]): Parser[B] = { in =>
      p(in) match {
        case l @ Left((next, _)) =>
          if (next.cutted <= in.pos) other(in)
          else l.asInstanceOf[ParseResult[B]]
        case r @ Right(_) => r
      }
    }

    def opt: Parser[Option[A]] = { in =>
      p(in) match {
        case Right((next, a)) => Right((next, Some(a)))
        case Left(_)          => Right((in, None))
      }
    }

    def cut: Parser[A] = { in =>
      p(in) match {
        case Right((in, a)) =>
          Right((in.cut, a))
        case l @ Left(_) =>
          l.asInstanceOf[ParseResult[A]]
      }
    }
  }
}
