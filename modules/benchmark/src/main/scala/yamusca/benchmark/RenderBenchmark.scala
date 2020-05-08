package yamusca.benchmark

import java.io.{StringReader, StringWriter}
import org.openjdk.jmh.annotations._
import com.github.mustachejava._
import io.circe._, io.circe.parser._
import org.fusesource.scalate.mustache.MustacheParser
import yamusca.imports._
import yamusca.circe._
import yamusca.implicits._

@State(Scope.Thread)
class RenderBenchmark {

  var data: JsonObject = _
  var yt: Template     = _
  var jt: Mustache     = _

  @Setup
  def setup(): Unit = {
    data = parse(dataJson).toOption.get.asObject.get
    yt = mustache.parse(template).toOption.get
    jt = {
      val mf = new DefaultMustacheFactory()
      mf.compile(new StringReader(template), "template")
    }
  }

  def arrToJava(ja: Vector[Json]): java.util.List[Any] = {
    val l = new java.util.ArrayList[Any]
    ja.foreach { j =>
      j.fold[Unit](
        (),
        b => l.add(s"$b"),
        n => l.add(s"${n.toLong.getOrElse(n.toDouble)}"),
        s => l.add(s),
        vs => l.add(arrToJava(vs)),
        obj => l.add(objToJava(obj))
      )
      ()
    }

    l
  }

  def objToJava(jo: JsonObject): java.util.Map[String, Any] = {
    val m = new java.util.HashMap[String, Any]()
    jo.keys.foreach { name =>
      jo(name).foreach {
        _.fold(
          (),
          b => m.put(name, s"$b"),
          n => m.put(name, s"${n.toLong.getOrElse(n.toDouble)}"),
          s => m.put(name, s),
          vs => m.put(name, arrToJava(vs)),
          obj => m.put(name, objToJava(obj))
        )
      }
    }
    m
  }

  def dataJava = {
    val c   = objToJava(data)
    val ctx = new java.util.HashMap[String, Any]()
    ctx.put("tweets", java.util.Arrays.asList(c, c, c))
    ctx
  }

  @Benchmark
  def parseAndRenderYamusca(): Unit = {
    val v = data.asMustacheValue
    mustache.render(mustache.parse(template).toOption.get)(
      Context("tweets" -> Value.seq(v, v, v))
    )
  }

  @Benchmark
  def parseOnlyYamusca(): Unit =
    mustache.parse(template)

  @Benchmark
  def parseAndRenderJava(): Unit = {
    val mf = new DefaultMustacheFactory()
    val t  = mf.compile(new StringReader(template), "template")
    val w  = new StringWriter()
    t.execute(w, dataJava)
  }

  @Benchmark
  def parseOnlyJava(): Unit = {
    val mf = new DefaultMustacheFactory()
    mf.compile(new StringReader(template), "template")
  }

  @Benchmark
  def renderOnlyYamusca(): Unit = {
    val v = data.asMustacheValue
    mustache.render(yt)(Context("tweets" -> Value.seq(v, v, v)))
  }

  @Benchmark
  def renderOnlyJava(): Unit = {
    val w = new StringWriter
    jt.execute(w, dataJava)
  }

  @Benchmark
  def parseScalate(): Unit = {
    val p = new MustacheParser()
    p.parse(template)
  }

}
