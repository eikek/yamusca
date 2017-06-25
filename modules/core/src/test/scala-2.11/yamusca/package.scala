package object yamusca {
  implicit class EitherOps[A,B](e: Either[A,B]) {

    def map[C](f: B => C): Either[A,C] =
      e.right.map(f)
  }
}
