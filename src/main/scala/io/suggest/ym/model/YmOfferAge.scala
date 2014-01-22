package io.suggest.ym.model

import io.suggest.util.CascadingFieldNamer
import cascading.tuple.{TupleEntry, Tuple, Fields}
import io.suggest.ym.OfferAgeUnits, OfferAgeUnits.OfferAgeUnit
import com.scaleunlimited.cascading.BaseDatum

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.14 14:51
 * Description: Поле age нередко указано, в месяцах или годах.
 */
object YmOfferAge extends CascadingFieldNamer {
  val UNITS_FN = fieldName("units")
  val VALUE_FN = fieldName("value")

  val FIELDS = new Fields(UNITS_FN, VALUE_FN)

  def serializeUnits(u: OfferAgeUnit) = u.id
  def deserializeUnits(uid: Int) = OfferAgeUnits(uid)
}


import YmOfferAge._

class YmOfferAge extends BaseDatum(FIELDS) {

  def this(t: Tuple) = {
    this
    setTuple(t)
  }

  def this(te: TupleEntry) = {
    this
    setTupleEntry(te)
  }

  def this(units: OfferAgeUnit, value: Int) = {
    this
    setUnits(units)
    setValue(value)
  }


  def getUnits = deserializeUnits(_tupleEntry getInteger UNITS_FN)
  def setUnits(units: OfferAgeUnit) = {
    val i = serializeUnits(units)
    _tupleEntry.setInteger(UNITS_FN, i)
    this
  }

  def getValue = _tupleEntry getInteger VALUE_FN
  def setValue(value: Int) = {
    _tupleEntry.setInteger(VALUE_FN, value)
    this
  }
}
