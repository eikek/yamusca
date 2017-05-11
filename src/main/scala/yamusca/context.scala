package yamusca

import yamusca.data.Section

object context {

  trait Context {
    def find(key: String): (Context, Option[Value])
    def :: (head: Context): Context = new StackedContext(List(head, this))
    def tail: Context = this match {
      case c: StackedContext =>
        val rest = c.cs.tail
        if (rest.isEmpty) Context.empty
        else new StackedContext(rest)
      case _ => this
    }
  }

  private case class StackedContext(val cs: List[Context]) extends Context {
    require(cs.nonEmpty, "empty list for stacked context")
    def find(key: String): (Context, Option[Value]) = {
      val (list, value) = cs.foldRight((List[Context](), None:Option[Value])) {
        case (ctx, (list, value)) =>
          if (value.isDefined) (ctx :: list, value)
          else {
            val (c, v) = ctx.find(key)
            (c :: list, v)
          }
      }
      if (list == cs) (this, value)
      else (StackedContext(list), value)
    }
    override def :: (head: Context) = StackedContext(head :: cs)
  }

  object Context {
    val empty: Context = new Context {
      def find(key: String) = (this, None)
    }

    def fromMap(m: Map[String, Value]): Context = new Context {
      def find(key: String) = (this, m.get(key))
    }

    def apply(ts: (String, Value)*): Context = fromMap(Map(ts: _*))

    def apply(f: String => Option[Value]): Context = new Context {
      def find(key: String) = (this, f(key))
    }
  }


  sealed trait Value {
    def isEmpty: Boolean
    def asContext: Context = this match {
      case MapValue(v, _) => v
      case _ => Context(key => if (key == ".") Some(this) else None)
    }
  }
  object Value {
    def fromString(s: String): Value = SimpleValue(s)
    def fromBoolean(b: Boolean): Value = BoolValue(b)

    def of(s: String): Value = SimpleValue(s)
    def of(s: Option[String]): Value = SimpleValue(s getOrElse "")
    def of(b: Boolean): Value = BoolValue(b)
    def list(vs: Value*): Value = ListValue(vs)
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

    def get: Find[Context] = Find(s => (s, s))

    def set(state: Context): Find[Unit] = Find(_ => (state, ()))

    def modify(f: Context => Context): Find[Unit] =
      for {
        s <- get
        _ <- set(f(s))
      } yield ()
  }

}
