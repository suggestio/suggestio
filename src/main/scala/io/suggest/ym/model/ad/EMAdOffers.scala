package io.suggest.ym.model.ad

import io.suggest.model.{EsModel, EsModelT, EsModelStaticT}
import java.util.Currency
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.ym.model.AdOfferType
import io.suggest.util.JacksonWrapper
import io.suggest.ym.model.common.AdOfferTypes
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
  val MODEL_ESFN        = "model"
  val PRICE_ESFN        = "price"
  val OLD_PRICE_ESFN    = "oldPrice"

  // DISCOUNT offer
  val TEXT1_ESFN        = "text1"
  val TEXT2_ESFN        = "text2"
  val DISCOUNT_ESFN     = "discount"

  /** В списке офферов порядок поддерживается с помощью поля n, которое поддерживает порядок по возрастанию. */
  val N_ESFN            = "n"

}

import EMAdOffers._


trait EMAdOffersStatic extends EsModelStaticT {
  override type T <: EMAdOffersMut

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
      // TODO нужно как-то проанализировать цифры эти, округлять например.
      FieldObject(PRICE_ESFN,  properties = priceFields(iia = true)),
      FieldObject(OLD_PRICE_ESFN,  properties = priceFields(iia = false)),
      // discount-поля
      FieldObject(TEXT1_ESFN, properties = Seq(stringValueField(1.1F), fontField)),
      FieldObject(DISCOUNT_ESFN, properties = Seq(floatValueField(iia = true), fontField)),
      FieldObject(TEXT2_ESFN, properties = Seq(stringValueField(0.9F), fontField))
    )
    val offersField = FieldNestedObject(OFFERS_ESFN,
      enabled = true,
      //includeInRoot = true,
      properties = Seq(
        FieldString(OFFER_TYPE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
        FieldNumber(N_ESFN, index = FieldIndexingVariants.no, include_in_all = false, fieldType = DocFieldTypes.integer),
        FieldObject(OFFER_BODY_ESFN, enabled = true, properties = offerBodyProps)
      )
    )
    offersField :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (OFFERS_ESFN, value: java.lang.Iterable[_]) =>
        acc.offers = value.toList
          .map(AdOffer.deserializeOne)
          .sortBy(_.n)
    }
  }

}


/** Интерфейс поля offers. Вынесен их [[EMAdOffers]] из-за потребностей blocks-инфраструктуры. */
trait IOffers {
  def offers: List[AOBlock]
}

trait EMAdOffersI extends EsModelT with IOffers {
  override type T <: EMAdOffersI
}

/** read-only аддон для экземпляра [[io.suggest.model.EsModelT]] для добавления поддержки работы с полем offers. */
trait EMAdOffers extends EMAdOffersI {
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


trait EMAdOffersMut extends EMAdOffers {
  override type T <: EMAdOffersMut
  var offers: List[AOBlock]
}


// -------------- Далее идёт конструктор, из которого собираются офферы ---------------
object AdOffer {

  /** Десериализовать один оффер. */
  def deserializeOne(x: Any): AOBlock = {
    x match {
      case jsObject: java.util.Map[_, _] =>
        jsObject.get(OFFER_TYPE_ESFN) match {
          case ots: String =>
            val n: Int = Option(jsObject.get(N_ESFN)).map(EsModel.intParser).getOrElse(0)
            AdOfferTypes.maybeWithName(ots) match {
              case Some(ot) =>
                val offerBody = jsObject.get(OFFER_BODY_ESFN)
                import AdOfferTypes._
                ot match {
                  case BLOCK => AOBlock.deserializeBody(offerBody, n)
                }
              // Старые AOProduct, AODiscount, AOText удалены -- тут обработка. // TODO Удалить это после первого полугодия 2014.
              case None =>
                AOBlock(
                  n = n,
                  text1 = Some(AOStringField("Оффер в старом формате не поддерживается.", font = AOFieldFont("888888")))
                )
            }

        }
    }
  }

}

/** Абстрактный оффер. */
sealed trait AdOfferT extends Serializable {
  @JsonIgnore
  def offerType: AdOfferType

  /** Порядковый номер оффера в списке офферов. Нужен для поддержания исходного порядка. */
  def n: Int

  @JsonIgnore
  def renderPlayJson = {
    // Метаданные оффера содержат его порядковый номер и тип. Body содержит сами данные по офферу.
    JsObject(Seq(
      N_ESFN          -> JsNumber(n),
      OFFER_TYPE_ESFN -> JsString(offerType.toString),
      OFFER_BODY_ESFN -> JsObject(renderPlayJsonBody)
    ))
  }

  @JsonIgnore
  def renderPlayJsonBody: FieldsJsonAcc
}


object AOBlock {
  /** Десериализация тела блочного оффера. */
  def deserializeBody(jsObject: Any, n: Int) = {
    jsObject match {
      case m: java.util.Map[_,_] =>
        val acc = AOBlock(n)
        m foreach {
          case (TEXT1_ESFN, text1Raw) =>
            acc.text1 = JacksonWrapper.convert[Option[AOStringField]](text1Raw)
          case (DISCOUNT_ESFN, discoRaw) =>
            acc.discount = Option(JacksonWrapper.convert[AOFloatField](discoRaw))
          case (TEXT2_ESFN, text2Raw) =>
            acc.text2 = JacksonWrapper.convert[Option[AOStringField]](text2Raw)
          case (PRICE_ESFN, priceRaw) =>
            acc.price = Option(JacksonWrapper.convert[AOPriceField](priceRaw))
          case (OLD_PRICE_ESFN, priceOldRaw) =>
            acc.oldPrice = Option(JacksonWrapper.convert[AOPriceField](priceOldRaw))
          case (other, v) =>
            println("AOBlock.deserializeBody: Skipping unknown field: " + other + " = " + v)
        }
        acc
    }
  }

}


case class AOBlock(
  var n: Int,
  var text1: Option[AOStringField] = None,
  var text2: Option[AOStringField] = None,
  var discount: Option[AOFloatField] = None,
  var price: Option[AOPriceField]  = None,
  var oldPrice: Option[AOPriceField] = None
) extends AdOfferT {
  @JsonIgnore
  override def offerType = AdOfferTypes.BLOCK

  @JsonIgnore
  override def renderPlayJsonBody: FieldsJsonAcc = {
    var acc: FieldsJsonAcc = Nil
    if (text1.isDefined)
      acc ::= TEXT1_ESFN -> text1.get.renderPlayJson
    if (text2.isDefined)
      acc ::= TEXT2_ESFN -> text2.get.renderPlayJson
    if (discount.isDefined)
      acc ::= DISCOUNT_ESFN -> discount.get.renderPlayJson
    if (price.isDefined)
      acc ::= PRICE_ESFN -> price.get.renderPlayJson
    if (oldPrice.isDefined)
      acc ::= OLD_PRICE_ESFN -> oldPrice.get.renderPlayJson
    acc
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


/**
 * Описание шрифтоты.
 * @param color Цвет шрифта.
 * @param size Необязательный размер шрифта.
 */
case class AOFieldFont(color: String, size: Option[Int] = None) {
  def renderPlayJsonFields(acc: FieldsJsonAcc) = {
    var fieldsAcc: FieldsJsonAcc = List(
      COLOR_ESFN -> JsString(color)
    )
    if (size.isDefined)
      fieldsAcc ::= SIZE_ESFN -> JsNumber(size.get)
    val fontBody = JsObject(fieldsAcc)
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



