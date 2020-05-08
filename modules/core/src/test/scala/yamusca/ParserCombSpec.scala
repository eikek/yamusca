package yamusca

import java.util.concurrent.atomic.AtomicBoolean
import yamusca.data._
import yamusca.parser._
import yamusca.parser.mustache._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ParserCombSpec extends AnyFlatSpec with Matchers {

  "standalone" should "ignore whitespace around p if only thing in line" in {
    val sp = standaloneOr(consume("x"))
    sp(ParseInput("   x   \n")).left.map(e => fail(e._2)).map { t =>
      t._1.current should be("")
      t._2 should be("x")
    }
  }

  it should "work without whitespace" in {
    standaloneOr(consume("x"))(ParseInput("x")).left.map(e => fail(e._2)).map { t =>
      t._1.exhausted should be(true)
      t._2 should be("x")
    }
  }

  "parseTag" should "parse a starting tag" in {
    def check(tag: String, expectDelim: Delim, expectName: String): Unit =
      parseTag(ParseInput(tag)) match {
        case Left((_, err)) => fail(err)
        case Right((_, (d, n))) =>
          d should be(expectDelim)
          n should be(expectName)
      }
    check("{{a}}", Delim.default, "a")
    check("{{{a}}}", Delim.triple, "a")
    check("{{ ab }}", Delim.default, " ab ")
    check("{{& xy}}", Delim.default, "& xy")
  }

  "parseVariable" should "parse variables" in {
    def check(in: String, expect: Variable): Unit =
      parseVariable(ParseInput(in)) match {
        case Left((_, err)) => fail(err)
        case Right((_, v))  => v should be(expect)
      }

    check("{{abc}}", Variable("abc", false))
    check("{{  abc  }}", Variable("abc", false))
    check("{{&abc}}", Variable("abc", true))
    check("{{{abc}}}", Variable("abc", true))
    check("{{& xy }}", Variable("xy", true))
  }

  "parseComment" should "parse comments" in {
    def check(in: String, expect: String): Unit =
      parseComment(ParseInput(in)) match {
        case Left((_, err)) => fail(err)
        case Right((_, v))  => v.text should be(expect)
      }

    check("{{! hallo }}", " hallo ")
    check("{{! hello\nworld }}", " hello\nworld ")
  }

  "parseStartSection" should "parse starting section tags" in {
    def check(in: String, invExpect: Boolean, nameExpect: String): Unit =
      parseStartSection(ParseInput(in)) match {
        case Left((_, err)) => fail(err)
        case Right((_, (inv, name))) =>
          inv should be(invExpect)
          name should be(nameExpect)
      }

    check("{{#hello}}", false, "hello")
    check("{{^hello}}", true, "hello")
    check("{{# hello }}", false, "hello")
  }

  "parseEndSection" should "parse ending section tags" in {
    def check(in: String, nameExpect: String): Unit =
      parseEndSection(ParseInput(in)) match {
        case Left((_, err)) => fail(err)
        case Right((_, name)) =>
          name should be(nameExpect)
      }

    check("{{/hello}}", "hello")
    check("{{/ hello }}", "hello")
  }

  "consumeUntilEndSection" should "consume input until end section" in {
    def check(in: String, name: String, expect: String, rest: String): Unit =
      consumeUntilEndSection(name)(ParseInput(in)) match {
        case Left((_, err)) => fail(err)
        case Right((next, pout)) =>
          pout.current should be(expect)
          next.current should be(rest)
      }

    check("abcde{{/hello}}", "hello", "abcde", "")
    check("abc {{/ hello }}", "hello", "abc ", "")
    check(
      "abcd{{#blup}}x{{/blup}}yz{{/hello}}blabla",
      "hello",
      "abcd{{#blup}}x{{/blup}}yz",
      "blabla"
    )
  }

  "parseLiteral" should "parse literals" in {
    def check(in: String, expect: String, rest: String): Unit =
      parseLiteral(ParseInput(in)) match {
        case Left((_, err)) => fail(err)
        case Right((next, l)) =>
          l.text should be(expect)
          next.current should be(rest)
      }

    check("abcde", "abcde", "")
    check("abcde{{#start}}", "abcde", "{{#start}}")
  }

  it should "stop before standalone tags" in {
    parseLiteral(ParseInput("abced \n  {{#test}}\nbla{{/test}}")).left
      .map(e => fail(e._2))
      .map { t =>
        t._2 should be(Literal("abced \n"))
        t._1.current should be("{{#test}}\nbla{{/test}}")
      }

    parseLiteral(ParseInput("{{/boolean}}\n {{#boolean}}").dropLeft(12)).left
      .map(e => fail(e._2))
      .map { t =>
        t._2 should be(Literal("\n"))
        t._1.current should be("{{#boolean}}")
      }
  }

  it should "recognize non standalone tags" in {
    parseLiteral(ParseInput("abc\n  '{{#test}}x{{/test}}")).left
      .map(e => fail(e._2))
      .map { t =>
        t._2 should be(Literal("abc\n  '"))
        t._1.current should be("{{#test}}x{{/test}}")
      }

    parseLiteral(ParseInput("{{#test}}x{{/test}}\nxy").dropLeft(19)).left
      .map(e => fail(e._2))
      .map { t =>
        t._2 should be(Literal("\nxy"))
        t._1.current should be("")
      }
  }

  "parseSetDelimiter" should "change delimiter to input" in {
    parseSetDelimiter(ParseInput("{{=<< >>=}}")).left.map(e => fail(e._2)).map { t =>
      t._2 should be(())
      t._1.delim should be(Delim("<<", ">>"))
    }
  }

  "parseSection" should "parse sections" in {
    def check(in: String, expect: Section, rest: String): Unit =
      parseSection(ParseInput(in)).left.map(e => fail(e._2)).map { res =>
        res._2 should be(expect)
        res._1.current should be(rest)
      }

    check("{{#test}}hallo{{/test}}", Section("test", Seq(Literal("hallo")), false), "")
    check(
      "  {{#test}}\n hallo{{/test}}",
      Section("test", Seq(Literal(" hallo")), false),
      ""
    )
    check("{{^test}}hallo{{/test}}", Section("test", Seq(Literal("hallo")), true), "")
    check(
      "{{#test}}hallo{{/test}} bla bla",
      Section("test", Seq(Literal("hallo")), false),
      " bla bla"
    )
    check(
      "{{^test}}hallo{{/test}} bla bla",
      Section("test", Seq(Literal("hallo")), true),
      " bla bla"
    )
    check(
      "{{#test}}hallo {{name}}{{/test}}",
      Section("test", Seq(Literal("hallo "), Variable("name", false)), false),
      ""
    )
    check("{{#a}}\n{{/a}}r", Section("a", Seq.empty), "r")
  }

  "parseElement" should "parse all elements" in {
    def check(in: String, expect: Element): Unit =
      parseElement(ParseInput(in)) match {
        case Left((_, err)) => fail(err)
        case Right((_, l))  => l should be(expect)
      }

    check("abcd", Literal("abcd"))
    check("{{test}}", Variable("test", false))
    check("{{! comment }}", Comment(" comment "))
    check("{{#test}}help{{/test}}", Section("test", Seq(Literal("help")), false))
  }

  it should "fail on wrong input" in {
    def check(in: String, msg: String): Unit =
      parseElement(ParseInput(in)) match {
        case Left((_, err)) => err should be(msg)
        case Right((_, el)) => fail(s"Expected not to succeed: $in => $el")
      }

    check("{{#test}}ab", "Cannot find end section: test")
    check("{{aaa", "Expected string not found: }}")
  }

  "repeat" should "call literal parser just once" in {
    val flag = new AtomicBoolean(false)
    val p: Parser[Literal] = in => {
      if (flag.compareAndSet(false, true)) parseLiteral(in)
      else sys.error("Called more than once")
    }

    repeat(p)(ParseInput("ab")).left
      .map(ex => fail(ex._2))
      .map(res => res._2 should be(Vector(Literal("ab"))))
  }

  it should "call parsers multiple times" in {
    val p = consume("a").or(consume("b"))
    repeat(p)(ParseInput("abab")).left
      .map(e => fail(e._2))
      .map(res => res._2 should be(Vector("a", "b", "a", "b")))
  }

  "parseTemplate" should "parse literals and variables" in {
    parseTemplate(ParseInput("'{{test}}'")).left
      .map(e => fail(e._2))
      .map(t =>
        t._2 should be(Template(Seq(Literal("'"), Variable("test", false), Literal("'"))))
      )
  }

  it should "parse long sections" in {
    val templateStr = """{{#a}}x
                        |{{b}}yz{{c}}
                        |{{/a}}""".stripMargin
    parseTemplate(ParseInput(templateStr)).left
      .map(e => fail(e._2))
      .map(t =>
        t._2 should be(
          Template(
            Seq(
              Section(
                "a",
                Seq(
                  Literal("x\n"),
                  Variable("b", false),
                  Literal("yz"),
                  Variable("c", false),
                  Literal("\n")
                )
              )
            )
          )
        )
      )
  }

  it should "parse multiple sections" in {
    parseTemplate(
      ParseInput("{{#bool}}first{{/bool}} {{two}} {{#bool}}third{{/bool}}")
    ).left
      .map(e => fail(e._2))
      .map(t =>
        t._2 should be(
          Template(
            Seq(
              Section("bool", Seq(Literal("first")), false),
              Literal(" "),
              Variable("two", false),
              Literal(" "),
              Section("bool", Seq(Literal("third")), false)
            )
          )
        )
      )
  }

  it should "parse multiple inverted sections" in {
    parseTemplate(
      ParseInput("{{^bool}}first{{/bool}} {{two}} {{^bool}}third{{/bool}}")
    ).left
      .map(e => fail(e._2))
      .map(t =>
        t._2 should be(
          Template(
            Seq(
              Section("bool", Seq(Literal("first")), true),
              Literal(" "),
              Variable("two", false),
              Literal(" "),
              Section("bool", Seq(Literal("third")), true)
            )
          )
        )
      )
  }

  it should "recognize custom delimiters" in {
    parseTemplate(
      ParseInput("{{=<< >>=}}<<#test>><<name>><</test>><<={{ }}=>>{{help}}")
    ).left.map(e => fail(e._2)).map { t =>
      t._2 should be(
        Template(
          Seq(
            Literal(""),
            Section("test", Seq(Variable("name", false)), false),
            Literal(""),
            Variable("help", false)
          )
        )
      )
    }
    parseTemplate(ParseInput("{{=<< >>=}}{{hello}}")).left.map(e => fail(e._2)).map { t =>
      t._2 should be(
        Template(
          Seq(
            Literal(""),
            Literal("{{hello}}")
          )
        )
      )
    }
  }

  it should "not remove whitespace on tags" in {
    parseTemplate(
      ParseInput(" {{#boolean}}YES{{/boolean}}\n {{#boolean}}GOOD{{/boolean}}\n")
    ).left.map(e => fail(e._2)).map { t =>
      t._2 should be(
        Template(
          Seq(
            Literal(" "),
            Section("boolean", Seq(Literal("YES")), false),
            Literal("\n "),
            Section("boolean", Seq(Literal("GOOD")), false),
            Literal("\n")
          )
        )
      )
    }

    parseTemplate(ParseInput("#{{#boolean}}\n/\n  {{/boolean}}")).left
      .map(e => fail(e._2))
      .map { t =>
        t._2 should be(
          Template(
            Seq(
              Literal("#"),
              Section("boolean", Seq(Literal("\n/\n")), false)
            )
          )
        )
      }
  }

  "cut" should "restrict backtracking" in {
    val p: Parser[String] =
      (consume("a").cut ~ consume("1"))
        .map(_ => "a1")
        .or((consume("b") ~ consume("2")).map(_ => "b2"))

    p(ParseInput("a3")) should be(
      Left((ParseInput("a3").copy(pos = 1, cutted = 1) -> "Expected '1'"))
    )
  }
}
