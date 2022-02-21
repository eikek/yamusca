package yamusca

import munit._
import yamusca.context.Find
import yamusca.expand.Expand
import yamusca.imports._
import yamusca.syntax._
import yamusca.util._

class YamuscaSpec extends FunSuite {

  def expectResult(template: String, expected: String, data: Context): Unit = {
    val t = mustache.parse(template) match {
      case Right(x)  => x
      case Left(err) => fail(s"Template parsing failed: $err")
    }
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("stackedcontext should replace correct positions") {
    val sc = Context("name" -> Value.of("red")) :: Context(
      "name" -> Value.of("blue")
    ) :: Context.empty
    val (c2, Some(v)) = sc.find("name") match {
      case (a, Some(b)) => (a, Some(b))
      case _            => sys.error("unexpected")
    }
    assertEquals(v, Value.of("red"))
    assertEquals(c2, sc)
  }

  test("collect missing keys") {
    val t = mustache.parse("{{#a}}hello {{name}}{{/a}}").toOption.get
    val in = Context("a" -> Value.of(true))
    val (missing, ctx, v) = mustache.expandWithMissingKeys(t)(in)
    assertEquals(missing, List("name"))
    assertEquals(ctx, in)
    assertEquals(v, "hello ")
  }

  test("template should render literals") {
    val t = Template(Literal("Hello"), Literal(" "), Literal("world!"))
    assertEquals(t.renderResult(Context.empty), "Hello world!")
  }

  test("render nested same sections") {
    expectResult(
      "{{#a.b}}Hello {{#d.e}}world{{/d.e}}{{/a.b}}",
      "Hello world",
      Context(
        "a" -> Value.map("b" -> Value.fromBoolean(true)),
        "d" -> Value.map("e" -> Value.fromBoolean(true))
      )
    )
    expectResult(
      "{{#a.b}}Hello {{#c}}world{{/c}}{{/a.b}}",
      "Hello world",
      Context("a" -> Value.map("b" -> Value.map("c" -> Value.fromBoolean(true))))
    )
    expectResult(
      "{{#a.b}}Hello {{#a.b.c}}world{{/a.b.c}}{{/a.b}}",
      "Hello world",
      Context("a" -> Value.map("b" -> Value.map("c" -> Value.fromBoolean(true))))
    )
  }

  test("replace variables") {
    val t = Template(Literal("Hello "), Variable("name"), Literal("!"))
    val context = Context("name" -> Value.of("Harry"))
    assertEquals(t.renderResult(context), "Hello Harry!")
  }

  test("replace variables by dotted access") {
    expectResult(
      "{{a.b}}",
      "hello",
      Context("a" -> Value.map("b" -> Value.of("hello")))
    )
  }

  test("render nothing for non-existing vars") {
    val t = Template(Literal("Hello "), Variable("name"), Literal("!"))
    assertEquals(t.renderResult(Context.empty), "Hello !")
  }

  test("not render empty sections") {
    val t = Template(Literal("Hello "), Section("name", Seq(Literal("World!"))))
    assertEquals(t.renderResult(Context.empty), "Hello ")
    assertEquals(t.renderResult(Context("name" -> Value.of(""))), "Hello ")
    assertEquals(t.renderResult(Context("name" -> Value.of(false))), "Hello ")
    assertEquals(t.renderResult(Context("name" -> Value.seq())), "Hello ")
  }

  test("render inverted sections") {
    val t = Template(
      Literal("Hello "),
      Section("name", Seq(Literal("World!")), inverted = true)
    )
    assertEquals(t.renderResult(Context.empty), "Hello World!")
    assertEquals(t.renderResult(Context("name" -> Value.of(""))), "Hello World!")
    assertEquals(t.renderResult(Context("name" -> Value.of("haha"))), "Hello ")
    assertEquals(t.renderResult(Context("name" -> Value.of(false))), "Hello World!")
    assertEquals(t.renderResult(Context("name" -> Value.of(true))), "Hello ")
    assertEquals(t.renderResult(Context("name" -> Value.seq())), "Hello World!")
    assertEquals(t.renderResult(Context("name" -> Value.seq(Value.of("haha")))), "Hello ")
  }

  test("render sections with dotted access") {
    expectResult(
      "{{#a.b}}{{name}}{{/a.b}}",
      "hello",
      Context("a" -> Value.map("b" -> Value.map("name" -> Value.of("hello"))))
    )
  }

  test("render simple lists") {
    val t = Template(Section("colors", Seq(Literal("- "), Variable("."), Literal("\n"))))
    val context =
      Context("colors" -> Value.seq("red".value, "green".value, "yellow".value))
    assertEquals(t.renderResult(context), "- red\n- green\n- yellow\n")
  }

  test("render list of objects") {
    val t =
      Template(Section("colors", Seq(Literal("- "), Variable("name"), Literal("\n"))))
    val context = Context(
      "colors" -> Value.seq(
        Value.map("name" -> "red".value),
        Value.map("name" -> "green".value),
        Value.map("name" -> "yellow".value)
      )
    )
    assertEquals(t.renderResult(context), "- red\n- green\n- yellow\n")
  }

  test("push/pop list element context") {
    val t =
      Template(Section("colors", Seq(Literal("- "), Variable("name"), Literal("\n"))))
    val maps = Value.seq(
      Value.map("name" -> "red".value),
      Value.map(),
      Value.map("name" -> "green".value),
      Value.map(),
      Value.map("name" -> "blue".value),
      Value.map()
    )
    val context = Context("colors" -> maps)
    assertEquals(t.renderResult(context), "- red\n- \n- green\n- \n- blue\n- \n")
  }

  test("render lambda values") {
    val t =
      Template(Section("colors", Seq(Literal("- "), Variable("name"), Literal("\n"))))
    val context = Context(
      "colors" -> Value.lambda(_ => Expand.variableExpand.asString(Variable("name"))),
      "name" -> "Willy".value
    )
    assertEquals(t.renderResult(context), "Willy")
    assertEquals(
      t.renderResult(
        Context("colors" -> Value.lambda(s => Find.unit(s.asString)))
      ),
      "{{#colors}}- {{name}}\n{{/colors}}"
    )
  }

  test("print template strings") {
    assertEquals(
      Template(Literal("Hello"), Literal(" "), Literal("world!")).asString,
      "Hello world!"
    )
    assertEquals(
      Template(Literal("Hello "), Variable("name"), Literal("!")).asString,
      "Hello {{name}}!"
    )
    assertEquals(
      Template(
        Literal("Hello "),
        Section("name", Seq(Literal("World!")), inverted = true)
      ).asString,
      "Hello {{^name}}World!{{/name}}"
    )
    assertEquals(
      Template(
        Section("colors", Seq(Literal("- "), Variable("."), Literal("\n")))
      ).asString,
      "{{#colors}}- {{.}}\n{{/colors}}"
    )
    assertEquals(
      Template(
        Section("colors", Seq(Literal("- "), Variable("name"), Literal("\n")))
      ).asString,
      "{{#colors}}- {{name}}\n{{/colors}}"
    )
  }

  test("thread context through") {
    val ctx0 = new Context {
      def find(key: String): (Context, Option[Value]) =
        (this, if (key == "x1") Some(Value.of("red")) else None)
    }
    val ctx1 = new Context {
      def find(key: String): (Context, Option[Value]) =
        (ctx0, if (key == "x2") Some(Value.of("blue")) else None)
    }
    val t0 = Template(Seq(Variable("x2"), Literal("-"), Variable("x1")))
    assertEquals(t0.renderResult(ctx1), "blue-red")
    assertEquals(t0.renderResult(ctx0), "-red")

    val t1 = Template(Seq(Variable("x1"), Literal("-"), Variable("x2")))
    assertEquals(t1.renderResult(ctx1), "-")
    assertEquals(t1.renderResult(ctx0), "red-")
  }

  test("handle new lines after tags") {
    val data = Context("boolean" -> Value.of(true))
    val template = "#{{#boolean}}\n/\n  {{/boolean}}"
//    info(s"template: ${template.visible}  expected: ${"#\n/".visible}")
    val t = mustache.parse(template).toOption.get
    assertEquals(mustache.render(t)(data).visible, "#\n/\n".visible)
  }

  test("handle new lines before tags") {
    val data = Context("boolean" -> Value.of(true))
    val template = """  {{#boolean}}
#{{/boolean}}
/"""
    // info(s"template: ${template.visible}  expected: ${"#\n/".visible}")
    val t = mustache.parse(template).toOption.get
    assertEquals(mustache.render(t)(data).visible, "#\n/".visible)
  }

  test("remove indented standalone lines") {
    val data = Context("boolean" -> Value.of(true))
    val template = """|
| This Is
  {{#boolean}}
|
  {{/boolean}}
| A Line"""

    val expected = """|
| This Is
|
| A Line"""
    val t = mustache.parse(template).toOption.get
//    info(s"template: ${template.visible}  expected: ${expected.visible}")
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("remove standalone lines") {
    val data = Context("boolean" -> Value.of(true))
    val template = """|
| This Is
{{#boolean}}
|
{{/boolean}}
| A Line"""

    val expected = """|
| This Is
|
| A Line"""
    val t = mustache.parse(template).toOption.get

    // info(s"template: ${template.visible}  expected: ${expected.visible}")
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("permit multiple sections per template") {
    val data = Context("bool" -> Value.of(true), "two" -> Value.of("second"))
    val template = """|
{{#bool}}
* first
{{/bool}}
* {{two}}
{{#bool}}
* third
{{/bool}}"""
    val expected = """|
* first
* second
* third
"""

    val t = mustache.parse(template).toOption.get
    // info(s"template: ${template.visible}  expected: ${expected.visible}")
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("keep surrounding whitespace") {
    val data = Context("boolean" -> Value.of(true))
    val template = " {{#boolean}}YES{{/boolean}}\n {{#boolean}}GOOD{{/boolean}}\n"
    val expected = " YES\n GOOD\n"
    val t = mustache.parse(template).toOption.get
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("access deep nested context") {
    val data = Context(
      "a" -> Value.map("one" -> Value.of("1")),
      "b" -> Value.map("two" -> Value.of("2")),
      "c" -> Value.map("three" -> Value.of("3")),
      "d" -> Value.map("four" -> Value.of("4")),
      "e" -> Value.map("five" -> Value.of("5"))
    )
    val template =
      """
        |{{#a}}
        |{{one}}
        |{{#b}}
        |{{one}}{{two}}{{one}}
        |{{#c}}
        |{{one}}{{two}}{{three}}{{two}}{{one}}
        |{{#d}}
        |{{one}}{{two}}{{three}}{{four}}{{three}}{{two}}{{one}}
        |{{#e}}
        |{{one}}{{two}}{{three}}{{four}}{{five}}{{four}}{{three}}{{two}}{{one}}
        |{{/e}}
        |{{one}}{{two}}{{three}}{{four}}{{three}}{{two}}{{one}}
        |{{/d}}
        |{{one}}{{two}}{{three}}{{two}}{{one}}
        |{{/c}}
        |{{one}}{{two}}{{one}}
        |{{/b}}
        |{{one}}
        |{{/a}}""".stripMargin

    val expected = """
                     |1
                     |121
                     |12321
                     |1234321
                     |123454321
                     |1234321
                     |12321
                     |121
                     |1
                     |""".stripMargin

    val t = mustache.parse(template).toOption.get
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("render falsy sections") {
    val data = Context("boolean" -> Value.of(false))
    val template = "'{{^boolean}}This, rendered.{{/boolean}}'"
    val expected = "'This, rendered.'"
    val t = mustache.parse(template).toOption.get
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("omit truthy sections") {
    val data = Context("boolean" -> Value.of(true))
    val template = "'{{^boolean}}This should not be rendered.{{/boolean}}'"
    val expected = "''"
    val t = mustache.parse(template).toOption.get
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("treat objects and hashes like truthy values ") {
    val data = Context("context" -> Value.map("name" -> Value.of("Joe")))
    val template = "'{{^context}}Hi {{name}}.{{/context}}'"
    val expected = "''"
    val t = mustache.parse(template).toOption.get
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("allow multiple inverted sections") {
    val data = Context("bool" -> Value.of(false), "two" -> Value.of("second"))
    val template = """|
                      |{{^bool}}
                      |* first
                      |{{/bool}}
                      |* {{two}}
                      |{{^bool}}
                      |* third
                      |{{/bool}}""".stripMargin

    val expected = """|
                      |* first
                      |* second
                      |* third
                      |""".stripMargin

    val t = mustache.parse(template).toOption.get
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("iterate over strings") {
    val data =
      Context("list" -> Value.fromSeq(List("a", "b", "c", "d", "e").map(Value.of)))
    val template = "'{{#list}}({{.}}){{/list}}'"
    val expected = "'(a)(b)(c)(d)(e)'"
    val t = mustache.parse(template).toOption.get
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("recognize \\r\\n line endings") {
    val data = Context("boolean" -> Value.of(true))
    val template = "|\r\n{{#boolean}}\r\ntest\r\n{{/boolean}}\r\n|"
    val expected = "|\r\ntest\r\n|"

    val t = mustache.parse(template).toOption.get
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("iterate nested arrays") {
    val data = Context(
      "list" ->
        Value.seq(
          Value.fromSeq(List("1", "2", "3").map(Value.of)),
          Value.fromSeq(List("a", "b", "c").map(Value.of))
        )
    )
    val template = "'{{#list}}({{#.}}{{.}}{{/.}}){{/list}}'"
    val expected = "'(123)(abc)'"
    val t = mustache.parse(template).toOption.get
    assertEquals(mustache.render(t)(data).visible, expected.visible)
  }

  test("comment should be removed") {
    val template = "123{{! this is a comment }}456"
    val expected = "123456"
    val t = mustache.parse(template).toOption.get
    assertEquals(mustache.render(t)(Context.empty), expected)
  }

  test("remove the whole line") {
    val template = """|Begin.
                      |  {{! this is a comment }}
                      |End.""".stripMargin
    val expected = """|Begin.
                      |End.""".stripMargin

    expectResult(template, expected, Context.empty)
  }

  test("not require a preceeding newline") {
    expectResult(
      "   {{! not here }}\n!",
      "!",
      Context.empty
    )
  }

  test("not require a newline to follow") {
    expectResult(
      "!\n  {{! remove me }}",
      "!\n",
      Context.empty
    )
  }

  test("remove multiline comments") {
    expectResult(
      """Begin.
        |{{!
        |  Something is going on here ...
        |}}
        |End.""".stripMargin,
      "Begin.\nEnd.",
      Context.empty
    )
  }

  test("not strip whitespace on inline comments") {
    expectResult(
      "   12  {{! 24 }}\n",
      "   12  \n",
      Context.empty
    )
  }

  test("not remove surrounding whitespace") {
    expectResult(
      "123 {{! removed }} 456",
      "123  456",
      Context.empty
    )
  }

  test("triple mustache should behave like unescape variable") {
    expectResult(
      "unescaped please: {{{forbidden}}}",
      "unescaped please: & \" < >",
      Context("forbidden" -> Value.of("& \" < >"))
    )
  }

  test("interpolate integers normal") {
    expectResult(
      "{{{mph}}} fast",
      "85 fast",
      Context("mph" -> Value.of("85"))
    )
  }

  test("delimiters should change") {
    expectResult(
      "{{=<% %>=}}(<%text%>)",
      "(Hey!)",
      Context("text" -> Value.of("Hey!"))
    )
  }

  test("change to regex special chars") {
    expectResult(
      "({{=[ ]=}}[text])",
      "(It worked!)",
      Context("text" -> Value.of("It worked!"))
    )
  }

  test("persist outside sections") {
    expectResult(
      """|[
         |{{#section}}
         |  {{data}}
         |  |data|
         |{{/section}}
         |
         |{{= | | =}}
         ||#section|
         |  {{data}}
         |  |data|
         ||/section|
         |]""".stripMargin,
      """|[
         |  I got interpolated.
         |  |data|
         |
         |  {{data}}
         |  I got interpolated.
         |]""".stripMargin,
      Context("section" -> Value.of(true), "data" -> Value.of("I got interpolated."))
    )
  }

  test("render special variables -first, -last and -index") {
    expectResult(
      "{{#things}}{{^-first}}, {{/-first}}{{.}}{{/things}}",
      "1, 2, 3",
      Context("things" -> Value.fromSeq(List("1", "2", "3").map(Value.of)))
    )

    expectResult(
      "{{#things}}{{.}}{{^-last}}, {{/-last}}{{/things}}",
      "1, 2, 3",
      Context("things" -> Value.fromSeq(List("1", "2", "3").map(Value.of)))
    )

    expectResult(
      "{{#things}}{{-index}}. {{.}}\n{{/things}}",
      "1. one\n2. two\n3. three\n",
      Context("things" -> Value.fromSeq(List("one", "two", "three").map(Value.of)))
    )
  }
}
