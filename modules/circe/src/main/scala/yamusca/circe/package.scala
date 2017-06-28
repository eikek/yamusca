package yamusca

import io.circe._
import yamusca.context.{Context, Value, ValueConverter}

package object circe {

  implicit def circeJsonNumberValueConverter(implicit
    vl: ValueConverter[Long],
    vd: ValueConverter[Double]): ValueConverter[JsonNumber] =
    n => n.toLong.map(vl).getOrElse(vd(n.toDouble))

  implicit def circeJsonObjectValueConverter(implicit v: ValueConverter[Json]): ValueConverter[JsonObject] =
    obj => Value.fromContext(Context.from(name => obj(name).map(v)), obj.isEmpty)

  implicit def circeJsonValueConverter(implicit
    vb: ValueConverter[Boolean],
    vnum: ValueConverter[JsonNumber],
    vs: ValueConverter[String]): ValueConverter[Json] =
    _.fold(
      Value.fromContext(Context.empty, true),
      b => vb(b),
      n => vnum(n),
      s => vs(s),
      vseq => converter.seqValueConverter(circeJsonValueConverter)(vseq),
      obj => circeJsonObjectValueConverter(circeJsonValueConverter)(obj)
    )
}
