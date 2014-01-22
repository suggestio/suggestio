package io.suggest.ym.model

import cascading.tuple.TupleEntry

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.14 12:31
 * Description: Поля store?, pickup?, delivery?, deliveryIncluded?, local_delivery_cost?, adult присутствуют в разных
 * датумах. Тут общий код поддержки этого, подмешиваемый в эти датумы.
 */
trait YmDatumDeliveryStaticT {
  val STORE_FN: String
  val PICKUP_FN: String
  val DELIVERY_FN: String
  val DELIVERY_INCLUDED_FN: String
  val LOCAL_DELIVERY_COST_FN: String
  val ADULT_FN: String
}

trait YmDatumDeliveryT {
  def companion: YmDatumDeliveryStaticT
  // Напрямую запользовать _tupleEntry раньше было нельзя, ибо он java protected.
  def getTupleEntry: TupleEntry

  def getStore = getTupleEntry getBoolean companion.STORE_FN
  def setStore(store: Boolean) = {
    getTupleEntry.setBoolean(companion.STORE_FN, store)
    this
  }

  def isPickup = getTupleEntry getBoolean companion.PICKUP_FN
  def setPickup(pickup: Boolean) = {
    getTupleEntry.setBoolean(companion.PICKUP_FN, pickup)
    this
  }

  def isDelivery = getTupleEntry getBoolean companion.DELIVERY_FN
  def setDelivery(delivery: Boolean) = {
    getTupleEntry.setBoolean(companion.DELIVERY_FN, delivery)
    this
  }

  def isDeliveryIncluded = getTupleEntry getBoolean companion.DELIVERY_INCLUDED_FN
  def setDeliveryIncluded(deliveryIncluded: Boolean) = {
    getTupleEntry.setBoolean(companion.DELIVERY_INCLUDED_FN, deliveryIncluded)
    this
  }

  def getLocalDeliveryCost: Option[Float] = {
    val raw = getTupleEntry.getFloat(companion.LOCAL_DELIVERY_COST_FN)
    if (raw < 0) None else Some(raw)
  }
  def setLocalDeliveryCost(ldcOpt: Option[Float]) = {
    getTupleEntry.setFloat(companion.LOCAL_DELIVERY_COST_FN, ldcOpt getOrElse -1F)
    this
  }

  def isAdult: Boolean = getTupleEntry getBoolean companion.ADULT_FN
  def setAdult(adult: Boolean) = {
    getTupleEntry.setBoolean(companion.ADULT_FN, adult)
    this
  }
}
