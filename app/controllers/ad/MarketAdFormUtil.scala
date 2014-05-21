package controllers.ad

import models._
import util.FormUtil._
import play.api.data._, Forms._
import play.api.Play.current
import io.suggest.ym.parsers.Price
import util.blocks.BlocksUtil.BlockImgMap
import util.blocks.BlockMapperResult

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:15
 * Description: Общая утиль для работы с разными ad-формами: preview и обычными.
 */
object MarketAdFormUtil {

  type AdFormMResult = (MAd, BlockImgMap)
  type AdFormM = Form[AdFormMResult]

  type FormDetected_t = Option[(AdOfferType, AdFormM)]


  // Есть шаблоны для шаблона скидки. Они различаются по id. Тут min и max для допустимых id.
  val DISCOUNT_TPL_ID_MIN = current.configuration.getInt("ad.discount.tpl.id.min") getOrElse 1
  val DISCOUNT_TPL_ID_MAX = current.configuration.getInt("ad.discount.tpl.id.max") getOrElse 6


  /** Маппинг для выравнивания текста в рамках поля. */
  val textAlignOptM: Mapping[Option[TextAlign]] = {
    optional(text(maxLength = 10))
      .transform[Option[TextAlign]](
        {_.filter(_.length <= 10)
          .flatMap { s =>
            if (s.length == 1) {
              TextAligns.maybeWithName(s)
            } else if (s.length == 0) {
              None
            } else {
              TextAligns.maybeWithCssName(s)
            }
          }
        },
        { _.map(_.cssName) }
      )
  }


  private def fontSizeOptM(fontSizes: Set[Int]): Mapping[Option[Int]] = {
    val hasFontSizes = fontSizes.isEmpty
    val (min, max) = if (hasFontSizes) {
      0 -> 0
    } else {
      fontSizes.min -> fontSizes.max
    }
    optional(number(min = min, max = max))
     .transform[Option[Int]](
        { szOpt => if (hasFontSizes) szOpt.filter(fontSizes.contains) else szOpt },
        { identity }
      )
     .verifying("error.unavailable.font.size", { szOpt => hasFontSizes || szOpt.exists(fontSizes.contains) })
  }

  val fontFamilyOptM: Mapping[Option[String]] = optional(text(maxLength = 15))

  /**
   * Сборка маппинга для шрифта.
   * @param withFontSizes Множество допустимых размеров шрифтов, если пусто то поле отключено.
   * @return Маппинг для AOFieldFont.
   */
  def getFontM(withFontSizes: Set[Int]): Mapping[AOFieldFont] = {
    mapping(
      "color"  -> colorM,
      "size"   -> fontSizeOptM(withFontSizes),
      "align"  -> textAlignOptM,
      "family" -> fontFamilyOptM
    )
    { AOFieldFont.apply }
    { AOFieldFont.unapply }
  }


  val coordM = number(min = 0, max = 2048)
  val coords2DM: Mapping[Coords2D] = {
    mapping(
      "x" -> coordM,
      "y" -> coordM
    )
    { Coords2D.apply }
    { Coords2D.unapply }
  }
  val coords2DOptM: Mapping[Option[Coords2D]] = optional(coords2DM)


  /** Маппим строковое поле с настройками шрифта. */
  def aoStringFieldM(m: Mapping[String], fontM: Mapping[AOFieldFont], withCoords: Boolean): Mapping[AOStringField] = {
    if (withCoords) {
      mapping(
        "value" -> m,
        "font"  -> fontM,
        "coords" -> coords2DOptM
      )
      { AOStringField.apply }
      { AOStringField.unapply }
    } else {
      mapping(
        "value" -> m,
        "font"  -> fontM
      )
      { AOStringField.apply(_, _) }
      { aosf => AOStringField.unapply(aosf).map { u => u._1 -> u._2 } }
    }
  }

  /** Маппим числовое (Float) поле. */
  def aoFloatFieldM(m: Mapping[Float], fontM: Mapping[AOFieldFont], withCoords: Boolean): Mapping[AOFloatField] = {
    if (withCoords) {
      mapping(
        "value"  -> m,
        "font"   -> fontM,
        "coords" -> coords2DOptM
      )
      { AOFloatField.apply }
      { AOFloatField.unapply }
    } else {
      mapping(
        "value" -> m,
        "font"  -> fontM
      )
      { AOFloatField.apply(_, _) }
      { AOFloatField.unapply(_).map { u => u._1 -> u._2} }
    }
  }

