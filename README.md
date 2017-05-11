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
import yamusca.context._
import yamusca.mustache._
import yamusca.mustache.syntax._

val templ = parseTemplate("Hello {{name}}, see all {{#items}} - {{.}}, {{/items}}.")
//templ: yamusca.parser.ParseResult = Right(Template(Vector(Literal(Hello ), Variable(name,false), Literal(, see all ), Section(items,Vector(Literal( - ), Variable(.,false), Literal(, )),false), Literal(.))))

val data = Context("name" -> "Eike".value, "items" -> Value.list("one".value, "two".value))
//data: yamusca.context.Context = yamusca.context$Context$$anon$2@42408a8f

templ.right.get.render(data)
//res0: String = Hello Eike, see all  - one,  - two, .
```
