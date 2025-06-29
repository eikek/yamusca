package yamusca

import java.util.concurrent.atomic.AtomicBoolean

import munit._
import yamusca.data._
import yamusca.parser._
import yamusca.parser.mustache._

class ParserCombSpec extends FunSuite {

  test("standalone ignore whitespace around p if only thing in line") {
    val sp = standaloneOr(consume("x"))
    sp(ParseInput("   x   \n")).left.map(e => fail(e._2)).map { t =>
      assertEquals(t._1.current, "")
      assertEquals(t._2, "x")
    }
  }

  test("work without whitespace") {
    standaloneOr(consume("x"))(ParseInput("x")).left.map(e => fail(e._2)).map { t =>
      assertEquals(t._1.exhausted, true)
      assertEquals(t._2, "x")
    }
  }

  test("parseTag should parse a starting tag") {
    def check(tag: String, expectDelim: Delim, expectName: String): Unit =
      parseTag(ParseInput(tag)) match {
        case Left((_, err))     => fail(err)
        case Right((_, (d, n))) =>
          assertEquals(d, expectDelim)
          assertEquals(n, expectName)
      }
    check("{{a}}", Delim.default, "a")
    check("{{{a}}}", Delim.triple, "a")
    check("{{ ab }}", Delim.default, " ab ")
    check("{{& xy}}", Delim.default, "& xy")
  }

  test("parseVariable should parse variables") {
    def check(in: String, expect: Variable): Unit =
      parseVariable(ParseInput(in)) match {
        case Left((_, err)) => fail(err)
        case Right((_, v))  => assertEquals(v, expect)
      }

    check("{{abc}}", Variable("abc", false))
    check("{{  abc  }}", Variable("abc", false))
    check("{{&abc}}", Variable("abc", true))
    check("{{{abc}}}", Variable("abc", true))
    check("{{& xy }}", Variable("xy", true))
  }

  test("parseComment should parse comments") {
    def check(in: String, expect: String): Unit =
      parseComment(ParseInput(in)) match {
        case Left((_, err)) => fail(err)
        case Right((_, v))  => assertEquals(v.text, expect)
      }

    check("{{! hallo }}", " hallo ")
    check("{{! hello\nworld }}", " hello\nworld ")
  }

  test("parseStartSection should parse starting section tags") {
    def check(in: String, invExpect: Boolean, nameExpect: String): Unit =
      parseStartSection(ParseInput(in)) match {
        case Left((_, err))          => fail(err)
        case Right((_, (inv, name))) =>
          assertEquals(inv, invExpect)
          assertEquals(name, nameExpect)
      }

    check("{{#hello}}", false, "hello")
    check("{{^hello}}", true, "hello")
    check("{{# hello }}", false, "hello")
  }

  test("parseEndSection should parse ending section tags") {
    def check(in: String, nameExpect: String): Unit =
      parseEndSection(ParseInput(in)) match {
        case Left((_, err))   => fail(err)
        case Right((_, name)) =>
          assertEquals(name, nameExpect)
      }

    check("{{/hello}}", "hello")
    check("{{/ hello }}", "hello")
  }

