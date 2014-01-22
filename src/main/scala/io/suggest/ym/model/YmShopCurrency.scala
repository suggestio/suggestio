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
    setId(id)
    setRate(rate)
    setPlus(plus)
  }

  def getId = _tupleEntry getString ID_FN
  def setId(id: String) = {
    _tupleEntry.setString(ID_FN, id)
    this
  }

  def getRate = _tupleEntry getString RATE_FN
  def setRate(rate: String) = {
    _tupleEntry.setString(RATE_FN, rate)
    this
  }

  def getPlus = _tupleEntry getString PLUS_FN
  def setPlus(plus: String) = {
    _tupleEntry.setString(PLUS_FN, plus)
    this
  }

}
