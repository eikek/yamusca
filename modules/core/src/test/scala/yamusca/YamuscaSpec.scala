package yamusca

import org.scalatest._
import yamusca.imports._
import yamusca.syntax._
import yamusca.context.Find
import yamusca.expand.Expand
import yamusca.util._

class YamuscaSpec extends FlatSpec with Matchers {

  def expectResult(template: String, expected: String, data: Context): Unit = {
    val t = mustache.parse(template) match {
      case Right(x) => x
      case Left(err) => fail(s"Template parsing failed: $err")
    }
    mustache.render(t)(data).visible should be (expected.visible)
  }

  "stackedcontext" should "replace correct positions" in {
    val sc = Context("name" -> Value.of("red")) :: Context("name" -> Value.of("blue")) :: Context.empty
    val (c2, Some(v)) = sc.find("name")
    v should be (Value.of("red"))
    c2 should be (sc)
  }

  "template" should "render literals" in {
    val t = Template(Literal("Hello"), Literal(" "), Literal("world!"))
    t.renderResult(Context.empty) should be ("Hello world!")
  }

  it should "render nested same sections" in {
    expectResult(
      "{{#a.b}}Hello {{#d.e}}world{{/d.e}}{{/a.b}}",
      "Hello world",
      Context("a" -> Value.map("b" -> Value.fromBoolean(true)), "d" -> Value.map("e" -> Value.fromBoolean(true)))
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

  it should "replace variables" in {
    val t = Template(Literal("Hello "), Variable("name"), Literal("!"))
    val context = Context("name" -> Value.of("Harry"))
    t.renderResult(context) should be ("Hello Harry!")
  }

  it should "replace variables by dotted access" in {
    expectResult(
      "{{a.b}}",
      "hello",
      Context("a" -> Value.map("b" -> Value.of("hello")))
    )
  }

  it should "render nothing for non-existing vars" in {
    val t = Template(Literal("Hello "), Variable("name"), Literal("!"))
    t.renderResult(Context.empty) should be ("Hello !")
  }

  it should "not render empty sections" in {
    val t = Template(Literal("Hello "), Section("name", Seq(Literal("World!"))))
    t.renderResult(Context.empty) should be ("Hello ")
    t.renderResult(Context("name" -> Value.of(""))) should be ("Hello ")
    t.renderResult(Context("name" -> Value.of(false))) should be ("Hello ")
    t.renderResult(Context("name" -> Value.seq())) should be ("Hello ")
  }

  it should "render inverted sections" in {
    val t = Template(Literal("Hello "), Section("name", Seq(Literal("World!")), inverted = true))
    t.renderResult(Context.empty) should be ("Hello World!")
    t.renderResult(Context("name" -> Value.of(""))) should be ("Hello World!")
    t.renderResult(Context("name" -> Value.of("haha"))) should be ("Hello ")
    t.renderResult(Context("name" -> Value.of(false))) should be ("Hello World!")
    t.renderResult(Context("name" -> Value.of(true))) should be ("Hello ")
    t.renderResult(Context("name" -> Value.seq())) should be ("Hello World!")
    t.renderResult(Context("name" -> Value.seq(Value.of("haha")))) should be ("Hello ")
  }

  it should "render sections with dotted access" in {
    expectResult(
      "{{#a.b}}{{name}}{{/a.b}}",
      "hello",
      Context("a" -> Value.map("b" -> Value.map("name" -> Value.of("hello"))))
    )
  }

  it should "render simple lists" in {
    val t = Template(Section("colors", Seq(Literal("- "), Variable("."), Literal("\n"))))
    val context = Context("colors" -> Value.seq("red".value, "green".value, "yellow".value))
    t.renderResult(context) should be ("- red\n- green\n- yellow\n")
  }

  it should "render list of objects" in {
    val t = Template(Section("colors", Seq(Literal("- "), Variable("name"), Literal("\n"))))
    val context = Context("colors" -> Value.seq(
      Value.map("name" -> "red".value), Value.map("name" -> "green".value), Value.map("name" -> "yellow".value)))
    t.renderResult(context) should be ("- red\n- green\n- yellow\n")
  }

  it should "push/pop list element context" in {
    val t = Template(Section("colors", Seq(Literal("- "), Variable("name"), Literal("\n"))))
    val maps = Value.seq(
      Value.map("name" -> "red".value),
      Value.map(),
      Value.map("name" -> "green".value),
      Value.map(),
      Value.map("name" -> "blue".value),
      Value.map())
    val context = Context("colors" -> maps)
    t.renderResult(context) should be ("- red\n- \n- green\n- \n- blue\n- \n")
  }

  it should "render lambda values" in {
    val t = Template(Section("colors", Seq(Literal("- "), Variable("name"), Literal("\n"))))
    val context = Context(
      "colors" -> Value.lambda(s =>
        Expand.variableExpand.asString(Variable("name"))
      ),
      "name" -> "Willy".value
    )
    t.renderResult(context) should be ("Willy")
    t.renderResult(Context("colors" -> Value.lambda(s => Find.unit(s.asString)))) should be (
      "{{#colors}}- {{name}}\n{{/colors}}"
    )
  }

  it should "print template strings" in {
    Template(Literal("Hello"), Literal(" "), Literal("world!")).asString should be (
      "Hello world!"
    )
    Template(Literal("Hello "), Variable("name"), Literal("!")).asString should be (
      "Hello {{name}}!"
    )
    Template(Literal("Hello "), Section("name", Seq(Literal("World!")), inverted = true)).asString should be (
      "Hello {{^name}}World!{{/name}}"
    )
    Template(Section("colors", Seq(Literal("- "), Variable("."), Literal("\n")))).asString should be (
      "{{#colors}}- {{.}}\n{{/colors}}"
    )
    Template(Section("colors", Seq(Literal("- "), Variable("name"), Literal("\n")))).asString should be (
      "{{#colors}}- {{name}}\n{{/colors}}"
    )
  }

  it should "thread context through" in {
    val ctx0 = new Context {
      def find(key: String): (Context, Option[Value]) = {
        (this, if (key == "x1") Some(Value.of("red")) else None)
      }
    }
    val ctx1 = new Context {
      def find(key: String): (Context, Option[Value]) = {
        (ctx0, if (key == "x2") Some(Value.of("blue")) else None)
      }
    }
    val t0 = Template(Seq(Variable("x2"), Literal("-"), Variable("x1")))
    t0.renderResult(ctx1) should be ("blue-red")
    t0.renderResult(ctx0) should be ("-red")

    val t1 = Template(Seq(Variable("x1"), Literal("-"), Variable("x2")))
    t1.renderResult(ctx1) should be ("-")
    t1.renderResult(ctx0) should be ("red-")
  }

  it should "handle new lines after tags" in {
    val data = Context("boolean" -> Value.of(true))
    val template = "#{{#boolean}}\n/\n  {{/boolean}}"
    info(s"template: ${template.visible}  expected: ${"#\n/".visible}")
    val t = mustache.parse(template).right.get
    mustache.render(t)(data).visible should be ("#\n/\n".visible)
  }

  it should "handle new lines before tags" in {
    val data = Context("boolean" -> Value.of(true))
    val template = """  {{#boolean}}
#{{/boolean}}
/"""
    info(s"template: ${template.visible}  expected: ${"#\n/".visible}")
    val t = mustache.parse(template).right.get
    mustache.render(t)(data).visible should be ("#\n/".visible)
  }

  it should "remove indented standalone lines" in {
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
    val t = mustache.parse(template).right.get
    info(s"template: ${template.visible}  expected: ${expected.visible}")
    mustache.render(t)(data).visible should be (expected.visible)
  }

  it should "remove standalone lines" in {
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
    val t = mustache.parse(template).right.get
    info(s"template: ${template.visible}  expected: ${expected.visible}")
    mustache.render(t)(data).visible should be (expected.visible)
  }

  it should "permit multiple sections per template" in {
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

    val t = mustache.parse(template).right.get
    info(s"template: ${template.visible}  expected: ${expected.visible}")
    mustache.render(t)(data).visible should be (expected.visible)
  }

  it should "keep surrounding whitespace" in {
    val data = Context("boolean" -> Value.of(true))
    val template = " {{#boolean}}YES{{/boolean}}\n {{#boolean}}GOOD{{/boolean}}\n"
    val expected = " YES\n GOOD\n"
    val t = mustache.parse(template).right.get
    mustache.render(t)(data).visible should be (expected.visible)
  }

  it should "access deep nested context" in {
    val data = Context(
      "a" -> Value.map("one" -> Value.of("1")),
      "b" -> Value.map("two" -> Value.of("2")),
      "c" -> Value.map("three" -> Value.of("3")),
      "d" -> Value.map("four" -> Value.of("4")),
      "e" -> Value.map("five" -> Value.of("5"))
    )
    val template = """
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

    val t = mustache.parse(template).right.get
    mustache.render(t)(data).visible should be (expected.visible)
  }

  it should "render falsy sections" in {
    val data = Context("boolean" -> Value.of(false))
    val template = "'{{^boolean}}This should be rendered.{{/boolean}}'"
    val expected = "'This should be rendered.'"
    val t = mustache.parse(template).right.get
    mustache.render(t)(data).visible should be (expected.visible)
  }

  it should "omit truthy sections" in {
    val data = Context("boolean" -> Value.of(true))
    val template = "'{{^boolean}}This should not be rendered.{{/boolean}}'"
    val expected = "''"
    val t = mustache.parse(template).right.get
    mustache.render(t)(data).visible should be (expected.visible)
  }

  it should "treat objects and hashes like truthy values " in {
    val data = Context("context" -> Value.map("name" -> Value.of("Joe")))
    val template = "'{{^context}}Hi {{name}}.{{/context}}'"
    val expected = "''"
    val t = mustache.parse(template).right.get
    mustache.render(t)(data).visible should be (expected.visible)
  }

  it should "allow multiple inverted sections" in {
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

    val t = mustache.parse(template).right.get
    mustache.render(t)(data).visible should be (expected.visible)
  }

  it should "iterate over strings" in {
    val data = Context("list" -> Value.fromSeq(List("a","b","c","d","e").map(Value.of)))
    val template =  "'{{#list}}({{.}}){{/list}}'"
    val expected = "'(a)(b)(c)(d)(e)'"
    val t = mustache.parse(template).right.get
    mustache.render(t)(data).visible should be (expected.visible)
  }

  it should "recognize \\r\\n line endings" in {
    val data = Context("boolean" -> Value.of(true))
    val template = "|\r\n{{#boolean}}\r\ntest\r\n{{/boolean}}\r\n|"
    val expected =  "|\r\ntest\r\n|"

    val t = mustache.parse(template).right.get
    mustache.render(t)(data).visible should be (expected.visible)
  }

  it should "iterate nested arrays" in {
    val data = Context("list" ->
      Value.seq(
        Value.fromSeq(List("1","2","3").map(Value.of)),
        Value.fromSeq(List("a","b","c").map(Value.of))
      ))
    val template = "'{{#list}}({{#.}}{{.}}{{/.}}){{/list}}'"
    val expected = "'(123)(abc)'"
    val t = mustache.parse(template).right.get
    mustache.render(t)(data).visible should be (expected.visible)
  }

  "comment" should "be removed" in {
    val template = "123{{! this is a comment }}456"
    val expected = "123456"
    val t = mustache.parse(template).right.get
    mustache.render(t)(Context.empty) should be (expected)
  }

  it should "remove the whole line" in {
    val template = """|Begin.
      |  {{! this is a comment }}
      |End.""".stripMargin
    val expected = """|Begin.
      |End.""".stripMargin

    expectResult(template, expected, Context.empty)
  }

  it should "not require a preceeding newline" in {
    expectResult(
      "   {{! not here }}\n!",
      "!",
      Context.empty
    )
  }

  it should "not require a newline to follow" in {
    expectResult(
      "!\n  {{! remove me }}",
      "!\n",
      Context.empty
    )
  }

  it should "remove multiline comments" in {
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

  it should "not strip whitespace on inline comments" in {
    expectResult(
      "   12  {{! 24 }}\n",
      "   12  \n",
      Context.empty
    )
  }

  it should "not remove surrounding whitespace" in {
    expectResult(
      "123 {{! removed }} 456",
      "123  456",
      Context.empty
    )
  }

  "triple mustache" should "behave like unescape variable" in {
    expectResult(
      "unescaped please: {{{forbidden}}}",
      "unescaped please: & \" < >",
      Context("forbidden" -> Value.of("& \" < >"))
    )
  }

  it should "interpolate integers normal" in {
    expectResult(
      "{{{mph}}} fast",
      "85 fast",
      Context("mph" -> Value.of("85"))
    )
  }

  "delimiters" should "change" in {
    expectResult(
      "{{=<% %>=}}(<%text%>)",
      "(Hey!)",
      Context("text" -> Value.of("Hey!"))
    )
  }

  it should "change to regex special chars" in {
    expectResult(
      "({{=[ ]=}}[text])",
      "(It worked!)",
      Context("text" -> Value.of("It worked!"))
    )
  }

  it should "persist outside sections" in {
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

  it should "render special variables -first, -last and -index" in {
    expectResult(
      "{{#things}}{{^-first}}, {{/-first}}{{.}}{{/things}}",
      "1, 2, 3",
      Context("things" -> Value.fromSeq(List("1","2","3").map(Value.of))))

    expectResult(
      "{{#things}}{{.}}{{^-last}}, {{/-last}}{{/things}}",
      "1, 2, 3",
      Context("things" -> Value.fromSeq(List("1","2","3").map(Value.of))))

    expectResult(
      "{{#things}}{{-index}}. {{.}}\n{{/things}}",
      "1. one\n2. two\n3. three\n",
      Context("things" -> Value.fromSeq(List("one","two","three").map(Value.of))))
  }
}
