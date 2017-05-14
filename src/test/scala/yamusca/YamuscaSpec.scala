package yamusca

import org.scalatest._
import yamusca.imports._
import yamusca.syntax._
import yamusca.context.Find
import yamusca.expand.Expand
import yamusca.util._

class YamuscaSpec extends FlatSpec with Matchers {

  "stackedcontext" should "replace correct positions" in {
    val sc = Context("name" -> Value.of("red")) :: Context("name" -> Value.of("red")) :: Context.empty
    val (c2, Some(v)) = sc.find("name")
    v should be (Value.of("red"))
    c2 should be (sc)
  }

  "template" should "render literals" in {
    val t = Template(Literal("Hello"), Literal(" "), Literal("world!"))
    t.renderResult(Context.empty) should be ("Hello world!")
  }

  it should "replace variables" in {
    val t = Template(Literal("Hello "), Variable("name"), Literal("!"))
    val context = Context("name" -> Value.of("Harry"))
    t.renderResult(context) should be ("Hello Harry!")
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

  ignore should "iterate nested arrays" in {
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
}
