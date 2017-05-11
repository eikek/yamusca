package yamusca

import yamusca.data._
import yamusca.context._

object mustache {

  def render(t: Template)(c: Context)(implicit r: Expand[Seq[Element]]): String = {
    val b = new StringBuilder
    r(b append _)(t.els).result(c)
    b.toString
  }

  def renderTo(t: Template)(c: Context, f: String => Unit)(implicit r: Expand[Seq[Element]]): Unit =
    r(f)(t.els).result(c)

  def parseTemplate(s: String) = parser.parse(s)

  object syntax {

    implicit final class TemplateOps(val t: Template) extends AnyVal {

      def render(ctx: Context)(implicit r: Expand[Seq[Element]]) =
        mustache.render(t)(ctx)

      def renderTo(c: Context, f: String => Unit)(implicit r: Expand[Seq[Element]]): Unit =
        mustache.renderTo(t)(c, f)

      def asString(implicit s: Show[Template]): String =
        Template.asString(t)
    }

    implicit final class StringOps(val s: String) extends AnyVal {
      def value: Value = Value.of(s)
    }
  }


  trait Expand[T] {
    def apply(consume: String => Unit)(e: T): Find[Unit]
    def asString(e: T): Find[String] = Find { ctx =>
      val buf = new StringBuilder
      val (next, _) = apply(buf append _)(e).run(ctx)
      (next, buf.toString)
    }
  }
  object Expand {
    def apply[T <: Element](f: T => Find[String]): Expand[T] = new Expand[T] {
      def apply(consume: String => Unit)(e: T): Find[Unit] =
        f(e).map(consume)
    }

    // only &, <, >, ", and '
    def escapeHtml(s: String): String =
      "&<>\"'".foldLeft(s) { (s, c) =>
        s.replace(c.toString, s"&#${c.toInt};")
      }

    implicit val literalExpand: Expand[Literal] = Expand(e => Find.unit(e.text))

    implicit val variableExpand: Expand[Variable] = Expand {
      case Variable(key, unescape) => Find.findOrEmpty(key).map {
        case SimpleValue(s) => if (unescape) s else escapeHtml(s)
        case BoolValue(b) => if (b) b.toString else ""
        case MapValue(_, e) =>
          val s = if (e) "<empty object>" else "<non-empty object>"
          if (unescape) s else escapeHtml(s)
        case ListValue(x) =>
          if (unescape) x.toString else escapeHtml(x.toString)
        case _: LambdaValue =>
          if (unescape) "<lambda>" else escapeHtml("<lambda>")
      }
    }

    implicit lazy val sectionExpand: Expand[Section] = new Expand[Section] {
      def apply(consume: String => Unit)(s: Section): Find[Unit] = {
        val expandInner: Find[Unit] = {
          val r = seqElementExpand
          r(consume)(s.inner)
        }
        Find.findOrEmpty(s.key).flatMap {
          case v if s.inverted =>
            if (v.isEmpty) expandInner
            else Find.unit(())
          case ListValue(vs) =>
            val list = vs.map(v => expandInner.stacked(v.asContext))
            list.foldLeft(Find.unit(()))(_ andThen _)
          case LambdaValue(f) =>
            f(s).map(consume)
          case v if !v.isEmpty =>
            expandInner.stacked(v.asContext)
          case _ => Find.unit(())
        }
      }
    }

    implicit def elementExpand(implicit el: Expand[Literal], ev: Expand[Variable], es: Expand[Section]): Expand[Element] = new Expand[Element] {
      def apply(consume: String => Unit)(e: Element): Find[Unit] = e match {
        case e: Literal => el(consume)(e)
        case e: Variable => ev(consume)(e)
        case e: Section => es(consume)(e)
      }
    }

    implicit def seqElementExpand(implicit r: Expand[Element]): Expand[Seq[Element]] =
      new Expand[Seq[Element]] {
        def apply(consume: String => Unit)(es: Seq[Element]): Find[Unit] =
          es.map(r(consume)).foldLeft(Find.unit(()))(_ andThen _)
      }
  }

}
