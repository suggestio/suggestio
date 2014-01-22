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

  def store = getTupleEntry getBoolean companion.STORE_FN
  def store_=(store: Boolean) {
    getTupleEntry.setBoolean(companion.STORE_FN, store)
  }

  def isPickup = getTupleEntry getBoolean companion.PICKUP_FN
  def isPickup_=(pickup: Boolean) {
    getTupleEntry.setBoolean(companion.PICKUP_FN, pickup)
  }

  def isDelivery = getTupleEntry getBoolean companion.DELIVERY_FN
  def isDelivery_=(delivery: Boolean) {
    getTupleEntry.setBoolean(companion.DELIVERY_FN, delivery)
  }

  def isDeliveryIncluded = getTupleEntry getBoolean companion.DELIVERY_INCLUDED_FN
  def isDeliveryIncluded_=(deliveryIncluded: Boolean) {
    getTupleEntry.setBoolean(companion.DELIVERY_INCLUDED_FN, deliveryIncluded)
  }

  def localDeliveryCost: Option[Float] = {
    val raw = getTupleEntry.getFloat(companion.LOCAL_DELIVERY_COST_FN)
    if (raw < 0) None else Some(raw)
  }
  def localDeliveryCost_=(ldcOpt: Option[Float]) {
    getTupleEntry.setFloat(companion.LOCAL_DELIVERY_COST_FN, ldcOpt getOrElse -1F)
  }

  def isAdult: Boolean = getTupleEntry getBoolean companion.ADULT_FN
  def isAdult_=(adult: Boolean) {
    getTupleEntry.setBoolean(companion.ADULT_FN, adult)
  }
}
