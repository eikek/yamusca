package yamusca

private[yamusca] object util {

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
