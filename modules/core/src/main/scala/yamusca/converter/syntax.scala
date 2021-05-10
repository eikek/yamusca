package yamusca.converter

import yamusca.context.{Context, Value}
import yamusca.data.Template
import yamusca.expand
import yamusca.parser.parse

trait syntax {

  implicit final class AnyValue[A](val a: A) {
    def asMustacheValue(implicit c: ValueConverter[A]): Value = c(a)

    def asContext(implicit c: ValueConverter[A]): Context =
      asMustacheValue.asContext

    def unsafeRender(templ: String)(implicit c: ValueConverter[A]): String =
      parse(templ) match {
        case Right(t)  => render(t)
        case Left(err) => sys.error(s"Error in template: $err")
      }

    def render(templ: Template)(implicit c: ValueConverter[A]): String =
      expand.renderResult(templ)(asContext)
  }
}

object syntax extends syntax
