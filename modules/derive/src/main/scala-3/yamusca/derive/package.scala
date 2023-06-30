package yamusca

import scala.deriving.Mirror
import yamusca.converter.ValueConverter
import yamusca.macros.ValueConverterMacros

package object derive {

  /** Derive a `ValueConverter` for a case class `A`. */
  final inline def deriveValueConverter[A <: Product](using
      A: Mirror.ProductOf[A]
  ): ValueConverter[A] =
    ValueConverterMacros.deriveConverter[A]
}
