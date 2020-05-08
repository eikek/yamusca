package yamusca

import io.circe._
import yamusca.imports._
import yamusca.converter.instances.seqValueConverter

package object circe {

  implicit def circeJsonNumberValueConverter(implicit
      vl: ValueConverter[Long],
      vd: ValueConverter[Double]
  ): ValueConverter[JsonNumber] =
    ValueConverter.of(n => n.toLong.map(vl).getOrElse(vd(n.toDouble)))

  implicit def circeJsonObjectValueConverter(implicit
      v: ValueConverter[Json]
  ): ValueConverter[JsonObject] =
    ValueConverter.of(obj =>
      Value.fromContext(Context.from(name => obj(name).map(v)), obj.isEmpty)
    )

  implicit def circeJsonValueConverter(implicit
      vb: ValueConverter[Boolean],
      vnum: ValueConverter[JsonNumber],
      vs: ValueConverter[String]
  ): ValueConverter[Json] =
    ValueConverter.of(
      _.fold(
        Value.fromContext(Context.empty, true),
        b => vb(b),
        n => vnum(n),
        s => vs(s),
        vseq => seqValueConverter(circeJsonValueConverter)(vseq),
        obj => circeJsonObjectValueConverter(circeJsonValueConverter)(obj)
      )
    )
}