  /** Поле с ценой. Является вариацией float-поля. */
  def aoPriceFieldM(fontM: Mapping[AOFieldFont], withCoords: Boolean): Mapping[AOPriceField] = {
    if (withCoords) {
      mapping(
        "value"  -> priceStrictM,
        "font"   -> fontM,
        "coords" -> coords2DOptM
      )
      {case ((rawPrice, price), font, coordsOpt) =>
        AOPriceField(price.price, price.currency.getCurrencyCode, rawPrice, font, coordsOpt)
      }
      {mmadp =>
        import mmadp._
        Some( (orig -> Price(value, currency), font, coords) )
      }
    } else {
      mapping(
        "value" -> priceStrictM,
        "font"  -> fontM
      )
      {case ((rawPrice, price), font) =>
        AOPriceField(price.price, price.currency.getCurrencyCode, rawPrice, font) }
      {mmadp =>
        import mmadp._
        Some( (orig -> Price(value, currency), font) )
      }
    }
  }


  /** Поле с необязательной ценой. Является вариацией float-поля. Жуткий говнокод. */
  def aoPriceOptM(fontM: Mapping[AOFieldFont], withCoords: Boolean): Mapping[Option[AOPriceField]] = {
    if (withCoords) {
      mapping(
        "value"  -> optional(priceStrictM),
        "font"   -> fontM,
        "coords" -> coords2DOptM
      )
      {(pricePairOpt, font, coordsOpt) =>
        pricePairOpt.map { case (rawPrice, price) =>
          AOPriceField(price.price, price.currency.getCurrencyCode, rawPrice, font, coordsOpt)
        }
      }
      {_.map { mmadp =>
        import mmadp._
        (Some(orig -> Price(value, currency)), font, coords)
      }}
    } else {
      mapping(
        "value" -> optional(priceStrictM),
        "font"  -> fontM
      )
      {(pricePairOpt, font) =>
        pricePairOpt.map { case (rawPrice, price) =>
          AOPriceField(price.price, price.currency.getCurrencyCode, rawPrice, font)
        }
      }
      {_.map { mmadp =>
        import mmadp._
        (Some(orig -> Price(value, currency)), font)
      }}
    }
  }


  /** Маппим необязательное Float-поле. */
  def aoFloatFieldOptM(m: Mapping[Float], fontM: Mapping[AOFieldFont], withCoords: Boolean): Mapping[Option[AOFloatField]] = {
    if (withCoords) {
      mapping(
        "value"  -> optional(m),
        "font"   -> fontM,
        "coords" -> coords2DOptM
      )
      {(valueOpt, color, coordsOpt) =>
        valueOpt map { AOFloatField(_, color, coordsOpt) }
      }
      {_.map { mmaff =>
        (Option(mmaff.value), mmaff.font, mmaff.coords)
      }}
    } else {
      mapping(
        "value" -> optional(m),
        "font"  -> fontM
      )
      {(valueOpt, color) =>
        valueOpt map { AOFloatField(_, color) }
      }
      {_.map { mmaff =>
        (Option(mmaff.value), mmaff.font)
      }}
    }
  }


  val DISCOUNT_TEXT_MAXLEN = 256

  val AD_TEXT_MAXLEN = 256


  // Дублирующиеся куски маппина выносим за пределы метода.
  val CAT_ID_K = "catId"
  val AD_IMG_ID_K = "image_key"


  /** apply-функция для формы добавления/редактировать рекламной карточки.
    * Вынесена за пределы генератора ad-маппингов во избежание многократного создания в памяти экземпляров функции. */
  def adFormApply(userCatId: Option[String], bmr: BlockMapperResult): AdFormMResult = {
    val mad = MAd(
      producerId  = null,
      offers      = bmr.bd.offers,
      blockMeta   = bmr.bd.blockMeta,
      colors      = bmr.bd.colors,
      imgs        = null,
      userCatId   = userCatId
    )
    mad -> bmr.bim
  }

  /** Функция разборки для маппинга формы добавления/редактирования рекламной карточки. */
  def adFormUnapply(applied: AdFormMResult): Option[(Option[String], BlockMapperResult)] = {
    val mad = applied._1
    val bmr = BlockMapperResult(mad, applied._2)
    Some( (mad.userCatId, bmr) )
  }


  /*
  val panelColorM = colorM
    .transform(
      { AdPanelSettings.apply },
      { mmaps: common.AdPanelSettings => mmaps.color }
    )
  val PANEL_COLOR_K = "panelColor"
  val panelColorKM = PANEL_COLOR_K -> panelColorM
  */


  val OFFER_K = "offer"

  /**
   * Определить владельца категорий узла.
   * @param adnNode Узел рекламной сети.
   * @return id узла-владельца категорий.
   */
  def getCatOwnerId(adnNode: MAdnNode): String = {
    import AdNetMemberTypes._
    adnNode.adn.memberType match {
      case SHOP | RESTAURANT => adnNode.adn.supId getOrElse adnNode.id.get
      case MART | RESTAURANT_SUP => adnNode.id.get
    }
  }

}

