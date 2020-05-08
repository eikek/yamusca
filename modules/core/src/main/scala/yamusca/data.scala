package yamusca

object data {

  sealed trait Element

  object Element {
    implicit def elementShow(implicit
        s1: Show[Literal],
        s2: Show[Variable],
        s3: Show[Section],
        s4: Show[Comment]
    ): Show[Element] =
      Show {
        case v: Literal  => s1.show(v)
        case v: Variable => s2.show(v)
        case v: Section  => s3.show(v)
        case v: Comment  => s4.show(v)
      }

    def show(el: Element)(implicit s: Show[Element]): String = s.show(el)
  }

  case class Literal(text: String) extends Element

  object Literal {
    implicit val literalShow: Show[Literal] = Show(_.text)
  }

  case class Comment(text: String) extends Element

  object Comment {
    implicit val commentShow: Show[Comment] = Show(c => s"{{!${c.text}}}")
  }

  case class Variable(key: String, unescape: Boolean = false) extends Element

  object Variable {
    implicit val variableShow: Show[Variable] = Show { v =>
      s"""{{${if (v.unescape) "&" else ""}${v.key}}}"""
    }
  }

  case class Section(key: String, inner: Seq[Element], inverted: Boolean = false)
      extends Element

  object Section {
    implicit def sectionShow(implicit
        s1: Show[Literal],
        s2: Show[Variable],
        s3: Show[Comment]
    ): Show[Section] = {
      def render(e: Element): String =
        e match {
          case l: Literal  => s1.show(l)
          case v: Variable => s2.show(v)
          case c: Comment  => s3.show(c)
          case s: Section  => sectionShow(s1, s2, s3).show(s)
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

    implicit def templateShow(implicit s: Show[Element]): Show[Template] =
      Show(t => t.els.map(s.show).mkString)
  }

  trait Show[A] {
    def show(a: A): String
  }

  object Show {
    def apply[A](f: A => String): Show[A] =
      new Show[A] {
        def show(a: A) = f(a)
      }
  }
}
