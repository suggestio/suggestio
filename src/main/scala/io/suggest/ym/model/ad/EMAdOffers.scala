package io.suggest.ym.model.ad

import io.suggest.model.EsModelT
import io.suggest.model.EsModelStaticT
import java.util.Currency
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonIgnore}
import io.suggest.ym.model.AdOfferType
import io.suggest.util.JacksonWrapper
import io.suggest.ym.model.common.AdOfferTypes
import io.suggest.util.SioConstants.CURRENCY_CODE_DFLT
import io.suggest.util.SioEsUtil._
import scala.collection.JavaConversions._
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json._
import java.util

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 19:04
 * Description: Поле рекламных офферов, лежащих внутри рекламных карточек.
 */
object EMAdOffers {
  /** Название поля, в котором складируются офферы. */
  val OFFERS_ESFN       = "offers"

  /** Имя поля с телом оффера. Нужно т.к. тип оффера хранится отдельно от оффера. */
  val OFFER_BODY_ESFN   = "offerBody"

  /** Название поля, хранящее тип оффера. */
  val OFFER_TYPE_ESFN   = "offerType"

  // price field
  val CURRENCY_CODE_ESFN = "currencyCode"
  val ORIG_ESFN = "orig"

  // Названия общих полей офферов
  val FONT_ESFN         = "font"
  val SIZE_ESFN         = "size"
  val COLOR_ESFN        = "color"
  val TEXT_ALIGN_ESFN   = "textAlign"
  val ALIGN_ESFN        = "align"
  val VALUE_ESFN        = "value"

  // PRODUCT offer
  val VENDOR_ESFN       = "vendor"
  val MODEL_ESFN        = "model"
  val PRICE_ESFN        = "price"
  val OLD_PRICE_ESFN    = "oldPrice"

  // DISCOUNT offer
  val TEXT1_ESFN        = "text1"
  val TEXT2_ESFN        = "text2"
  val DISCOUNT_ESFN     = "discount"
  val DISCOUNT_TPL_ESFN = "discoTpl"

  // TEXT offer
  val TEXT_ESFN         = "text"
}

import EMAdOffers._


trait EMAdOffersStatic[T <: EMAdOffersMut[T]] extends EsModelStaticT[T] {

  abstract override def generateMappingProps: List[DocField] = {
    val fontField = FieldObject(FONT_ESFN, enabled = false, properties = Nil)
    def stringValueField(boost: Float = 1.0F) = FieldString(
      VALUE_ESFN,
      index = FieldIndexingVariants.no,
      include_in_all = true,
      boost = Some(boost)
    )
    def floatValueField(iia: Boolean) = {
      FieldNumber(VALUE_ESFN,  fieldType = DocFieldTypes.float,  index = FieldIndexingVariants.no,  include_in_all = iia)
    }
    def priceFields(iia: Boolean) = Seq(
      floatValueField(iia),
      fontField,
      FieldString("currencyCode", include_in_all = false, index = FieldIndexingVariants.no),
      FieldString("orig", include_in_all = false, index = FieldIndexingVariants.no)
    )
    // Поле приоритета. На первом этапе null или число.
    val offerBodyProps = Seq(
      // product-поля
      FieldObject(VENDOR_ESFN, properties = Seq(stringValueField(1.5F), fontField)),
      FieldObject(MODEL_ESFN, properties = Seq(stringValueField(), fontField)),
      // TODO нужно как-то проанализировать цифры эти, округлять например.
      FieldObject(PRICE_ESFN,  properties = priceFields(iia = true)),
      FieldObject(OLD_PRICE_ESFN,  properties = priceFields(iia = false)),
      // discount-поля
      FieldObject(TEXT1_ESFN, properties = Seq(stringValueField(1.1F), fontField)),
      FieldObject(DISCOUNT_ESFN, properties = Seq(floatValueField(iia = true), fontField)),
      FieldObject(DISCOUNT_TPL_ESFN, enabled = false, properties = Nil),
      FieldObject(TEXT2_ESFN, properties = Seq(stringValueField(0.9F), fontField)),
      // text-поля
      FieldObject(TEXT_ESFN, properties = Seq(
        // HTML будет пострипан тут автоматом.
        FieldString(VALUE_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
        stringValueField(),
        fontField
      ))
    )
    val offersField = FieldNestedObject(OFFERS_ESFN, enabled = true, properties = Seq(
      FieldString(OFFER_TYPE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldObject(OFFER_BODY_ESFN, enabled = true, properties = offerBodyProps)
    ))
    offersField :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (OFFERS_ESFN, value: java.lang.Iterable[_]) =>
        acc.offers = value.toList.map {
          case jsObject: java.util.Map[_, _] =>
            jsObject.get(OFFER_TYPE_ESFN) match {
              case ots: String =>
                val ot = AdOfferTypes.withName(ots)
                val offerBody = jsObject.get(OFFER_BODY_ESFN)
                import AdOfferTypes._
                ot match {
                  case RAW      => AORaw.deserialize(offerBody)
                  case PRODUCT  => AOProduct.deserialize(offerBody)
                  case DISCOUNT => AODiscount.deserialize(offerBody)
                  case TEXT     => AOText.deserialize(offerBody)
                }
            }
        }
    }
  }

}


trait EMAdOffers[T <: EMAdOffers[T]] extends EsModelT[T] {

