package io.suggest.ym.model.ad

import io.suggest.model.EsModelT
import io.suggest.model.EsModelStaticT
import java.util.Currency
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonIgnore}
import io.suggest.ym.model.AdOfferType
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.JacksonWrapper
import io.suggest.ym.model.common.AdOfferTypes
import io.suggest.util.SioConstants.CURRENCY_CODE_DFLT
import io.suggest.util.SioEsUtil._
import scala.collection.JavaConversions._
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json._

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
                  case PRODUCT => AOProduct.deserialize(offerBody)
                  case DISCOUNT => AODiscount.deserialize(offerBody)
                  case TEXT => AOText.deserialize(offerBody)
                }
            }
        }
    }
  }

}


trait EMAdOffers[T <: EMAdOffers[T]] extends EsModelT[T] {

  def offers: List[AdOfferT]

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    super.writeJsonFields(acc)
    if (!offers.isEmpty) {
      val offersJson = offers.map(_.renderPlayJson)
      (OFFERS_ESFN, JsArray(offersJson)) :: acc
    } else acc
  }

}


trait EMAdOffersMut[T <: EMAdOffersMut[T]] extends EMAdOffers[T] {
  var offers: List[AdOfferT]
}


// -------------- Далее идёт конструктор, из которого собираются офферы ---------------

sealed trait AdOfferT extends Serializable {
  @JsonIgnore def offerType: AdOfferType
  def renderJson(acc: XContentBuilder) {
    acc.startObject()
    acc.field(OFFER_TYPE_ESFN, offerType.toString)
    val offerBodyJson = JacksonWrapper.serialize(this)
    acc.rawField(OFFER_BODY_ESFN, offerBodyJson.getBytes)
    acc.endObject()
  }
  
  @JsonIgnore
  def renderPlayJson = {
    JsObject(Seq(
      OFFER_TYPE_ESFN -> JsString(offerType.toString),
      OFFER_BODY_ESFN -> JsObject(renderPlayJsonBody)
    ))
  }
  
  def renderPlayJsonBody: FieldsJsonAcc
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
  def deserialize(jsObject: Any) = JacksonWrapper.convert[AODiscount](jsObject)
}
case class AODiscount(
  text1: Option[AOStringField],
  discount: AOFloatField,
  template: AODiscountTemplate,
  text2: Option[AOStringField]
) extends AdOfferT {
  @JsonIgnore def offerType = AdOfferTypes.DISCOUNT

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
  def renderXCB(acc: XContentBuilder) {
    acc.startObject(DISCOUNT_TPL_ESFN)
      .field("id", id)
      .field(COLOR_ESFN, color)
    .startObject()
  }

  def renderPlayJson = {
    JsObject(Seq(
      "id" -> JsNumber(id),
      COLOR_ESFN -> JsString(color)
    ))
  }
}

sealed trait AOValueField {
  def renderFieldsXCB(acc: XContentBuilder)
  def font: AOFieldFont
  def renderJson(acc: XContentBuilder) {
    acc.startObject()
      renderFieldsXCB(acc)
      font.renderXCB(acc)
    acc.endObject()
  }

  @JsonIgnore
  def renderPlayJson = {
    var acc0 = font.renderPlayJson(Nil)
    acc0 = renderPlayJsonFields(acc0)
    JsObject(acc0)
  }
  
  def renderPlayJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc
}

case class AOStringField(value:String, font: AOFieldFont) extends AOValueField {
  def renderFieldsXCB(acc: XContentBuilder) {
    acc.field(VALUE_ESFN, value)
  }

  def renderPlayJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    (VALUE_ESFN, JsString(value)) :: acc0
  }
}

trait AOFloatFieldT extends AOValueField {
  def value: Float
  def renderFieldsXCB(acc: XContentBuilder) {
    acc.field(VALUE_ESFN, value)
  }

  def renderPlayJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    (VALUE_ESFN, JsNumber(value)) :: acc0
  }
}
case class AOFloatField(value: Float, font: AOFieldFont) extends AOFloatFieldT


case class AOFieldFont(color: String) {
  def renderXCB(acc: XContentBuilder) {
    acc.startObject(FONT_ESFN)
      .field(COLOR_ESFN, color)
    .endObject()
  }
  
  def renderPlayJson(acc: FieldsJsonAcc) = {
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

  override def renderFieldsXCB(acc: XContentBuilder) {
    ??? // TODO XCB-поддержка для полей не написана, т.к. целиком использовался Jackson.
    super.renderFieldsXCB(acc)
  }

  override def renderPlayJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    (CURRENCY_CODE_ESFN, JsString(currencyCode)) ::
      (ORIG_ESFN, JsString(orig)) ::
      super.renderPlayJsonFields(acc0)
  }
}

/** Допустимые значения textAlign-полей. */
object AOTextAlignValues extends Enumeration {
  type TextAlignValue = Value
  val left, right = Value

  def maybeWithName(n: String): Option[TextAlignValue] = {
    try {
      Some(withName(n))
    } catch {
      case _: Exception => None
    }
  }
}

