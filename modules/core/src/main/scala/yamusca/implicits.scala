package yamusca

import yamusca.data.Template
import yamusca.parser._

object implicits extends converter.instances with converter.syntax {

  implicit class StringInterpolations(sc: StringContext) {
    def mustache(args: Any*): Template =
      parse(value(args)) match {
        case Right(t) => t
        case Left(err) => sys.error(s"Cannot parse mustache template: $err")
      }

    private[this] def value(args: Seq[Any]): String =
      sc.s(args: _*)
  }

}
