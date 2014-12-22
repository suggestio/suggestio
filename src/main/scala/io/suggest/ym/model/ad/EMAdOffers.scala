package io.suggest.ym.model.ad

import io.suggest.model.{EsModel, EsModelPlayJsonT, EsModelStaticMutAkvT}
import java.util.Currency
import java.{util => ju}
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.ym.model.AdOfferType
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
  val FAMILY_ESFN       = "family"
  val LINE_HEIGHT_ESFN  = "lineHeight"

  val FONT_COLOR_DFLT   = "FFFFFF"

  // DISCOUNT offer
  val TEXT1_ESFN        = "text1"
  val TEXT2_ESFN        = "text2"
  val DISCOUNT_ESFN     = "discount"
  val HREF_ESFN         = "href"

  /** В списке офферов порядок поддерживается с помощью поля n, которое поддерживает порядок по возрастанию. */
  val N_ESFN            = "n"

  val COORDS_ESFN       = "coords"
}

import EMAdOffers._


trait EMAdOffersStatic extends EsModelStaticMutAkvT {
  override type T <: EMAdOffersMut

  abstract override def generateMappingProps: List[DocField] = {
    // vfields0 содержит поля, присущие для всех сериализованных AOValueField.
    val vfields0 = List(
      FieldObject(FONT_ESFN, enabled = false, properties = Nil),
      FieldObject(COORDS_ESFN, enabled = false, properties = Nil)
    )
    // Сгенерить поле строкового значения (.value)
    def stringValueField(boost: Float = 1.0F) = FieldString(
      VALUE_ESFN,
      index = FieldIndexingVariants.no,
      include_in_all = true,
      boost = Some(boost)
    )
    // Сгенерить поле числового значения (.value)
    def floatValueField(iia: Boolean) = {
      FieldNumber(VALUE_ESFN,  fieldType = DocFieldTypes.float,  index = FieldIndexingVariants.no,  include_in_all = iia)
    }
    // Основной список полей для AOPriceField дополняется валютой и исходной ценой.
    val priceFields0 = {
      FieldString(CURRENCY_CODE_ESFN, include_in_all = false, index = FieldIndexingVariants.no) ::
      FieldString(ORIG_ESFN, include_in_all = false, index = FieldIndexingVariants.no) ::
      vfields0
    }
    // Маппинг для объекта, представляющего сериализованный AOBlock.
    val offerBodyProps = Seq(
      // product-поля
      // discount-поля
      FieldObject(TEXT1_ESFN, properties = stringValueField(1.1F) :: vfields0),
      FieldObject(DISCOUNT_ESFN, properties = floatValueField(iia = true) :: vfields0),
      FieldObject(TEXT2_ESFN, properties = stringValueField(0.9F) :: vfields0),
      FieldString(HREF_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
    )
    // Полный маппинг для поля offer.
    val offersField = FieldNestedObject(OFFERS_ESFN,
      enabled = true,
      //includeInRoot = true,
      properties = Seq(
        // TODO Отсутствие необходимости в индексировании OFFER_TYPE была обнаружена лишь перед самым стартом. Надо бы тут index = no выставить...
        FieldString(OFFER_TYPE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
        FieldNumber(N_ESFN, index = FieldIndexingVariants.no, include_in_all = false, fieldType = DocFieldTypes.integer),
        FieldObject(OFFER_BODY_ESFN, enabled = true, properties = offerBodyProps)
      )
    )
    // Закинуть результат в аккамулятор.
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

trait EMAdOffersI extends EsModelPlayJsonT with IOffers {
  override type T <: EMAdOffersI
}

/** read-only аддон для экземпляра [[io.suggest.model.EsModelPlayJsonT]] для добавления поддержки работы с полем offers. */
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
        AOBlock(
          n = n,
          text1 = Option(m get TEXT1_ESFN)
            .flatMap(AOStringField.deserializeOpt),
          text2 = Option(m get TEXT2_ESFN)
            .flatMap(AOStringField.deserializeOpt),
          discount = Option(m get DISCOUNT_ESFN)
            .flatMap(AOFloatField.deserializeOpt),
          href = Option(m get HREF_ESFN)
            .map(EsModel.stringParser)
        )
    }
  }

}


// TODO Нужно дедублицировать text1 и text2, убрать discount и все price'ы. Сделать immutable и что-то решить с href.
// Поле n наверное остаётся нужен для упорядочивания. Хотя и это тоже не обязательно.
case class AOBlock(
  n             : Int,
  var text1     : Option[AOStringField] = None,
  var text2     : Option[AOStringField] = None,
  var discount  : Option[AOFloatField] = None,
  var href      : Option[String] = None
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
    if (href.isDefined)
      acc ::= HREF_ESFN -> JsString(href.get)
    acc
  }
}


