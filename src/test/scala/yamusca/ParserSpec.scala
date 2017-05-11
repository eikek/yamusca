package yamusca

import org.scalatest._
import yamusca.data._
import yamusca.parser._

class ParserSpec extends FlatSpec with Matchers {

  "parser" should "parse literal text" in {
    val Right(text0) = parse("hello world!")
    text0 should be (Template(Literal("hello world!")))

    val Right(text1) = parse("hello {{name}}!")
    text1 should be (Template(Literal("hello "), Variable("name", false), Literal("!")))
  }

  it should "parse variables" in {
    val Right(var0) = parse("{{name}}")
    var0 should be (Template(Variable("name")))

    val Right(var1) = parse("{{ name}}")
    var1 should be (Template(Variable("name")))

    val Right(var2) = parse("{{& name}}")
    var2 should be (Template(Variable("name", true)))
  }

  it should "parse sections" in {
    val inputs = List(
      "{{#colors}}- {{name}}\n{{/colors}}",
      "{{#colors}}- {{.}}\n{{/colors}}"
    )
    for (in <- inputs) {
      val Right(t) = parse(in)
      t.els(0) shouldBe a [Section]
      Template.asString(t) should be (in)
    }
  }
}
