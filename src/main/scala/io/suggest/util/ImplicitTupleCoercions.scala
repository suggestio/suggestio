package io.suggest.util

import cascading.tuple.coerce.Coercions.{INTEGER, FLOAT, BOOLEAN, STRING, LONG, DOUBLE}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 13:49
 * Description: Автоматические приведения типов при работе с элементами кортежей.
 */
object ImplicitTupleCoercions {
  implicit def coerceInt(v: Any)     = INTEGER.coerce(v).intValue()
  implicit def coerceFloat(v: Any)   = FLOAT.coerce(v).floatValue()
  implicit def coerceString(v: Any)  = STRING.coerce(v)
  implicit def coerceLong(v: Any)    = LONG.coerce(v).longValue()
  implicit def coerceDouble(v: Any)  = DOUBLE.coerce(v).doubleValue()
  implicit def coerceBoolean(v: Any) = BOOLEAN.coerce(v).booleanValue()
}
