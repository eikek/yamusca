package yamusca.benchmark

import java.io.{StringReader, StringWriter}
import org.openjdk.jmh.annotations._
import com.github.mustachejava._
import io.circe._, io.circe.parser._
import yamusca.imports._

@State(Scope.Thread)
class RenderBenchmark {

  val data = parse(dataJson).right.get.asObject.get


  def arrToJava(ja: Vector[Json]): java.util.List[Any] = {
    val l = new java.util.ArrayList[Any]
    ja.foreach(j => {
      j.fold[Unit](
        (),
        b => l.add(b+""),
        n => l.add(n.toLong.getOrElse(n.toDouble)+""),
        s => l.add(s),
        vs => l.add(arrToJava(vs)),
        obj => l.add(objToJava(obj)))
      ()
    })

    l
  }

  def objToJava(jo: JsonObject): java.util.Map[String, Any] = {
    val m = new java.util.HashMap[String, Any]()
    jo.fields.foreach { name =>
      jo(name).foreach { _.fold(
        (),
        b => m.put(name, b+ ""),
        n => m.put(name, n.toLong.getOrElse(n.toDouble)+ ""),
        s => m.put(name, s),
        vs => m.put(name, arrToJava(vs)),
        obj => m.put(name, objToJava(obj)))
      }
    }

    m
  }

  def dataJava = {
    val c = objToJava(data)
    val ctx = new java.util.HashMap[String, Any]()
    ctx.put("tweets", java.util.Arrays.asList(c, c, c))
    ctx
  }

  case class JsonData(json: JsonObject) extends Context {
    def find(key: String): (Context, Option[Value]) = {
      json(key) match {
        case Some(js) => (this, Some(jsonToValue(js)))
        case None => (this, None)
      }
    }

    def jsonToValue(js: Json): Value = js.fold(
      Value.of(false),
      b => Value.of(b),
      n => Value.of(n.toLong.getOrElse(n.toDouble)+ ""),
      s => Value.of(s),
      vs => Value.fromSeq(vs.map(jsonToValue)),
      obj => Value.fromContext(JsonData(obj), obj.isEmpty)
    )
  }

  @Benchmark
  def parseAndRenderYamusca(): Unit = {
    val v = Value.fromContext(JsonData(data), data.isEmpty)
    mustache.render(mustache.parse(template).right.get)(Context("tweets" -> Value.seq(v, v, v)))
  }


  @Benchmark
  def parseAndRenderJava(): Unit = {
    val mf = new DefaultMustacheFactory()
    val t = mf.compile(new StringReader(template), "template")
    val w = new StringWriter()
    t.execute(w, dataJava)
  }

  val yt = mustache.parse(template).right.get

  @Benchmark
  def renderOnlyYamusca(): Unit = {
    val v = Value.fromContext(JsonData(data), data.isEmpty)
    mustache.render(yt)(Context("tweets" -> Value.seq(v,v,v)))
  }

  val jt = {
    val mf = new DefaultMustacheFactory()
    mf.compile(new StringReader(template), "template")
  }

  @Benchmark
  def renderOnlyJava(): Unit = {
    val w = new StringWriter
    jt.execute(w, dataJava)
  }

}
