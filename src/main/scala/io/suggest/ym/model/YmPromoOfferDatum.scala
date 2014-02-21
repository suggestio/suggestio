package io.suggest.ym.model

import cascading.tuple.{TupleEntry, Tuple}
import io.suggest.ym.OfferTypes.OfferType
import io.suggest.util.ImplicitTupleCoercions
import scala.collection.JavaConversions._
import scala.collection.Map
import io.suggest.ym.YmColors.YmColor
import io.suggest.ym.YmColors

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 19:07
 * Description: Модель датумов для промо-офферов.
 * Такой оффер используется для завлекухи, не содержа в себе конкретных данных о товаре.
 */
object YmPromoOfferDatum extends YmOfferDatumStaticT[YmPromoOfferDatum] with Serializable {

  /** Базовый размер одежды в исходных единицах. */
  val SIZES_ORIG_PFN      = "sizesOrig"
  /** Исходные единицы размера. */
  val SIZES_UNIT_ORIG_PFN = "sizesUnitOrig"

  /** Цвета, в которых доступно предложение. */
  val COLORS_PFN          = "colors"

  override val FIELDS = super.FIELDS


  def serializeSizes(sizes: Seq[String]) = new Tuple(sizes : _*)
  val deserializeSizes: PartialFunction[AnyRef, Seq[String]] = {
    case null =>
      Nil
    case t: java.lang.Iterable[_] =>
      t.toSeq.map(ImplicitTupleCoercions.coerceString)
    case a: Array[String] =>
      a.toSeq
  }


  def serializeColors(colors: Seq[YmColor]): Tuple = {
    val t = new Tuple
    colors foreach { color =>
      t addString color.toString
    }
    t
  }
  val deserializeColors: PartialFunction[AnyRef, Seq[YmColor]] = {
    case null =>
      Nil
    case t: java.lang.Iterable[_] =>
      t.toSeq.map { rawColor =>
        val color = ImplicitTupleCoercions.coerceString(rawColor)
        YmColors.withName(color)
      }
    case a: Array[String] =>
      a.toSeq.map(YmColors.withName)
  }


  override val payloadV2jsV: Payload2JsF_t = {
    val applyF: Payload2JsF_t = {
      case (k @ SIZES_ORIG_PFN, value, xcb) =>
        val sizes = deserializeSizes(value)
        if (!sizes.isEmpty) {
          xcb.startArray(k)
          sizes.foreach { size =>
            xcb.value(size)
          }
          xcb.endArray()
        }

      case (k @ SIZES_UNIT_ORIG_PFN, value, xcb) =>
        if (value != null) {
          val units = ImplicitTupleCoercions coerceString value
          xcb.field(k, units)
        }

      case (k @ COLORS_PFN, value, xcb) =>
        if (value != null) {
          xcb.startArray(k)
          deserializeColors(value).foreach { color =>
            xcb value color.toString
          }
          xcb.endArray()
        }
    }
    applyF orElse super.payloadV2jsV
  }


  override def fromJsonMapper(srcDatum: YmPromoOfferDatum): FromJsonMapperF_t = {
    import srcDatum._
    import ImplicitTupleCoercions._
    val applyF: PartialFunction[(String, AnyRef), Unit] = {
      case (SIZES_ORIG_PFN, value)      => sizesOrig = deserializeSizes(value)
      case (SIZES_UNIT_ORIG_PFN, value) => sizeUnitsOrig = value
      case (COLORS_PFN, value)          => colors = deserializeColors(value)
    }
    applyF orElse super.fromJsonMapper(srcDatum)
  }


  override def fromJson(jsonMap: Map[String, AnyRef], srcDatum: YmPromoOfferDatum = new YmPromoOfferDatum) = {
    super.fromJson(jsonMap, srcDatum)
  }
}

import YmPromoOfferDatum._

/** Тут промо-оффер. Такой оффер содержит в некоторых полях (размер, цвет) множественные значения. */
class YmPromoOfferDatum extends AbstractYmOfferDatum(FIELDS) {
  def companion = YmPromoOfferDatum

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


  def sizeUnitsOrig = getPayloadString(SIZES_UNIT_ORIG_PFN)
  def sizeUnitsOrig_=(units: String) = {
    setPayloadValue(SIZES_UNIT_ORIG_PFN, units)
  }


  def colors = deserializeColors(getPayloadValue(COLORS_PFN))
  def colors_=(colors: Seq[YmColor]) {
    val colorsSer = serializeColors(colors)
    setPayloadValue(COLORS_PFN, colorsSer)
  }

}

