yamusca
=======

Yet another mustache parser/rendrer for scala.

Features
--------

-   zero dependencies
-   type safe and functional template data
-   simple and easy to use

Building
--------

Using [sbt](http://scala-sbt.org):

``` {.shell .rundoc-block rundoc-language="shell" rundoc-exports="both"}
sbt package
```

Example
-------

``` {.scala .rundoc-block rundoc-language="scala" rundoc-exports="both"}
import yamusca.imports._

val data = Context("name" -> Value.of("Eike"), "items" -> Value.fromSeq( List("one", "two").map(Value.of) ))
//data: yamusca.context.Context = yamusca.context$Context$$anon$2@4c41848e

val templ = mustache.parse("Hello {{name}}, see all {{#items}} - {{.}}, {{/items}}.")
//templ: yamusca.parser.ParseResult = Right(Template(Vector(Literal(Hello ), Variable(name,false), Literal(, see all ), Section(items,Vector(Literal( - ), Variable(.,false), Literal(, )),false), Literal(.))))

mustache.render(templ.right.get)(data)
//res0: String = Hello Eike, see all  - one,  - two, .
```