  test("consumeUntilEndSection should consume input until end section") {
    def check(in: String, name: String, expect: String, rest: String): Unit =
      consumeUntilEndSection(name)(ParseInput(in)) match {
        case Left((_, err))      => fail(err)
        case Right((next, pout)) =>
          assertEquals(pout.current, expect)
          assertEquals(next.current, rest)
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

  test("parseLiteral should parse literals") {
    def check(in: String, expect: String, rest: String): Unit =
      parseLiteral(ParseInput(in)) match {
        case Left((_, err))   => fail(err)
        case Right((next, l)) =>
          assertEquals(l.text, expect)
          assertEquals(next.current, rest)
      }

    check("abcde", "abcde", "")
    check("abcde{{#start}}", "abcde", "{{#start}}")
  }

  test("stop before standalone tags") {
    parseLiteral(ParseInput("abced \n  {{#test}}\nbla{{/test}}")).left
      .map(e => fail(e._2))
      .map { t =>
        assertEquals(t._2, Literal("abced \n"))
        assertEquals(t._1.current, "{{#test}}\nbla{{/test}}")
      }

    parseLiteral(ParseInput("{{/boolean}}\n {{#boolean}}").dropLeft(12)).left
      .map(e => fail(e._2))
      .map { t =>
        assertEquals(t._2, Literal("\n"))
        assertEquals(t._1.current, "{{#boolean}}")
      }
  }

  test("recognize non standalone tags") {
    parseLiteral(ParseInput("abc\n  '{{#test}}x{{/test}}")).left
      .map(e => fail(e._2))
      .map { t =>
        assertEquals(t._2, Literal("abc\n  '"))
        assertEquals(t._1.current, "{{#test}}x{{/test}}")
      }

    parseLiteral(ParseInput("{{#test}}x{{/test}}\nxy").dropLeft(19)).left
      .map(e => fail(e._2))
      .map { t =>
        assertEquals(t._2, Literal("\nxy"))
        assertEquals(t._1.current, "")
      }
  }

  test("parseSetDelimiter should change delimiter to input") {
    parseSetDelimiter(ParseInput("{{=<< >>=}}")).left.map(e => fail(e._2)).map { t =>
      assertEquals(t._2, ())
      assertEquals(t._1.delim, Delim("<<", ">>"))
    }
  }

  test("parseSection should parse sections") {
    def check(in: String, expect: Section, rest: String): Unit =
      parseSection(ParseInput(in)).left.map(e => fail(e._2)).map { res =>
        assertEquals(res._2, expect)
        assertEquals(res._1.current, rest)
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

  test("parseElement should parse all elements") {
    def check(in: String, expect: Element): Unit =
      parseElement(ParseInput(in)) match {
        case Left((_, err)) => fail(err)
        case Right((_, l))  => assertEquals(l, expect)
      }

    check("abcd", Literal("abcd"))
    check("{{test}}", Variable("test", false))
    check("{{! comment }}", Comment(" comment "))
    check("{{#test}}help{{/test}}", Section("test", Seq(Literal("help")), false))
  }

  test("fail on wrong input") {
    def check(in: String, msg: String): Unit =
      parseElement(ParseInput(in)) match {
        case Left((_, err)) => assertEquals(err, msg)
        case Right((_, el)) => fail(s"Expected not to succeed: $in => $el")
      }

    check("{{#test}}ab", "Cannot find end section: test")
    check("{{aaa", "Expected string not found: }}")
  }

  test("repeat should call literal parser just once") {
    val flag = new AtomicBoolean(false)
    val p: Parser[Literal] = in =>
      if (flag.compareAndSet(false, true)) parseLiteral(in)
      else sys.error("Called more than once")

    repeat(p)(ParseInput("ab")).left
      .map(ex => fail(ex._2))
      .map(res => assertEquals(res._2, Vector(Literal("ab"))))
  }

  test("call parsers multiple times") {
    val p = consume("a").or(consume("b"))
    repeat(p)(ParseInput("abab")).left
      .map(e => fail(e._2))
      .map(res => assertEquals(res._2, Vector("a", "b", "a", "b")))
  }

  test("parseTemplate should parse literals and variables") {
    parseTemplate(ParseInput("'{{test}}'")).left
      .map(e => fail(e._2))
      .map(t =>
        assertEquals(
          t._2,
          Template(Seq(Literal("'"), Variable("test", false), Literal("'")))
        )
      )
  }

  test("parse long sections") {
    val templateStr = """{{#a}}x
                        |{{b}}yz{{c}}
                        |{{/a}}""".stripMargin
    parseTemplate(ParseInput(templateStr)).left
      .map(e => fail(e._2))
      .map(t =>
        assertEquals(
          t._2,
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

  test("parse multiple sections") {
    parseTemplate(
      ParseInput("{{#bool}}first{{/bool}} {{two}} {{#bool}}third{{/bool}}")
    ).left
      .map(e => fail(e._2))
      .map(t =>
        assertEquals(
          t._2,
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

  test("parse multiple inverted sections") {
    parseTemplate(
      ParseInput("{{^bool}}first{{/bool}} {{two}} {{^bool}}third{{/bool}}")
    ).left
      .map(e => fail(e._2))
      .map(t =>
        assertEquals(
          t._2,
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

  test("recognize custom delimiters") {
    parseTemplate(
      ParseInput("{{=<< >>=}}<<#test>><<name>><</test>><<={{ }}=>>{{help}}")
    ).left.map(e => fail(e._2)).map { t =>
      assertEquals(
        t._2,
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
      assertEquals(
        t._2,
        Template(
          Seq(
            Literal(""),
            Literal("{{hello}}")
          )
        )
      )
    }
  }

  test("not remove whitespace on tags") {
    parseTemplate(
      ParseInput(" {{#boolean}}YES{{/boolean}}\n {{#boolean}}GOOD{{/boolean}}\n")
    ).left.map(e => fail(e._2)).map { t =>
      assertEquals(
        t._2,
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
        assertEquals(
          t._2,
          Template(
            Seq(
              Literal("#"),
              Section("boolean", Seq(Literal("\n/\n")), false)
            )
          )
        )
      }
  }

  test("cut should restrict backtracking") {
    val p: Parser[String] =
      (consume("a").cut ~ consume("1"))
        .map(_ => "a1")
        .or((consume("b") ~ consume("2")).map(_ => "b2"))

    assertEquals(
      p(ParseInput("a3")),
      Left(ParseInput("a3").copy(pos = 1, cutted = 1) -> "Expected '1'")
    )
  }
}
