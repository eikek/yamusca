package yamusca

import yamusca.converter.ValueConverter
import yamusca.macros.ValueConverterMacros

package object derive {
  import scala.language.experimental.macros

  /** Derive a `ValueConverter` for a case class `A`. */
  final def deriveValueConverter[A]: ValueConverter[A] =
    macro ValueConverterMacros.valueConverterImpl[A]
}
