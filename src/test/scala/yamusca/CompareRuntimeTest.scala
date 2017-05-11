package yamusca

import org.scalatest._
import yamusca.context._
import yamusca.mustache._
import yamusca.mustache.syntax._

class CompareRuntimeTest extends FlatSpec {


  val templ = """
    |Hi {{name}},
    |
    |follow the next steps and find the holy grail:
    |{{#items}}
    |- {{.}}
    |{{/items}}
    |{{^items}}
    |Oops, there is nothing to do.
    |{{/items}}
    |Bye.
    |""".stripMargin


  val data = Context(
    "name" -> "John".value,
    "items" -> Value.list("do thing a".value, "do thing b".value, "do thing c".value)
  )


  def render(c: Context) = {
    val t = parseTemplate(templ).right.get
    t.render(c)
  }

  "run yamusca" should "be not slow, don't need to be soo fast" in {
    val t = parseTemplate(templ).right.get

    for (i <- 1 to 10000) render(data)

    var s: String = null
    val start = System.nanoTime
    for (i <- 1 to 50000) {
      val out = t.render(data)
      s = out
    }
    val duration = System.nanoTime - start
    println(java.time.Duration.ofNanos(duration))
  }

  //todo compare with some other
}
