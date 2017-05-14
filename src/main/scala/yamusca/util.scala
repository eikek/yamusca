package yamusca

private[yamusca] object util {

  private def firstNewline(s: String): Option[(Int, Int)] = {
    s.indexOf('\n') match {
      case -1 => None
      case n =>
        if (n > 0 && s.charAt(n-1) == '\r') Some((n-1, 2))
        else Some((n, 1))
    }
  }

  private def lastNewline(s: String): Option[(Int, Int)] = {
    s.lastIndexOf('\n') match {
      case -1 => None
      case n =>
        if (n > 0 && s.charAt(n-1) == '\r') Some((n-1, 2))
        else Some((n, 1))
    }
  }

  def removeStartingWS(s: String): String = {
    firstNewline(s) match {
      case None =>
        if (s.trim == "") ""
        else s

      case Some((n, len)) =>
        val sub = s.substring(0, n)
        if (sub.trim == "") s.substring(n+len) //remove newline
        else s
    }
  }

  def removeEndingWS(s: String): String = {
    lastNewline(s) match {
      case None =>
        if (s.trim == "") ""
        else s
      case Some((n, len)) =>
        val sub = s.substring(n)
        if (sub.trim == "") s.substring(0, n+len) //keep newline
        else s
    }
  }

  def isStandalone(prefix: String, suffix: String): Boolean = {
    val sub1 = trimLineLeft(removeStartingWS(suffix))
    val sub2 = trimLineRight(removeEndingWS(prefix).trim)
    sub1 != suffix && sub2 != prefix
  }

  def trimLineLeft(s: String) =
    if (s.startsWith("\n")) s.substring(1) else s

  def trimLineRight(s: String) =
    if (s.endsWith("\n")) s.dropRight(1) else s


  def visibleWs(s: String) = {
    val space = '␣'
    val r = '←'
    val nl = '↓'

    s.replace(' ', space).replace('\n', nl).replace('\r', r)
  }

  implicit class StringWs(s: String) {
    def visible = visibleWs(s)
  }

}
