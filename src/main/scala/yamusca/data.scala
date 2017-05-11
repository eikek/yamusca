package yamusca

object data {

  sealed trait Element

  case class Literal(text: String) extends Element {
    def asString(implicit s: Show[Literal]) = s.show(this)
  }

  object Literal {
    implicit val literalShow: Show[Literal] = Show(_.text)
  }

  case class Variable(key: String, unescape: Boolean = false) extends Element {
    def asString(implicit s: Show[Variable]) = s.show(this)
  }

  object Variable {
    implicit val variableShow: Show[Variable] = Show { v =>
      s"""{{${if (v.unescape) "&" else ""}${v.key}}}"""
    }
  }

  case class Section(key: String, inner: Seq[Element], inverted: Boolean = false) extends Element {
    def asString(implicit s: Show[Section]) = s.show(this)
  }

  object Section {
    implicit def sectionShow(implicit s1: Show[Literal], s2: Show[Variable]): Show[Section] = {
      def render(e: Element): String = e match {
        case l: Literal => s1.show(l)
        case v: Variable => s2.show(v)
        case s: Section => sectionShow(s1, s2).show(s)
      }

      Show { s =>
        val prefix = "{{" + (if (s.inverted) "^" else "#")
        prefix + s"${s.key}}}" + (s.inner.map(render).mkString) + s"{{/${s.key}}}"
      }
    }

  }

  case class Template(els: Seq[Element])

  object Template {
    def apply(e: Element, es: Element*): Template = Template(e +: es)

    def asString(t: Template)(implicit s: Show[Template]): String =
      s.show(t)

    implicit def templateShow(implicit s1: Show[Literal], s2: Show[Variable], s3: Show[Section]): Show[Template] = {
      def render(t: Element): String = t match {
        case l: Literal => s1.show(l)
        case v: Variable => s2.show(v)
        case s: Section => s3.show(s)
      }
      Show { t => t.els.map(render).mkString }
    }
  }

  trait Show[A] {
    def show(a: A): String
  }

  object Show {
    def apply[A](f: A => String): Show[A] = new Show[A] {
      def show(a: A) = f(a)
    }
  }
}
