yamusca
=======

<a href="https://travis-ci.org/eikek/yamusca"><img src="https://travis-ci.org/eikek/yamusca.svg"></a>
<a href="https://maven-badges.herokuapp.com/maven-central/com.github.eikek/yamusca_2.12"><img src="https://img.shields.io/maven-central/v/com.github.eikek/yamusca_2.12.svg"></a>

Yet another mustache parser/renderer for scala.

Goals
-----

-   zero dependencies
-   type safe and functional template data
-   simple and easy to use

### Not (yet) supported

-   Partials
-   dotted access (`{{a.b.c}}`)
-   custom delimiters

Using
-----

Using [sbt](http://scala-sbt.org):

``` {.scala .rundoc-block rundoc-language="scala" rundoc-exports="both"}
libraryDependencies ++= Seq(
  "com.github.eikek" %% "yamusca" % "0.2.0"
)
```

It is available for Scala 2.11 and 2.12.

Simple Example
--------------

``` {.scala .rundoc-block rundoc-language="scala" rundoc-exports="both"}
import yamusca.imports._

val data = Context("name" -> Value.of("Eike"), "items" -> Value.fromSeq( List("one", "two").map(Value.of) ))
//data: yamusca.context.Context = yamusca.context$Context$$anon$2@4c41848e

val templ = mustache.parse("Hello {{name}}, see all {{#items}} - {{.}}, {{/items}}.")
//templ: yamusca.parser.ParseResult = Right(Template(Vector(Literal(Hello ), Variable(name,false), Literal(, see all ), Section(items,Vector(Literal( - ), Variable(.,false), Literal(, )),false), Literal(.))))

mustache.render(templ.right.get)(data)
//res0: String = Hello Eike, see all  - one,  - two, .
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
(Context, Value)`. This allows to pass on the updated `Context` which is
threaded through the expansion process. In this example, the checksum
value is cached in the updated context. So the checksum is computed at
most once, or not at all, if the template doesn't need it.

This can be useful if you already have this kind of immutable data
structure, so it is easy to wrap it in the `Context` trait. Using
`mustache.expand` returns the final `Context` value together with the
rendered template; while `mustache.render` discards the final context
and only returns the rendered template.
