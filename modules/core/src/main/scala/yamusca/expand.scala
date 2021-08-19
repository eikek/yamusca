package yamusca

import yamusca.context._
import yamusca.data._

object expand {

  def renderResult(t: Template)(c: Context)(implicit r: Expand[Template]): String =
    render(t)(c)._2

  def render(t: Template)(c: Context)(implicit r: Expand[Template]): (Context, String) = {
    val b         = new Buffer
    val (next, _) = r(b.append(_))(t).run(c)
    (next, b.toString)
  }

  def renderWithMissingKeys(
      t: Template
  )(c: Context)(implicit r: Expand[Template]): (List[String], Context, String) = {
    val (next, v) = render(t)(c :: MissingKeysContext(Nil))
    val ctx       = next.head.getOrElse(next)
    next.tail match {
      case Some(MissingKeysContext(ks)) => (ks, ctx, v)
      case _                            => (Nil, ctx, v)
    }
  }

  def renderTo(
      t: Template
  )(c: Context, f: String => Unit)(implicit r: Expand[Template]): Unit =
    r(f)(t).result(c)

  private case class MissingKeysContext(names: List[String]) extends Context {
    def find(key: String): (Context, Option[Value]) =
      (MissingKeysContext(key :: names), None)
  }

  trait Expand[T] {
    def apply(consume: String => Unit)(e: T): Find[Unit]
    def asString(e: T): Find[String] =
      Find { ctx =>
        val buf       = new Buffer
        val (next, _) = apply(buf append _)(e).run(ctx)
        (next, buf.toString)
      }
  }
  object Expand {
    def apply[T <: Element](f: T => Find[String]): Expand[T] =
      new Expand[T] {
        def apply(consume: String => Unit)(e: T): Find[Unit] =
          f(e).map(consume)
      }

    // only &, <, >, ", and '
    def escapeHtml(s: String): String =
      "&<>\"'".foldLeft(s) { (s, c) =>
        s.replace(c.toString, s"&#${c.toInt};")
      }

    implicit val literalExpand: Expand[Literal] = Expand(e => Find.unit(e.text))

    implicit val commentExpand: Expand[Comment] = Expand(_ => Find.unit(""))

    implicit val variableExpand: Expand[Variable] = Expand {
      case Variable(key, unescape) =>
        Find.findOrEmptyPath(key).map {
          case SimpleValue(s) => if (unescape) s else escapeHtml(s)
          case BoolValue(b)   => if (b) b.toString else ""
          case MapValue(_, e) =>
            val s = if (e) "<empty object>" else "<non-empty object>"
            if (unescape) s else escapeHtml(s)
          case ListValue(x) =>
            if (unescape) x.toString else escapeHtml(x.toString)
          case _: LambdaValue =>
            if (unescape) "<lambda>" else escapeHtml("<lambda>")
        }
    }

    implicit def sectionExpand(implicit
        e1: Expand[Literal],
        e2: Expand[Variable],
        e3: Expand[Comment]
    ): Expand[Section] =
      new Expand[Section] {
        def apply(consume: String => Unit)(s: Section): Find[Unit] = {
          val expandInner: Find[Unit] = {
            val r = templateExpand(elementExpand(e1, e2, this, e3))
            r(consume)(Template(s.inner))
          }

          Find.findOrEmptyPath(s.key).flatMap {
            case v if s.inverted =>
              if (v.isEmpty) expandInner
              else Find.unit(())
            case ListValue(vs) =>
              val list = vs.zipWithIndex.map { case (v, i) =>
                expandInner
                  .stacked(v.asContext)
                  .stacked(Context.indexContext(i, vs.size))
              }
              list.foldLeft(Find.unit(()))(_ andThen _)
            case LambdaValue(f) =>
              f(s).map(consume)
            case v if !v.isEmpty =>
              expandInner.stacked(v.asContext)
            case _ =>
              Find.unit(())
          }
        }
      }

    implicit def elementExpand(implicit
        e1: Expand[Literal],
        e2: Expand[Variable],
        e3: Expand[Section],
        e4: Expand[Comment]
    ): Expand[Element] =
      new Expand[Element] {
        def apply(consume: String => Unit)(e: Element): Find[Unit] =
          e match {
            case v: Literal  => e1(consume)(v)
            case v: Variable => e2(consume)(v)
            case v: Section  => e3(consume)(v)
            case v: Comment  => e4(consume)(v)
          }
      }

    implicit def templateExpand(implicit r: Expand[Element]): Expand[Template] =
      new Expand[Template] {
        def apply(consume: String => Unit)(t: Template): Find[Unit] =
          t.els.map(r(consume)).foldLeft(Find.unit(()))(_ andThen _)
      }
  }

  final private class Buffer(val buffer: StringBuilder = new StringBuilder()) {
    def append(str: String): Unit = {
      buffer.append(str)
      ()
    }

    override def toString() = buffer.toString()
  }
}