object AOValueField {
  def getAndDeserializeFont(jm: ju.Map[_,_]): AOFieldFont = {
    Option(jm.get(FONT_ESFN)).fold(AOFieldFont(FONT_COLOR_DFLT))(AOFieldFont.deserialize)
  }
  
  def getAndDeserializeCoords(jm: ju.Map[_,_]): Option[Coords2D] = {
    Option(jm.get(COORDS_ESFN)).map(Coords2D.deserialize)
  }
}
sealed trait AOValueField {
  def font: AOFieldFont
  def coords: Option[Coords2D]

  @JsonIgnore
  def renderPlayJson = {
    var acc = font.renderPlayJsonFields(Nil)
    acc = renderPlayJsonFields(acc)
    if (coords.isDefined)
      acc = coords.get.renderPlayJsonFields(acc)
    JsObject(acc)
  }
  
  def renderPlayJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc
}


object AOStringField {
  def getAndDeserializeValue(jm: ju.Map[_,_]): String = {
    Option(jm.get(VALUE_ESFN))
      .fold("")(EsModel.stringParser)
  }
  
  val deserializeOpt: PartialFunction[Any, Option[AOStringField]] = {
    case null => None
    case jm: ju.Map[_,_] =>
      if (jm.isEmpty) {
        None
      } else {
        val result = AOStringField(
          value  = getAndDeserializeValue(jm),
          font   = AOValueField.getAndDeserializeFont(jm),
          coords = AOValueField.getAndDeserializeCoords(jm)
        )
        Some(result)
      }
  }
}
case class AOStringField(value:String, font: AOFieldFont, coords: Option[Coords2D] = None) extends AOValueField {

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


object AOFloatField {
  def getAndDeserializeValue(jm: ju.Map[_,_]): Float = {
    Option(jm.get(VALUE_ESFN)).fold(0F)(EsModel.floatParser)
  }

  val deserializeOpt: PartialFunction[Any, Option[AOFloatField]] = {
    case null => None
    case jm: ju.Map[_,_] =>
      if (jm.isEmpty) {
        None
      } else {
        val result = AOFloatField(
          value  = getAndDeserializeValue(jm),
          font   = AOValueField.getAndDeserializeFont(jm),
          coords = AOValueField.getAndDeserializeCoords(jm)
        )
        Some(result)
      }
  }
}
case class AOFloatField(value: Float, font: AOFieldFont, coords: Option[Coords2D] = None) extends AOFloatFieldT


/** Список допустимых значений для выравнивания текста. */
object TextAligns extends Enumeration {
  protected case class Val(strId: String, cssName: String) extends super.Val(strId)
  type TextAlign = Val

  val Left    : TextAlign = Val("l", "left")
  val Right   : TextAlign = Val("r", "right")
  val Center  : TextAlign = Val("c", "center")
  val Justify : TextAlign = Val("j", "justify")

  def maybeWithName(s: String): Option[TextAlign] = {
    try {
      Some(withName(s))
    } catch {
      case ex: NoSuchElementException => None
    }
  }

  def maybeWithCssName(cssName: String): Option[TextAlign] = {
    values.find(_.cssName equalsIgnoreCase cssName)
      .asInstanceOf[Option[TextAlign]]
  }

  implicit def value2val(x: Value): TextAlign = x.asInstanceOf[TextAlign]
}


import TextAligns.TextAlign


object AOFieldFont {
  val deserialize: PartialFunction[Any, AOFieldFont] = {
    case jm: ju.Map[_,_] =>
      AOFieldFont(
        color  = Option(jm.get(COLOR_ESFN))
          .fold(FONT_COLOR_DFLT)(EsModel.stringParser),
        size   = Option(jm.get(SIZE_ESFN))
          .map(EsModel.intParser),
        align  = Option(jm.get(ALIGN_ESFN))
          .map(EsModel.stringParser)
          .flatMap(TextAligns.maybeWithName),
        family = Option(jm.get(FAMILY_ESFN))
          .map(EsModel.stringParser),
        lineHeight = Option(jm.get(LINE_HEIGHT_ESFN))
          .map(EsModel.intParser)
      )
  }
}

/**
 * Описание шрифтоты.
 * @param color Цвет шрифта.
 * @param size Необязательный размер шрифта.
 */
case class AOFieldFont(
  color       : String,
  size        : Option[Int] = None,
  align       : Option[TextAlign] = None,
  family      : Option[String] = None,
  lineHeight  : Option[Int] = None
) {
  def renderPlayJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    var fieldsAcc: FieldsJsonAcc = List(
      COLOR_ESFN -> JsString(color)
    )
    if (family.isDefined)
      fieldsAcc ::= FAMILY_ESFN -> JsString(family.get)
    if (align.isDefined)
      fieldsAcc ::= ALIGN_ESFN -> JsString(align.get.toString())
    if (size.isDefined)
      fieldsAcc ::= SIZE_ESFN -> JsNumber(size.get)
    if (lineHeight.isDefined)
      fieldsAcc ::= LINE_HEIGHT_ESFN -> JsNumber(lineHeight.get)
    val fontBody = JsObject(fieldsAcc)
    (FONT_ESFN, fontBody) :: acc
  }
}


