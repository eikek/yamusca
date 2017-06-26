package yamusca

import java.io.File
import java.nio.file.Path
import java.net.{URI,URL}
import java.util.UUID
import java.time.{Duration,Instant}
import java.math.{BigDecimal => JavaBigDecimal, BigInteger => JavaBigInteger}

import language.experimental.{macros => smacros}
import yamusca.context._
import yamusca.macros.ValueConverterMacros

trait PrimitiveConverter {
  implicit val stringValueConverter: ValueConverter[String] = Value.fromString _
  implicit val booValueConverter: ValueConverter[Boolean] = Value.fromBoolean _
  implicit val shortConverter: ValueConverter[Short] = ValueConverter.toDefaultString[Short]
  implicit val intValueConverter: ValueConverter[Int] = ValueConverter.toDefaultString[Int]
  implicit val longValueConverter: ValueConverter[Long] = ValueConverter.toDefaultString[Long]

  // default format for floating points
  implicit val doubleConverter: ValueConverter[Double] =
    ValueConverter.toFormatString[Double]("%.02f")

  implicit val floatConverter: ValueConverter[Float] =
    ValueConverter.toFormatString[Float]("%.02f")
}

trait UriFileConverter {
  implicit val jfileConverter: ValueConverter[File] =
    ValueConverter.toDefaultString[File]

  implicit val juriConverter: ValueConverter[URI] =
    ValueConverter.toDefaultString[URI]

  implicit val jurlConverter: ValueConverter[URL] =
    ValueConverter.toDefaultString[URL]

  implicit val jpathConverter: ValueConverter[Path] =
    ValueConverter.toDefaultString[Path]

  implicit val uuidConverter: ValueConverter[UUID] =
    ValueConverter.toDefaultString[UUID]
}

trait JavaTimeConverter {
  implicit val javaDurationConverter: ValueConverter[Duration] =
    ValueConverter.toDefaultString[Duration]

  implicit val javaInstantConverter: ValueConverter[Instant] =
    ValueConverter.toDefaultString[Instant]
}

trait CompoundConverter {
  implicit val contextValueConverter: ValueConverter[Context] =
    ctx => Value.fromContext(ctx, false)

  implicit def seqValueConverter[A](implicit c: ValueConverter[A]): ValueConverter[Seq[A]] =
    seq => Value.fromSeq(seq.map(c))

  implicit def mapValueConverter[A](implicit c: ValueConverter[A]): ValueConverter[Map[String, A]] =
    m => Value.fromContext(Context.from(name => m.get(name).map(c)), m.isEmpty)

  implicit def optionValueConverter[A](implicit c: ValueConverter[A]): ValueConverter[Option[A]] =
    _.map(c).getOrElse(Value.fromContext(Context.empty, true))

  implicit def eitherValueConverter[A,B](implicit
    va: ValueConverter[A],
    vb: ValueConverter[B]): ValueConverter[Either[A,B]] =
    eab => eab.fold(va, vb)
}

trait NumberConverter {
  implicit val bigDecimalConverter: ValueConverter[BigDecimal] =
    ValueConverter.toDefaultString[BigDecimal]

  implicit val bigintConverter: ValueConverter[BigInt] =
    ValueConverter.toDefaultString[BigInt]

  implicit val javaBigDecimalConverter: ValueConverter[JavaBigDecimal] =
    ValueConverter.toDefaultString[JavaBigDecimal]

  implicit val javaBigintConverter: ValueConverter[JavaBigInteger] =
    ValueConverter.toDefaultString[JavaBigInteger]
}

trait CaseClassConverter {
  /** Derive a `ValueConverter` for a case class `A`. */
  def deriveConverter[A]: ValueConverter[A] =
    macro ValueConverterMacros.valueConverterImpl[A]
}

trait converter
    extends PrimitiveConverter
    with UriFileConverter
    with JavaTimeConverter
    with NumberConverter
    with CompoundConverter
    with CaseClassConverter

object converter extends converter
