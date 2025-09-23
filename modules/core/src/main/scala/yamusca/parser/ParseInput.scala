package yamusca.parser

final case class ParseInput(
    raw: String,
    pos: Int,
    end: Int,
    delim: Delim,
    cutted: Int = 0
) {
  def current: String =
    if (exhausted) ""
    else raw.substring(pos, end)

  def cut: ParseInput =
    copy(cutted = pos)

  def exhausted: Boolean = pos >= end

  def length: Int =
    if (exhausted) 0
    else end - pos

  def dropLeft(n: Int) =
    if (n < 0) throw new IllegalArgumentException("n must be positive or 0")
    else if (n == 0) this
    else ParseInput(raw, pos + n, end, delim)

  def takeLeft(n: Int) =
    if (n < 0) throw new IllegalArgumentException("n must be positive or 0")
    else if (n >= length) this
    else ParseInput(raw, pos, pos + n, delim)

  def dropRight(n: Int) =
    if (n < 0) throw new IllegalArgumentException("n must be positive or 0")
    else if (n == 0) this
    else ParseInput(raw, pos, end - n, delim)

  def takeRight(n: Int) =
    if (n < 0) throw new IllegalArgumentException("n must be positive or 0")
    else if (n >= length) this
    else ParseInput(raw, end - n, end, delim)

  def expandRight(n: Int) =
    if (n < 0) throw new IllegalArgumentException("n must be positive or 0")
    else if (end + n > raw.length)
      throw new IndexOutOfBoundsException(
        s"Expand right $n (${end + n}) exceeds raw input ${raw.length}"
      )
    else ParseInput(raw, pos, end + n, delim)

  def moveRight(n: Int) =
    if (n > 0 && end + n <= raw.length) expandRight(n)
    else this

  def expandLeft(n: Int) =
    if (n < 0) throw new IllegalArgumentException("n must be positive or 0")
    else if (pos - n < 0)
      throw new IndexOutOfBoundsException(
        s"Expand left $n (${pos - n}) exceeds raw input"
      )
    else ParseInput(raw, pos - n, end, delim)

  def charAt(n: Int): Option[Char] =
    if (n >= length || n < 0) None
    else Some(raw.charAt(pos + n))

  def splitAtNext(s: String): Option[(ParseInput, ParseInput)] =
    raw.indexOf(s, pos) match {
      case -1 => None
      case n  =>
        if (n >= end) None
        else Some(takeLeft(n - pos) -> dropLeft(n - pos))
    }

  def standaloneStart: Option[Int] = {
    @annotation.tailrec
    def go(index: Int): Option[Int] =
      if (index < 0) Some(0)
      else
        raw.charAt(index) match {
          case c if c == ' ' || c == '\t' =>
            go(index - 1)
          case c if c == '\n' =>
            Some(index)
          case _ =>
            None
        }

    go(pos - 1)
  }
}

object ParseInput {
  def apply(in: String): ParseInput = ParseInput(in, 0, in.length, Delim.default)
}
