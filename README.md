yamusca
=======

<a href="https://scala-steward.org" title="Scala Steward badge"><img src="https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat-square&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII="></a>
<a href="https://github.com/eikek/yamusca/blob/master/LICENSE.txt" title="License"><img src="https://img.shields.io/github/license/eikek/yamusca.svg?style=flat-square&color=steelblue"></a>
<a href="https://index.scala-lang.org/eikek/yamusca/yamusca-core" title="Scaladex"><img src="https://index.scala-lang.org/eikek/yamusca/yamusca-core/latest.svg?color=blue&style=flat-square"></a>

Yet another mustache parser/renderer for Scala and ScalaJS. It is
published for scala 2.12, 2.13 and 3.


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
  "com.github.eikek" %% "yamusca-core" % "x.y.z",
  "com.github.eikek" %% "yamusca-derive" % "x.y.z" //only if you use `deriveValueConverter`
)
```

It is available for Scala 2.12, 2.13 and 3.

The examples are compiled one after the other such that imports from a
previous one are available to the next.

Simple Example
--------------

```scala
import yamusca.imports._

val data = Context("name" -> Value.of("Eike"), "items" -> Value.fromSeq( List("one", "two").map(Value.of) ))
// data: Context = yamusca.context.Context$$anon$2@69c30ca2

val templ = mustache.parse("Hello {{name}}, items: {{#items}} - {{.}}{{^-last}}, {{/-last}}{{/items}}.")
// templ: Either[(yamusca.parser.ParseInput, String), Template] = Right(
//   value = Template(
//     els = Vector(
//       Literal(text = "Hello "),
//       Variable(key = "name", unescape = false),
//       Literal(text = ", items: "),
//       Section(
//         key = "items",
//         inner = Vector(
//           Literal(text = " - "),
//           Variable(key = ".", unescape = false),
//           Section(
//             key = "-last",
//             inner = Vector(Literal(text = ", ")),
//             inverted = true
//           )
//         ),
//         inverted = false
//       ),
//       Literal(text = ".")
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
import yamusca.derive._

case class Data(name: String, items: List[String])

implicit val dataConv: ValueConverter[Data] = deriveValueConverter[Data]
// dataConv: ValueConverter[Data] = <function1>

Data("Eike", List("one", "two")).unsafeRender("Hello {{name}}, items: {{#items}} - {{.}}, {{/items}}.")
// res1: String = "Hello Eike, items:  - one,  - two, ."
```

The `deriveValueConverter` is a macro that creates a `ValueConverter`
implementation for a case class. It requires that there are
`ValueConverter` in scope for each member type. The import
`yamusca.implicits._` pulls in `ValueConverter` for some standard
types (`String`, `Int`, etc see
[converter.scala](./modules/core/src/main/scala/yamusca/converter.scala))
and it enriches all types that implement `ValueConverter` with three
methods:

- `asMustacheValue` creates the `Value`
- `asContext` as a shortcut for `asMustacheValue.asContext`
- `render(t: Template)` renders the given template using the current
  value as `Context` which is derived by calling `asMustacheValue`
- `unsafeRender(template: String)` same as `render` but parses the
  string first, throwing exceptions on parse errors


Parsing and expanding
---------------------

In order to parse a string into a template, you can use `parse`:

```scala
import yamusca.parser.ParseInput

val te = mustache.parse("hello {{name}}!")
// te: Either[(ParseInput, String), Template] = Right(
//   value = Template(
//     els = Vector(
//       Literal(text = "hello "),
//       Variable(key = "name", unescape = false),
//       Literal(text = "!")
//     )
//   )
// )
```

which returns a `Either[(ParseInput, String), Template]`. If you parse
constant templates you can use the `mustache` interpolator, which will
throw exceptions on parsing errors:

```scala
val t = mustache"hello {{name}}!"
// t: Template = Template(
//   els = Vector(
//     Literal(text = "hello "),
//     Variable(key = "name", unescape = false),
//     Literal(text = "!")
//   )
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
//   els = Vector(
//     Literal(text = "Name: "),
//     Variable(key = "name", unescape = false),
//     Literal(
//       text = """
// Size: """
//     ),
//     Variable(key = "size", unescape = false)
//   )
// )
val template2 = mustache.parse(
  """|Name: {{name}}
     |Sha: {{sha}}
     |Sha again: {{sha}}
     |Size: {{size}}""".stripMargin
).toOption.get
// template2: Template = Template(
//   els = Vector(
//     Literal(text = "Name: "),
//     Variable(key = "name", unescape = false),
//     Literal(
//       text = """
// Sha: """
//     ),
//     Variable(key = "sha", unescape = false),
//     Literal(
//       text = """
// Sha again: """
//     ),
//     Variable(key = "sha", unescape = false),
//     Literal(
//       text = """
// Size: """
//     ),
//     Variable(key = "size", unescape = false)
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
// Size: 5465)
main(2, Paths.get("build.sbt"))
// (FileData(Some(294392842a34aef6c213ea8a96f61ef51da30abb172b277d8e4941792c0c6dc),build.sbt),Name: build.sbt
// Sha: 294392842a34aef6c213ea8a96f61ef51da30abb172b277d8e4941792c0c6dc
// Sha again: 294392842a34aef6c213ea8a96f61ef51da30abb172b277d8e4941792c0c6dc
// Size: 5465)
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


Collect missing keys
--------------------

By default, if the context doesn't contain a value requested by the
template, it renders nothing. In order to know, whether the context
did provide all necessary data for rendering the template, one can
check afterwards if there were failed lookups.

```scala
val templateData = Data("John", List("one", "two"))
// templateData: Data = Data(name = "John", items = List("one", "two"))
mustache.expandWithMissingKeys(
  mustache"Hello {{firstname}} {{name}}, items: {{#items}} - {{.}}, {{/items}}."
)(templateData.asContext)
// res5: (List[String], Context, String) = (
//   List("firstname"),
//   yamusca.context.Context$$anon$3@35ab16ae,
//   "Hello  John, items:  - one,  - two, ."
// )
```

This version of `expand` additionally returns a list of all keys used
in the template, where the context didn't provide any value. It is
empty if all keys could be resolved.

```scala
mustache.expandWithMissingKeys(
  mustache"Hello {{name}}, items: {{#items}} - {{.}}, {{/items}}."
)(templateData.asContext)
// res6: (List[String], Context, String) = (
//   List(),
//   yamusca.context.Context$$anon$3@679257d6,
//   "Hello John, items:  - one,  - two, ."
// )
```
