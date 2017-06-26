package yamusca

/** Collection of all useful imports */
object imports {

  type Literal = data.Literal
  val Literal = data.Literal

  type Variable = data.Variable
  val Variable = data.Variable

  type Section = data.Section
  val Section = data.Section

  type Template = data.Template
  val Template = data.Template

  type Context = context.Context
  val Context = context.Context

  type Value = context.Value
  val Value = context.Value

  type ValueConverter[A] = context.ValueConverter[A]

  object mustache {
    def expand(t: Template): Context => (Context, String) =
      yamusca.expand.render(t)_

    def render(t: Template): Context => String =
      yamusca.expand.renderResult(t)_

    def renderTo(t: Template)(f: String => Unit): Context => Unit =
      yamusca.expand.renderTo(t)(_, f)

    def parse(s: String) =
      parser.parse(s)
  }
}
