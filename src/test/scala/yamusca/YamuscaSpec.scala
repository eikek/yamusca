package yamusca

import org.scalatest._
import yamusca.imports._
import yamusca.syntax._
import yamusca.context.Find
import yamusca.expand.Expand

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
}
