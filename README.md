yamusca
=======

<a href="https://travis-ci.org/eikek/yamusca"><img src="https://travis-ci.org/eikek/yamusca.svg"></a>
<a href="https://maven-badges.herokuapp.com/maven-central/com.github.eikek/yamusca-core_2.12"><img src="https://img.shields.io/maven-central/v/com.github.eikek/yamusca-core_2.12.svg"></a>

Yet another mustache parser/renderer for scala.

Goals
-----

-   zero dependencies
-   type safe and functional template data
-   simple and easy to use

### Supported features

-   triple mustache (`{{{`)
-   dotted access (`{{a.b.c}}`)
-   custom delimiters (`{{= … =}}`)

### Not supported

-   Partials

Using
-----

Using [sbt](http://scala-sbt.org):

``` {.scala .rundoc-block rundoc-language="scala" rundoc-exports="both"}
libraryDependencies ++= Seq(
  "com.github.eikek" %% "yamusca-core" % "0.4.0"
)
```

It is available for Scala 2.11 and 2.12.

Simple Example
--------------

``` {.scala .rundoc-block rundoc-language="scala" rundoc-exports="both"}
import yamusca.imports._

val data = Context("name" -> Value.of("Eike"), "items" -> Value.fromSeq( List("one", "two").map(Value.of) ))
//data: yamusca.context.Context = yamusca.context$Context$$anon$2@4c41848e

val templ = mustache.parse("Hello {{name}}, items: {{#items}} - {{.}}, {{/items}}.")
//templ: yamusca.parser.ParseResult = Right(Template(Vector(Literal(Hello ), Variable(name,false), Literal(, items: ), Section(items,Vector(Literal( - ), Variable(.,false), Literal(, )),false), Literal(.))))

mustache.render(templ.right.get)(data)
//res0: String = Hello Eike, items:  - one,  - two, .
```

This is the basic usage, but involves creation of the `Context` value
that is required to fill the template with data.

Another way to create a `Context` is to use the `ValueConverter` type
class. This is a function `A => Value` to convert an `A` into a `Value`
form (which can finally be converted to a `Context`). Adding another
import gets rid of some boilerplate for creating a `Context` object:

``` {.scala .rundoc-block rundoc-language="scala" rundoc-exports="both"}
import yamusca.imports._, yamusca.implicits._

case class Data(name: String, items: List[String])

implicit val dataConv: ValueConverter[Data] = ValueConverter.deriveConverter[Data]
//dataConv: yamusca.imports.ValueConverter[Data] = <function1>

Data("Eike", List("one", "two")).unsafeRender("Hello {{name}}, items: {{#items}} - {{.}}, {{/items}}.")
//res0: String = Hello Eike, items:  - one,  - two, .
```

The `deriveConverter` is a macro that creates a `ValueConverter`
implementation for a case class. It requires that there are
`ValueConverter` in scope for each member type. The import
`yamusca.implicits._` pulls in `ValueConverter` for some standard types
(`String`, `Int`, etc see
[converter.scala](./modules/core/src/main/scala/yamusca/converter.scala))
and it enriches all types that implement `ValueConverter` with three
methods:

-   `asMustacheValue` creates the `Value`
-   `render(t: Template)` renders the given template using the current
    value as `Context` which is derived by calling `asMustacheValue`
-   `unsafeRender(template: String)` same as `render` but parses the
    string first, throwing exceptions on parse errors

Parsing and expanding
---------------------

In order to parse a string into a template, you can use `parse`:

``` {.scala .rundoc-block rundoc-language="scala" rundoc-exports="both"}
import yamusca.imports._
import yamusca.parser.ParseInput

val t: Either[(ParseInput, String), Template] = mustache.parse("hello {{name}}!")
```

which returns a `Either[(ParseInput, String), Template]`. If you parse
constant templates you can use the `mustache` interpolator, which will
throw exceptions on parsing errors:

``` {.scala .rundoc-block rundoc-language="scala" rundoc-exports="both"}
val t: Template = mustache"hello {{name}}!"
```

Once you have a template you can render it by supplying a `Context`
object:

``` {.scala .rundoc-block rundoc-language="scala" rundoc-exports="both"}
import yamusca.imports._
val t = mustache"hello {{name}}!"
val res: String = mustache.render(t)(Context.empty)
//res = "hello !"
```

The `Context` is defined as `String => (Context, Option[Value])`, so it
may return a new `Context` with every value. You can use `expand` to get
the final `Context` that has been threaded through the expansion
process.

``` {.scala .rundoc-block rundoc-language="scala" rundoc-exports="both"}
import yamusca.imports._
val t = mustache"hello {{name}}"
val res: (yamusca.imports.Context, String) = mustache.expand(t)(Context.empty)
// res =  (Context.empty,"hello ")
```

Advanced Example
----------------

The following is an [Ammonite](http://www.lihaoyi.com/Ammonite/) script
showing a (contrived) example:

``` {.scala .rundoc-block rundoc-language="scala" rundoc-exports="both"}
import $ivy.`com.github.eikek::yamusca:0.2.0`
import ammonite.ops._
import java.nio.file.Files
import java.security.MessageDigest
import yamusca.imports._

def computeSha(f: Path): String = {
  println(s"Computing checksum for ${f.name}")
  val md = MessageDigest.getInstance("SHA-256")
  md.update(Files.readAllBytes(f.toNIO))
  md.digest().map(c => "%x".format(c)).mkString
}

case class Data(sha: Option[String], file: Path) extends Context {
  def find(key: String) = key match {
    case "name" => (this, Some(Value.of(file.name)))
    case "size" => (this, Some(Value.of(Files.size(file.toNIO).toString)))
    case "sha" =>
      val checksum = Option(sha.getOrElse(computeSha(file)))
      (copy(sha = checksum), Some(Value.of(checksum)))
    case _ => (this, None)
  }
}

val template1 = mustache.parse(
  """|Name: {{name}}
     |Size: {{size}}""".stripMargin
).right.get
val template2 = mustache.parse(
  """|Name: {{name}}
     |Sha: {{sha}}
     |Sha again: {{sha}}
     |Size: {{size}}""".stripMargin
).right.get


@main
def main(n: Int, f: Path): Unit = {
  n match {
    case 1 =>
      println(mustache.render(template1)(Data(None, f)))
    case 2 =>
      println(mustache.expand(template2)(Data(None, f)))
    case _ =>
      println("Say 1 or 2 please")
  }
}
```

The interesting thing is in `Data` case class which implements the
[Context](./src/main/scala/yamusca/context.scala) trait. The context
passed to the template expansion is not a fixed data structure (like a
`Map`) but a function `String =>
(Context, Option[Value])`. This allows to pass on the updated `Context`
which is threaded through the expansion process. In this example, the
checksum value is cached in the updated context. So the checksum is
computed at most once, or not at all, if the template doesn't need it.

This can be useful if you already have this kind of immutable data
structure, so it is easy to wrap it in the `Context` trait. Using
`mustache.expand` returns the final `Context` value together with the
rendered template; while `mustache.render` discards the final context
and only returns the rendered template.
