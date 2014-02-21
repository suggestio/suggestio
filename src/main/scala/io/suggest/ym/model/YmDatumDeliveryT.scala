package io.suggest.ym.model

import cascading.tuple.{Fields, TupleEntry}
import com.scaleunlimited.cascading.Payload
import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.14 12:31
 * Description: Поля store?, pickup?, delivery?, deliveryIncluded?, local_delivery_cost?, adult присутствуют в разных
 * датумах. Тут общий код поддержки этого, подмешиваемый в эти датумы.
 */
trait YmDatumDeliveryStaticT {
  def STORE_FN: String
  def PICKUP_FN: String
  def DELIVERY_FN: String
  def DELIVERY_INCLUDED_FN: String
  def LOCAL_DELIVERY_COST_FN: String
  def ADULT_FN: String

  def FIELDS = new Fields(STORE_FN, PICKUP_FN, DELIVERY_FN, DELIVERY_INCLUDED_FN, LOCAL_DELIVERY_COST_FN, ADULT_FN)
}

trait YmDatumDeliveryT {
  def companion: YmDatumDeliveryStaticT
  // Напрямую запользовать _tupleEntry нельзя, ибо он java protected.
  def getTupleEntry: TupleEntry

  /** store: Элемент позволяет указать возможность купить товар в розничном магазине. */
  def isStore = getTupleEntry getBoolean companion.STORE_FN
  def isStore_=(store: Boolean) {
    getTupleEntry.setBoolean(companion.STORE_FN, store)
  }

  /** pickup: Доступность резервирования с самовывозом. */
  def isPickup = getTupleEntry getBoolean companion.PICKUP_FN
  def isPickup_=(pickup: Boolean) {
    getTupleEntry.setBoolean(companion.PICKUP_FN, pickup)
  }

  /** delivery: Допустима ли доставка для указанного товара? */
  def isDelivery = getTupleEntry getBoolean companion.DELIVERY_FN
  def isDelivery_=(delivery: Boolean) {
    getTupleEntry.setBoolean(companion.DELIVERY_FN, delivery)
  }

  /** Включена ли доставка в стоимость товара/товаров магазина? */
  def isDeliveryIncluded = getTupleEntry getBoolean companion.DELIVERY_INCLUDED_FN
  def isDeliveryIncluded_=(deliveryIncluded: Boolean) {
    getTupleEntry.setBoolean(companion.DELIVERY_INCLUDED_FN, deliveryIncluded)
  }

  /** Стоимость доставки товара в своем регионе. */
  def localDeliveryCostOpt: Option[Float] = {
    val raw = getTupleEntry.getFloat(companion.LOCAL_DELIVERY_COST_FN)
    if (raw < 0) None else Some(raw)
  }
  def localDeliveryCostOpt_=(ldcOpt: Option[Float]) {
    getTupleEntry.setFloat(companion.LOCAL_DELIVERY_COST_FN, ldcOpt getOrElse -1F)
  }

  /** Это товар/магазин "для взрослых"? */
  def isAdult: Boolean = getTupleEntry getBoolean companion.ADULT_FN
  def isAdult_=(adult: Boolean) {
    getTupleEntry.setBoolean(companion.ADULT_FN, adult)
  }
}
