package yamusca.context

case class Find[+A](run: Context => (Context, A)) { self =>
  def flatMap[B](f: A => Find[B]): Find[B] =
    Find[B] { s =>
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
      _ <- Find.modify(c => c.tail.getOrElse(c))
    } yield v
}

object Find {
  def unit[A](a: A): Find[A] = Find(s => (s, a))

  def find(key: String): Find[Option[Value]] = Find(_.find(key))

  def findOrEmpty(key: String): Find[Value] =
    find(key).map(_.getOrElse(Value.of(false)))

  def findOrEmptyPath(path: String): Find[Value] =
    if (path == "." || path.indexOf('.') == -1) findOrEmpty(path)
    else {
      val parts = path.split('.').toList
      parts.map(findOrEmpty).reduce { (f1, f2) =>
        f1.flatMap {
          case v if !v.isEmpty =>
            f2.stacked(v.asContext)
          case v =>
            unit(v)
        }
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