  def offers: List[AdOfferT]

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (!offers.isEmpty) {
      val offersJson = offers.map(_.renderPlayJson)
      (OFFERS_ESFN, JsArray(offersJson)) :: acc0
    } else {
      acc0
    }
  }

}


trait EMAdOffersMut[T <: EMAdOffersMut[T]] extends EMAdOffers[T] {
  var offers: List[AdOfferT]
}


// -------------- Далее идёт конструктор, из которого собираются офферы ---------------

sealed trait AdOfferT extends Serializable {
  @JsonIgnore
  def offerType: AdOfferType

  @JsonIgnore
  def renderPlayJson = {
    JsObject(Seq(
      OFFER_TYPE_ESFN -> JsString(offerType.toString),
      OFFER_BODY_ESFN -> JsObject(renderPlayJsonBody)
    ))
  }

  @JsonIgnore
  def renderPlayJsonBody: FieldsJsonAcc
}


object AORaw {
  def deserialize(jsObject: Any) = AORaw(
    bodyMap = JacksonWrapper.convert[Map[String,Any]](jsObject)
  )

  def convertJsValue2play(v: Any): JsValue = {
    v match {
      case null       => JsNull
      case i: Int     => JsNumber(i)
      case s: String  => JsString(s)
      case d: Double  => JsNumber(d)
      case f: Float   => JsNumber(f)
      case l: Long    => JsNumber(l)
      case b: Boolean => JsBoolean(b)
      case s: Short   => JsNumber(s)
      case m: java.util.Map[_,_] =>
        JsObject(convertJacksonJson2play(m))
      case m: collection.Map[_,_] =>
        JsObject(convertJacksonJson2play(m))
      case l: java.lang.Iterable[_] =>
        JsArray(l.map(convertJsValue2play).toSeq)
      case l: Iterable[_] =>
        JsArray(l.map(convertJsValue2play).toSeq)
    }
  }

  def convertJacksonJson2play(o: collection.Map[_, _]): FieldsJsonAcc = {
    o.foldLeft[FieldsJsonAcc] (Nil) { case (acc, (k, v)) =>
      val jsV = convertJsValue2play(v)
      k.toString -> jsV :: acc
    }
  }

}


case class AORaw(
  bodyMap: Map[String, Any]
) extends AdOfferT {
  @JsonIgnore
  override def offerType = AdOfferTypes.RAW

  @JsonIgnore
  override def renderPlayJsonBody: FieldsJsonAcc = {
    AORaw.convertJacksonJson2play(bodyMap)
  }
}



object AOProduct {
  def deserialize(jsObject: Any) = JacksonWrapper.convert[AOProduct](jsObject)
}

@JsonIgnoreProperties(Array("currencyCode")) // 27.03.2014 Было удалено поле до 1-запуска. Потом можно это удалить.
case class AOProduct(
  vendor:   AOStringField,
  price:    AOPriceField,
  oldPrice: Option[AOPriceField]
) extends AdOfferT {
  @JsonIgnore def offerType = AdOfferTypes.PRODUCT

  @JsonIgnore
  def renderPlayJsonBody: FieldsJsonAcc = {
    var acc: FieldsJsonAcc = List(
      VENDOR_ESFN -> vendor.renderPlayJson,
      PRICE_ESFN  -> price.renderPlayJson
    )
    if (oldPrice.isDefined)
      acc ::= OLD_PRICE_ESFN -> oldPrice.get.renderPlayJson
    acc
  }
}


