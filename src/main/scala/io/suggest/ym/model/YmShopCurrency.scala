package io.suggest.ym.model

import io.suggest.util.CascadingFieldNamer
import com.scaleunlimited.cascading.BaseDatum
import cascading.tuple.{TupleEntry, Tuple, Fields}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.14 10:56
 * Description: Хранилище одной валюты одного магазина.
 */
object YmShopCurrency extends CascadingFieldNamer {
  val ID_FN   = fieldName("id")
  val RATE_FN = fieldName("rate")
  val PLUS_FN = fieldName("plus")

  val FIELDS = new Fields(ID_FN, RATE_FN, PLUS_FN)
}


import YmShopCurrency._

class YmShopCurrency extends BaseDatum(FIELDS) {

  def this(t: Tuple) = {
    this
    setTuple(t)
  }

  def this(te: TupleEntry) = {
    this
    setTupleEntry(te)
  }

  def this(id:String, rate:String, plus:String) {
    this
    this.id = id
    this.rate = rate
    this.plus = plus
  }

  def id = _tupleEntry getString ID_FN
  def id_=(id: String) {
    _tupleEntry.setString(ID_FN, id)
  }

  def rate = _tupleEntry getString RATE_FN
  def rate_=(rate: String) {
    _tupleEntry.setString(RATE_FN, rate)
  }

  def plus = _tupleEntry getString PLUS_FN
  def plus_=(plus: String) {
    _tupleEntry.setString(PLUS_FN, plus)
  }

}
