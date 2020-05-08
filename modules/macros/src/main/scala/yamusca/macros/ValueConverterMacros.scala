package yamusca.macros

import scala.reflect.macros.blackbox.Context

// thanks https://stackoverflow.com/questions/19544756/scala-macros-accessing-members-with-quasiquotes
object ValueConverterMacros {

  def valueConverterImpl[T: c.WeakTypeTag](c: Context): c.Tree = {
    import c.universe._

    val tpa = weakTypeOf[T]
    val fields = tpa.decls.collect {
      case field if field.isMethod && field.asMethod.isCaseAccessor =>
        field.name.toTermName
    }
    val cases = fields.map { f =>
      q"""if (${TermName(
        f.toString
      ).decodedName.toString} == key) { result = Some(a.$f.asMustacheValue) }"""
    }

    if (fields.isEmpty)
      c.abort(
        c.enclosingPosition,
        s"No accessors for type `$tpa'. This is only meant for case clases."
      )
    else {
      val quasi = q"""new ValueConverter[$tpa] {
        def apply(a: $tpa): Value = Value.fromContext(Context.from { key =>
           var result: Option[Value] = None

           ${cases.toList}

           result
         }, false)
      }"""
      quasi
    }
  }
}