object AOPriceField {
  val deserializeOpt: PartialFunction[Any, Option[AOPriceField]] = {
    case null => None
    case jm: ju.Map[_,_] =>
      if (jm.isEmpty) {
        None
      } else {
        val result = AOPriceField(
          value = AOFloatField.getAndDeserializeValue(jm),
          currencyCode = Option(jm.get(CURRENCY_CODE_ESFN))
            .fold("RUB")(EsModel.stringParser),
          orig = Option(jm.get(ORIG_ESFN))
            .fold("")(EsModel.stringParser),
          font = AOValueField.getAndDeserializeFont(jm),
          coords = AOValueField.getAndDeserializeCoords(jm)
        )
        Some(result)
      }
  }
}

/** Поле, содержащее цену. */
case class AOPriceField(
  value   : Float,
  currencyCode: String,
  orig    : String,
  font    : AOFieldFont,
  coords  : Option[Coords2D] = None
)
  extends AOFloatFieldT {
  @JsonIgnore
  lazy val currency = Currency.getInstance(currencyCode)

  override def renderPlayJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    (CURRENCY_CODE_ESFN, JsString(currencyCode)) ::
      (ORIG_ESFN, JsString(orig)) ::
      super.renderPlayJsonFields(acc0)
  }
}


object Coords2D {
  val X_ESFN = "x"
  val Y_ESFN = "y"

  def getAndDeserializeCoord(fn: String, jm: ju.Map[_,_]): Int = {
    Option(jm.get(fn)).fold(0)(EsModel.intParser)
  }

  val deserialize: PartialFunction[Any, Coords2D] = {
    case jm: ju.Map[_,_] =>
      Coords2D(
        x = getAndDeserializeCoord(X_ESFN, jm),
        y = getAndDeserializeCoord(Y_ESFN, jm)
      )
  }
}


/** Интерфейс для 2D-координат. */
trait ICoords2D {
  def x: Int
  def y: Int
}

case class Coords2D(x: Int, y: Int) extends ICoords2D {

  import Coords2D._

  def renderPlayJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc = {
    val obj = JsObject(Seq(
      X_ESFN -> JsNumber(x),
      Y_ESFN -> JsNumber(y)
    ))
    COORDS_ESFN -> obj :: acc0
  }

}

