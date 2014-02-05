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
object YmOfferAge extends CascadingFieldNamer with Serializable {
  val UNITS_FN = fieldName("units")
  val VALUE_FN = fieldName("value")

  val FIELDS = new Fields(UNITS_FN, VALUE_FN)

  def serializeUnits(u: OfferAgeUnit) = u.id
  def deserializeUnits(uid: Int) = OfferAgeUnits(uid)

  def deserializeFromString(s: String): YmOfferAge = {
    val Array(v, u) = s.split("\\s")
    val age = v.toInt
    val units = OfferAgeUnits.withName(u)
    new YmOfferAge(value=age, units=units)
  }
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
    this.units = units
    this.value = value
  }


  def units = deserializeUnits(_tupleEntry getInteger UNITS_FN)
  def units_=(units: OfferAgeUnit) = {
    val i = serializeUnits(units)
    _tupleEntry.setInteger(UNITS_FN, i)
    this
  }

  def value = _tupleEntry getInteger VALUE_FN
  def value_=(value: Int) = {
    _tupleEntry.setInteger(VALUE_FN, value)
    this
  }

  override def toString: String = value.toString + " " + units
}