object AODiscount {
  def deserialize(jsObject: Any): AODiscount = {
    jsObject match {
      case m: java.util.Map[_,_] =>
        val acc = AODiscount(null, null, null, null)
        m foreach {
          case (TEXT1_ESFN, text1Raw) =>
            acc.text1 = JacksonWrapper.convert[Option[AOStringField]](text1Raw)
          case (DISCOUNT_ESFN, discoRaw) =>
            acc.discount = JacksonWrapper.convert[AOFloatField](discoRaw)
          case (DISCOUNT_TPL_ESFN | "template", tplRaw) =>
            acc.template = JacksonWrapper.convert[AODiscountTemplate](tplRaw)
          case (TEXT2_ESFN, text2Raw) =>
            acc.text2 = JacksonWrapper.convert[Option[AOStringField]](text2Raw)
        }
        acc
    }
    //JacksonWrapper.convert[AODiscount](jsObject)
  }
}
case class AODiscount(
  var text1: Option[AOStringField],
  var discount: AOFloatField,
  var template: AODiscountTemplate,
  var text2: Option[AOStringField]
) extends AdOfferT {
  @JsonIgnore def offerType = AdOfferTypes.DISCOUNT

  @JsonIgnore
  def renderPlayJsonBody: FieldsJsonAcc = {
    var acc: FieldsJsonAcc = List(
      DISCOUNT_ESFN -> discount.renderPlayJson,
      DISCOUNT_TPL_ESFN -> template.renderPlayJson
    )
    if (text1.isDefined)
      acc ::= (TEXT1_ESFN, text1.get.renderPlayJson)
    if (text2.isDefined)
      acc ::= (TEXT2_ESFN, text2.get.renderPlayJson)
    acc
  }
}


object AOText {
  def deserialize(jsObject: Any) = JacksonWrapper.convert[AOText](jsObject)
}
case class AOText(text: AOStringField) extends AdOfferT {
  @JsonIgnore
  def offerType = AdOfferTypes.TEXT

  @JsonIgnore
  def renderPlayJsonBody: FieldsJsonAcc = List(
    TEXT_ESFN -> text.renderPlayJson
  )
}


case class AODiscountTemplate(id: Int, color: String) {
  @JsonIgnore
  def renderPlayJson = {
    JsObject(Seq(
      "id" -> JsNumber(id),
      COLOR_ESFN -> JsString(color)
    ))
  }
}

sealed trait AOValueField {
  def font: AOFieldFont

  @JsonIgnore
  def renderPlayJson = {
    var acc0 = font.renderPlayJsonFields(Nil)
    acc0 = renderPlayJsonFields(acc0)
    JsObject(acc0)
  }
  
  def renderPlayJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc
}

case class AOStringField(value:String, font: AOFieldFont) extends AOValueField {
  def renderPlayJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    (VALUE_ESFN, JsString(value)) :: acc0
  }
}

trait AOFloatFieldT extends AOValueField {
  def value: Float

  def renderPlayJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    (VALUE_ESFN, JsNumber(value)) :: acc0
  }
}
case class AOFloatField(value: Float, font: AOFieldFont) extends AOFloatFieldT


case class AOFieldFont(color: String) {
  def renderPlayJsonFields(acc: FieldsJsonAcc) = {
    val fontBody = JsObject(Seq(
      COLOR_ESFN -> JsString(color)
    ))
    (FONT_ESFN, fontBody) :: acc
  }
}


/** Поле, содержащее цену. */
case class AOPriceField(value: Float, currencyCode: String, orig: String, font: AOFieldFont) extends AOFloatFieldT {
  @JsonIgnore
  lazy val currency = Currency.getInstance(currencyCode)

  override def renderPlayJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    (CURRENCY_CODE_ESFN, JsString(currencyCode)) ::
      (ORIG_ESFN, JsString(orig)) ::
      super.renderPlayJsonFields(acc0)
  }
}



