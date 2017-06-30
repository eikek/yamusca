package yamusca.converter

import yamusca.parser.parse
import yamusca.expand
import yamusca.data.Template
import yamusca.context.Value

trait syntax {

  implicit final class AnyValue[A](val a: A) {
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

object syntax extends syntax
