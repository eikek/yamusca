package yamusca

import yamusca.context._
import yamusca.data._
import yamusca.expand._

object syntax {

  implicit final class TemplateOps(val t: Template) extends AnyVal {
    def renderResult(ctx: Context)(implicit r: Expand[Template]) =
      expand.renderResult(t)(ctx)

    def renderTo(c: Context, f: String => Unit)(implicit r: Expand[Template]): Unit =
      expand.renderTo(t)(c, f)

    def asString(implicit s: Show[Template]): String =
      Template.asString(t)
  }

  implicit final class StringOps(val s: String) extends AnyVal {
    def value: Value = Value.of(s)
  }

  implicit final class ElementOps(val el: Element) extends AnyVal {

    def asString(implicit s: Show[Element]): String = Element.show(el)

  }
}
