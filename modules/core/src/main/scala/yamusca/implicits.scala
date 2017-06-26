package yamusca

import yamusca.data.Template
import yamusca.parser._
import yamusca.context._

object implicits extends converter {

  implicit class StringInterpolations(sc: StringContext) {
    def mustache(args: Any*): Template =
      parse(value(args)) match {
        case Right(t) => t
        case Left(err) => sys.error(s"Cannot parse mustache template: $err")
      }

    private[this] def value(args: Seq[Any]): String =
      sc.s(args: _*)
  }

  implicit class AnyValue[A](a: A) {
    def asMustacheValue(implicit c: ValueConverter[A]): Value = c(a)

    def unsafeRender(templ: String)(implicit c: ValueConverter[A]): String =
      parse(templ) match {
        case Right(t) => render(t)
        case Left(err) => sys.error(s"Error in template: $err")
      }

    def render(templ: Template)(implicit c: ValueConverter[A]): String =
      expand.renderResult(templ)(asMustacheValue.asContext)
  }

}
