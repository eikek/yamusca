package yamusca

import io.circe._
import yamusca.context.{Context, Value, ValueConverter}

package object circe {
  implicit def circeJsonValueConverter(implicit
    vb: ValueConverter[Boolean],
    vl: ValueConverter[Long],
    vd: ValueConverter[Double],
    vs: ValueConverter[String]): ValueConverter[Json] =
    _.fold(
      Value.fromContext(Context.empty, true),
      b => vb(b),
      n => n.toLong.map(vl).getOrElse(vd(n.toDouble)),
      s => vs(s),
      vseq => jsonSeqValue(vseq),
      obj => jsonObjectValue(obj)
    )

  def jsonSeqValue(seq: Seq[Json])(implicit v: ValueConverter[Json]): Value = {
    converter.seqValueConverter(v)(seq)
  }

  def jsonObjectValue(obj: JsonObject)(implicit v: ValueConverter[Json]): Value =
    Value.fromContext(Context.from(name => obj(name).map(v)), obj.isEmpty)
}
