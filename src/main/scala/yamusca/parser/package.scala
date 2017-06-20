package yamusca

import yamusca.data.Template

package object parser {

  type ParseResult[A] = Either[(ParseInput, String), (ParseInput, A)]
  type Parser[A] = ParseInput => ParseResult[A]

  final def parse(s: String): Either[(ParseInput, String), Template] = {
    mustache.parseTemplate(ParseInput(s)).map(r => r._2)
  }

  implicit final class ParserOps[A](val p: Parser[A]) extends AnyVal {
    def flatMap[B](f: A => Parser[B]): Parser[B] = { in =>
      p(in) match {
        case Right((next, a)) => f(a)(next)
        case Left((next, err)) => Left((next, err))
      }
    }

    def map[B](f: A => B): Parser[B] =
      flatMap(a => in => Right(in -> f(a)))

    def emap[B](f: A => Either[String, B]): Parser[B] =
      flatMap(a => in => f(a) match {
        case Right(b) => Right(in -> b)
        case Left(err) => Left(in -> err)
      })

    def ~[B](next: Parser[B]): Parser[(A,B)] =
      flatMap(a => next.map(b => (a,b)))

    def or[B >: A](other: Parser[B]): Parser[B] = { in =>
      p(in) match {
        case Left(_) => other(in)
        case r@ Right(_) => r
      }
    }

    def opt: Parser[Option[A]] = { in =>
      p(in) match {
        case Right((in, a)) => Right((in, Some(a)))
        case Left((in, _)) => Right((in, None))
      }
    }
  }
}
