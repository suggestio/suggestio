package io.suggest.ym.model

import cascading.tuple.{TupleEntry, Tuple}
import io.suggest.ym.OfferTypes.OfferType
import io.suggest.util.{ImplicitTupleCoercions, CascadingFieldNamer}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 19:07
 * Description: Модель датумов для промо-офферов.
 * Такой оффер используется для завлекухи, не содержа в себе конкретных данных о товаре.
 */
object YmPromoOfferDatum extends Serializable {

  /** Базовый размер одежды в исходных единицах. */
  val SIZES_ORIG_PFN      = "sizesOrig"
  val SIZES_UNIT_ORIG_PFN = "sizesUnitOrig"

  def serializeSizes(sizes: Seq[String]) = new Tuple(sizes : _*)
  val deserializeSizes: PartialFunction[AnyRef, Seq[String]] = {
    case null     => Nil
    case t: java.lang.Iterable[_] =>
      t.toSeq.map(ImplicitTupleCoercions.coerceString)
  }


  def fromJson(jsonMap: collection.Map[String, AnyRef], srcDatum: YmPromoOfferDatum = new YmPromoOfferDatum()): YmPromoOfferDatum = {
    YmOfferDatum.fromJson(jsonMap, srcDatum)
  }
}

import YmPromoOfferDatum._

/** Тут промо-оффер. Такой оффер содержит в некоторых полях (размер, цвет) множественные значения. */
class YmPromoOfferDatum extends AbstractYmOfferDatum {

  def this(t: Tuple) = {
    this
    setTuple(t)
    getTupleEntry
  }

  def this(te: TupleEntry) = {
    this
    setTupleEntry(te)
  }

  def this(url: String, offerType:OfferType, shop: YmShopDatum) = {
    this
    this.url = url
    this.offerType = offerType
    this.shopMeta = shop
  }


  def sizesOrig = {
    val raw = getPayloadValue(SIZES_ORIG_PFN)
    deserializeSizes(raw)
  }
  def sizesOrig_=(sizes: Seq[String]) = {
    val ser = serializeSizes(sizes)
    setPayloadValue(SIZES_ORIG_PFN, ser)
  }


  def sizesUnitsOrig = _tupleEntry getString SIZES_UNIT_ORIG_PFN
  def sizesUnitsOrig_=(units: String) = {
    _tupleEntry.setString(SIZES_UNIT_ORIG_PFN, units)
  }

}
