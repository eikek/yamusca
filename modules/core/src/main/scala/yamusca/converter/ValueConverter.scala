package yamusca.converter

import java.util.Locale

import language.experimental.{macros => smacros}
import yamusca.context._
import yamusca.macros.ValueConverterMacros

@annotation.implicitNotFound("There is no ValueConverter for type `${A}' in scope.")
trait ValueConverter[A] extends (A => Value)

object ValueConverter {

  def apply[A](implicit vc: ValueConverter[A]): ValueConverter[A] = vc

  def of[A](f: A => Value): ValueConverter[A] =
    new ValueConverter[A] {
      def apply(a: A) = f(a)
    }

  /** A `ValueConverter` that calls `toString` on the input value. */
  def toDefaultString[A]: ValueConverter[A] =
    of(a => Value.fromString(a.toString))

  /** A `ValueConverter` that calls `fmt.format(a)` using given locale. */
  def toFormatString[A](locale: Locale, fmt: String): ValueConverter[A] =
    of(a => Value.fromString(fmt.formatLocal(locale, a)))

  /** A `ValueConverter` that calls `fmt.format(a)` using `Locale.ROOT`. */
  def toFormatString[A](fmt: String): ValueConverter[A] =
    toFormatString(Locale.ROOT, fmt)

  /** Derive a `ValueConverter` for a case class `A`. */
  def deriveConverter[A]: ValueConverter[A] =
    macro ValueConverterMacros.valueConverterImpl[A]

}
