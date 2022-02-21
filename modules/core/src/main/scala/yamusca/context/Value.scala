package yamusca.context

import yamusca.context.Value.MapValue
import yamusca.data.Section

sealed trait Value {
  def isEmpty: Boolean
  def asContext: Context =
    this match {
      case MapValue(v, _) => v
      case _              => Context.from(key => if (key == ".") Some(this) else None)
    }
}
object Value {
  def fromString(s: String): Value = SimpleValue(s)
  def fromBoolean(b: Boolean): Value = BoolValue(b)
  def fromSeq(vs: Seq[Value]): Value = ListValue(vs)
  def fromContext(ctx: Context, empty: Boolean): Value = MapValue(ctx, empty)
  def fromMap(m: Map[String, Value]) = fromContext(Context.fromMap(m), m.isEmpty)

  def of(s: String): Value = SimpleValue(s)
  def of(s: Option[String]): Value = SimpleValue(s.getOrElse(""))
  def of(b: Boolean): Value = BoolValue(b)
  def seq(vs: Value*): Value = ListValue(vs)
  def map(vs: (String, Value)*): Value = MapValue(Context(vs: _*), vs.isEmpty)
  def lambda(f: Section => Find[String]): Value = LambdaValue(f)

  case class SimpleValue(v: String) extends Value {
    val isEmpty = v.isEmpty
  }
  case class BoolValue(v: Boolean) extends Value {
    val isEmpty = !v
  }
  case class ListValue(v: Seq[Value]) extends Value {
    lazy val isEmpty = v.isEmpty
  }
  case class MapValue(ctx: Context, isEmpty: Boolean) extends Value
  case class LambdaValue(f: Section => Find[String]) extends Value {
    val isEmpty = false
  }
}
