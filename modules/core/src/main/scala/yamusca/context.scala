package yamusca

import java.util.Locale
import yamusca.data.Section

object context {

  trait Context {
    def find(key: String): (Context, Option[Value])
    def :: (head: Context): Context = Context.prepend(head, this)

    def tail: Context = this match {
      case c: StackedContext =>
        c.rest match {
          case Nil => Context.empty
          case a :: rest => StackedContext(a, rest)
        }
      case _ => this
    }
  }

  private case class StackedContext(first: Context, rest: List[Context]) extends Context {
    def find(key: String): (Context, Option[Value]) = {
      @annotation.tailrec
      def loop(rest: List[Context], tried: Vector[Context]): (Context, Option[Value]) =
        rest match {
          case a :: rest =>
            a.find(key) match {
              case (next, v: Some[_]) =>
                val newCtx = (tried :+ next).toList ::: rest
                (StackedContext(newCtx.head, newCtx.tail), v)
              case (next, v) =>
                loop(rest, tried :+ next)
            }
          case _ =>
            if (tried.isEmpty) (Context.empty, None)
            else (StackedContext(tried.head, tried.tail.toList), None)
        }

      loop(first :: rest, Vector.empty)
    }
  }

  object Context {
    private def prepend(c1: Context, c2: Context): Context =
      (c1, c2) match {
        case (StackedContext(ch, ct), StackedContext(dh, dt)) => StackedContext(ch, ct ::: List(dh) ::: dt)
        case (StackedContext(ch, ct), d) => StackedContext(ch, ct ::: List(d))
        case (c, StackedContext(dh, dt)) => StackedContext(c, dh :: dt)
        case (c, d) => StackedContext(c, List(d))
      }

    val empty: Context = new Context {
      def find(key: String) = (this, None)
      override def toString(): String = "Context.empty"
    }

    def fromMap(m: Map[String, Value]): Context = new Context {
      def find(key: String) = (this, m.get(key))
    }

    def apply(ts: (String, Value)*): Context = fromMap(Map(ts: _*))

    def from(f: String => Option[Value]): Context = new Context {
      def find(key: String) = (this, f(key))
    }
  }


  sealed trait Value {
    def isEmpty: Boolean
    def asContext: Context = this match {
      case MapValue(v, _) => v
      case _ => Context.from(key => if (key == ".") Some(this) else None)
    }
  }
  object Value {
    def fromString(s: String): Value = SimpleValue(s)
    def fromBoolean(b: Boolean): Value = BoolValue(b)
    def fromSeq(vs: Seq[Value]): Value = ListValue(vs)
    def fromContext(ctx: Context, empty: Boolean): Value = MapValue(ctx, empty)
    def fromMap(m: Map[String, Value]) = fromContext(Context.fromMap(m), m.isEmpty)

    def of(s: String): Value = SimpleValue(s)
    def of(s: Option[String]): Value = SimpleValue(s getOrElse "")
    def of(b: Boolean): Value = BoolValue(b)
    def seq(vs: Value*): Value = ListValue(vs)
    def map(vs: (String, Value)*): Value = MapValue(Context(vs: _*), vs.isEmpty)
    def lambda(f: Section => Find[String]): Value = LambdaValue(f)
  }
  case class SimpleValue(v: String) extends Value {
    val isEmpty = v.isEmpty
  }
  case class BoolValue(v: Boolean) extends Value {
    val isEmpty = v == false
  }
  case class ListValue(v: Seq[Value]) extends Value {
    lazy val isEmpty = v.isEmpty
  }
  case class MapValue(ctx: Context, isEmpty: Boolean) extends Value
  case class LambdaValue(f: Section => Find[String]) extends Value {
    val isEmpty = false
  }

  @annotation.implicitNotFound("There is no ValueConverter for type `${A}' in scope.")
  type ValueConverter[A] = A => Value

  object ValueConverter {
    /** A `ValueConverter` that calls `toString` on the input value.*/
    def toDefaultString[A]: ValueConverter[A] =
      a => Value.fromString(a.toString)

    /** A `ValueConverter` that calls `fmt.format(a)` using given locale. */
    def toFormatString[A](locale: Locale, fmt: String): ValueConverter[A] =
      a => Value.fromString(fmt.formatLocal(locale, a))

    /** A `ValueConverter` that calls `fmt.format(a)` using `Locale.ROOT`. */
    def toFormatString[A](fmt: String): ValueConverter[A] =
      toFormatString(Locale.ROOT, fmt)
  }


  case class Find[+A](run: Context => (Context, A)) { self =>
    def flatMap[B](f: A => Find[B]): Find[B] = Find[B] { s =>
      val (next, a) = run(s)
      f(a).run(next)
    }

    def map[B](f: A => B): Find[B] =
      flatMap(a => Find.unit(f(a)))

    def result(s: Context): A = {
      val (_, a) = run(s)
      a
    }

    def andThen(next: Find[_]): Find[Unit] =
      for {
        _ <- self
        _ <- next
      } yield ()

    def stacked(head: Context): Find[A] =
      for {
        _ <- Find.modify(c => head :: c)
        v <- self
        _ <- Find.modify(_.tail)
      } yield v
  }

  object Find {
    def unit[A](a: A): Find[A] = Find(s => (s, a))

    def find(key: String): Find[Option[Value]] = Find(_.find(key))

    def findOrEmpty(key: String): Find[Value] =
      find(key).map(_.getOrElse(Value.of(false)))

    def findOrEmptyPath(path: String): Find[Value] = {
      if (path == "." || path.indexOf('.') == -1) findOrEmpty(path)
      else {
        val parts = path.split('.').toList
        parts.map(findOrEmpty).reduce({ (f1, f2) =>
          f1.flatMap {
            case v if !v.isEmpty =>
              f2.stacked(v.asContext)
            case v =>
              unit(v)
          }
        })
      }
    }

    def get: Find[Context] = Find(s => (s, s))

    def set(state: Context): Find[Unit] = Find(_ => (state, ()))

    def modify(f: Context => Context): Find[Unit] =
      for {
        s <- get
        _ <- set(f(s))
      } yield ()
  }

}
