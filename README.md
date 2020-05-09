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
-   special variables `{{-first}}`, `{{-last}}` and `{{-index}}`

### Not supported

-   Partials

Using
-----

Using [sbt](http://scala-sbt.org):

``` sbt
libraryDependencies ++= Seq(
  "com.github.eikek" %% "yamusca-core" % "0.6.2"
)
```

It is available for Scala 2.12 and 2.13.

Simple Example
--------------

```scala
import yamusca.imports._

val data = Context("name" -> Value.of("Eike"), "items" -> Value.fromSeq( List("one", "two").map(Value.of) ))
// data: Context = yamusca.context$Context$$anon$2@6d881cea

val templ = mustache.parse("Hello {{name}}, items: {{#items}} - {{.}}{{^-last}}, {{/-last}}{{/items}}.")
// templ: Either[(yamusca.parser.ParseInput, String), Template] = Right(
//   Template(
//     Vector(
//       Literal("Hello "),
//       Variable("name", false),
//       Literal(", items: "),
//       Section(
//         "items",
//         Vector(
//           Literal(" - "),
//           Variable(".", false),
//           Section("-last", Vector(Literal(", ")), true)
//         ),
//         false
//       ),
//       Literal(".")
//     )
//   )
// )

mustache.render(templ.toOption.get)(data)
// res0: String = "Hello Eike, items:  - one,  - two."
```

This is the basic usage, but involves creation of the `Context` value
that is required to fill the template with data.

Another way to create a `Context` is to use the `ValueConverter` type
class. This is a function `A => Value` to convert an `A` into a `Value`
form (which can finally be converted to a `Context`). Adding another
import gets rid of some boilerplate for creating a `Context` object:

```scala
import yamusca.implicits._

case class Data(name: String, items: List[String])

implicit val dataConv: ValueConverter[Data] = ValueConverter.deriveConverter[Data]
// dataConv: ValueConverter[Data] = <function1>

Data("Eike", List("one", "two")).unsafeRender("Hello {{name}}, items: {{#items}} - {{.}}, {{/items}}.")
// res1: String = "Hello Eike, items:  - one,  - two, ."
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

```scala
import yamusca.imports._
import yamusca.parser.ParseInput

val te = mustache.parse("hello {{name}}!")
// te: Either[(ParseInput, String), Template] = Right(
//   Template(Vector(Literal("hello "), Variable("name", false), Literal("!")))
// )
```

which returns a `Either[(ParseInput, String), Template]`. If you parse
constant templates you can use the `mustache` interpolator, which will
throw exceptions on parsing errors:

```scala
val t = mustache"hello {{name}}!"
// t: Template = Template(
//   Vector(Literal("hello "), Variable("name", false), Literal("!"))
// )
```

Once you have a template you can render it by supplying a `Context`
object:

```scala
mustache.render(t)(Context.empty)
// res2: String = "hello !"
```

The `Context` is defined as `String => (Context, Option[Value])`, so it
may return a new `Context` with every value. You can use `expand` to get
the final `Context` that has been threaded through the expansion
process.

```scala
val res = mustache.expand(t)(Context.empty)
// res: (Context, String) = (Context.empty, "hello !")
```

Lazy Context Example
--------------------

The following is an (contrived) example showing a how to allow the
context to load things on demand.

```scala
import java.nio.file.{Files, Path, Paths}
import java.security.MessageDigest

def computeSha(f: Path): String = {
  val md = MessageDigest.getInstance("SHA-256")
  md.update(Files.readAllBytes(f))
  md.digest().map(c => "%x".format(c)).mkString
}

case class FileData(sha: Option[String], file: Path) extends Context {
  def find(key: String) = key match {
    case "name" => (this, Some(Value.of(file.getFileName.toString)))
    case "size" => (this, Some(Value.of(Files.size(file).toString)))
    case "sha" =>
      val checksum = Option(sha.getOrElse(computeSha(file)))
      (copy(sha = checksum), Some(Value.of(checksum)))
    case _ => (this, None)
  }
}

val template1 = mustache.parse(
  """|Name: {{name}}
     |Size: {{size}}""".stripMargin
).toOption.get
// template1: Template = Template(
//   Vector(
//     Literal("Name: "),
//     Variable("name", false),
//     Literal(
//       """
// Size: """
//     ),
//     Variable("size", false)
//   )
// )
val template2 = mustache.parse(
  """|Name: {{name}}
     |Sha: {{sha}}
     |Sha again: {{sha}}
     |Size: {{size}}""".stripMargin
).toOption.get
// template2: Template = Template(
//   Vector(
//     Literal("Name: "),
//     Variable("name", false),
//     Literal(
//       """
// Sha: """
//     ),
//     Variable("sha", false),
//     Literal(
//       """
// Sha again: """
//     ),
//     Variable("sha", false),
//     Literal(
//       """
// Size: """
//     ),
//     Variable("size", false)
//   )
// )


def main(n: Int, f: Path): Unit = {
  n match {
    case 1 =>
      println(mustache.expand(template1)(FileData(None, f)))
    case 2 =>
      println(mustache.expand(template2)(FileData(None, f)))
    case _ =>
      println("Say 1 or 2 please")
  }
}

main(1, Paths.get("build.sbt"))
// (FileData(None,build.sbt),Name: build.sbt
// Size: 4571)
main(2, Paths.get("build.sbt"))
// (FileData(Some(e45c6ddbc9f8866d6460ff5863935e49c8c39c881ab36424535425646856a7),build.sbt),Name: build.sbt
// Sha: e45c6ddbc9f8866d6460ff5863935e49c8c39c881ab36424535425646856a7
// Sha again: e45c6ddbc9f8866d6460ff5863935e49c8c39c881ab36424535425646856a7
// Size: 4571)
```

The `FileData` case class implements the
[Context](./src/main/scala/yamusca/context.scala) trait. The context
passed to the template expansion is not a fixed data structure (like a
`Map`) but a function `String => (Context, Option[Value])`. This
allows to pass on the updated `Context` which is threaded through the
expansion process. In this example, the checksum value is cached in
the updated context. So the checksum is computed at most once, or not
at all, if the template doesn\'t need it.

This can be useful if you already have this kind of immutable data
structure, so it is easy to wrap it in the `Context` trait. Using
`mustache.expand` returns the final `Context` value together with the
rendered template; while `mustache.render` discards the final context
and only returns the rendered template.
