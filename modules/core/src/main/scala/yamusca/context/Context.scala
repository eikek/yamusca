package yamusca.context

trait Context {
  def find(key: String): (Context, Option[Value])

  /** Prepend the given context to this one. */
  def ::(head: Context): Context = Context.prepend(head, this)

  /** If stacked, removes the head context and returns the rest. */
  def tail: Option[Context] =
    None

  /** If stacked returns the head context */
  def head: Option[Context] =
    None
}

object Context {
  private def prepend(c1: Context, c2: Context): Context =
    (c1, c2) match {
      case (StackedContext(ch, ct), StackedContext(dh, dt)) =>
        StackedContext(ch, ct ::: List(dh) ::: dt)
      case (StackedContext(ch, ct), d) => StackedContext(ch, ct ::: List(d))
      case (c, StackedContext(dh, dt)) => StackedContext(c, dh :: dt)
      case (c, d)                      => StackedContext(c, List(d))
    }

  val empty: Context = new Context {
    def find(key: String) = (this, None)
    override def toString(): String = "Context.empty"
  }

  def fromMap(m: Map[String, Value]): Context =
    new Context {
      def find(key: String) = (this, m.get(key))
    }

  def apply(ts: (String, Value)*): Context = fromMap(Map(ts: _*))

  def from(f: String => Option[Value]): Context =
    new Context {
      def find(key: String) = (this, f(key))
    }

  def indexContext(index: Int, length: Int): Context =
    Context(
      "-first" -> Value.of(index == 0),
      "-last" -> Value.of(index == length - 1),
      "-index" -> Value.of(s"${1 + index}")
    )

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
              case (next, _) =>
                loop(rest, tried :+ next)
            }
          case _ =>
            if (tried.isEmpty) (Context.empty, None)
            else (StackedContext(tried.head, tried.tail.toList), None)
        }

      loop(first :: rest, Vector.empty)
    }

    override def head: Option[Context] =
      Some(first)

    override def tail: Option[Context] =
      rest match {
        case Nil      => None
        case a :: Nil => Some(a)
        case a :: as  => Some(StackedContext(a, as))
      }
  }
}
