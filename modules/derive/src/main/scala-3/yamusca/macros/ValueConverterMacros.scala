package yamusca.macros

import scala.deriving.*
import scala.compiletime.*
import scala.quoted.{Expr, Quotes, Type}
import yamusca.converter.ValueConverter
import yamusca.context.*

// Thanks to:
// - https://blog.philipp-martini.de/blog/magic-mirror-scala3/
// - https://docs.scala-lang.org/scala3/reference/contextual/derivation.html
object ValueConverterMacros:
  private def newConverter[A](
      elems: List[ValueConverter[?]],
      names: Seq[String]
  ): ValueConverter[A] =
    new ValueConverter[A]:
      def apply(a: A): Value =
        val t = a.asInstanceOf[Product].productIterator.toList
        Value.fromContext(
          Context.from { key =>
            names.indexOf(key) match {
              case n if n >= 0 =>
                val tc = elems(n).asInstanceOf[ValueConverter[Any]]
                val el = t(n)
                Some(tc.apply(el))
              case _ => None
            }
          },
          false
        )

  final inline def deriveConverter[A <: Product](using inline A: Mirror.ProductOf[A]) =
    deriveViaMirror[A]

  inline given deriveViaMirror[A <: Product](using
      m: Mirror.ProductOf[A]
  ): ValueConverter[A] =
    val elems = summonAll[m.MirroredElemTypes]
    val names = getElemLabels[m.MirroredElemLabels]
    newConverter(elems, names)

  inline def summonAll[T <: Tuple]: List[ValueConverter[_]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => summonInline[ValueConverter[t]] :: summonAll[ts]

  inline def getElemLabels[A <: Tuple]: List[String] =
    inline erasedValue[A] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  => constValue[t].toString :: getElemLabels[ts]
    }
