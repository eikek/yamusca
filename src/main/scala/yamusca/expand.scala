package yamusca

import yamusca.data._
import yamusca.context._

object expand {

  def renderResult(t: Template)(c: Context)(implicit r: Expand[Template]): String = {
    render(t)(c)._2
  }

  def render(t: Template)(c: Context)(implicit r: Expand[Template]): (Context, String) = {
    val b = new StringBuilder
    val (next, _) = r(b append _)(t).run(c)
    (next, b.toString)
  }

  def renderTo(t: Template)(c: Context, f: String => Unit)(implicit r: Expand[Template]): Unit =
    r(f)(t).result(c)

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
          r(consume)(Template(s.inner))
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

    implicit def elementExpand(implicit el: Expand[Literal], ev: Expand[Variable], es: Expand[Section]): Expand[Element] =
      new Expand[Element] {
        def apply(consume: String => Unit)(e: Element): Find[Unit] = e match {
          case e: Literal => el(consume)(e)
          case e: Variable => ev(consume)(e)
          case e: Section => es(consume)(e)
        }
      }

    implicit def seqElementExpand(implicit r: Expand[Element]): Expand[Template] =
      new Expand[Template] {
        def apply(consume: String => Unit)(t: Template): Find[Unit] =
          t.els.map(r(consume)).foldLeft(Find.unit(()))(_ andThen _)
      }
  }

}
