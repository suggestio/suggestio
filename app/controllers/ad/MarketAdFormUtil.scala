package controllers.ad

import models._
import util.FormUtil._
import play.api.data._, Forms._
import play.api.Play.current
import io.suggest.ym.parsers.Price
import util.blocks.BlocksUtil.BlockImgMap
import util.blocks.BlockMapperResult
import io.suggest.ym.model.ad.RichDescr

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


  /** Маппинг для выравнивания текста в рамках поля. */
  def textAlignOptM: Mapping[Option[TextAlign]] = {
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


  private def fontSizeOptM(fontSizes: Seq[FontSize]): Mapping[Option[FontSize]] = {
    val hasFontSizes = fontSizes.nonEmpty
    val (min, max) = if (hasFontSizes) {
      val minSz = fontSizes.iterator.map(_.size).min
      val maxSz = fontSizes.iterator.map(_.size).max
      minSz -> maxSz
    } else {
      0 -> 0
    }
    optional(number(min = min, max = max))
      .transform[Option[FontSize]](
        {szOpt =>
          if (hasFontSizes) {
            szOpt flatMap { sz => fontSizes.find(_.size == sz) }
          } else {
            None
          }
        },
        { _.map(_.size) }
      )
     .verifying("error.unavailable.font.size", { szOpt => !hasFontSizes || szOpt.isDefined })
  }

  /** Маппер для значения font.family. */
  def fontFamilyOptM: Mapping[Option[String]] = {
    optional(
      // TODO RELEASE: Добавить валидацию перед запуском
      text(maxLength = 32)
    )
  }


  /** Маппер для описания, прилагаемого к рекламной карточке. */
  def richDescrOptM: Mapping[Option[RichDescr]] = {
    val rdTextM = text(maxLength = 8192)
      .transform(strFmtTrimF, strIdentityF)
    val m = mapping(
      "bgColor" -> colorM,
      "text"    -> rdTextM
    )
    {(bgColor, rdText) =>
      if (rdText.isEmpty) {
        None
      } else {
        Some(RichDescr(bgColor = bgColor, text = rdText))
      }
    }
    {rdOpt =>
      val bgColor = rdOpt.fold("FFFFFF")(_.bgColor)
      val rdText  = rdOpt.fold("")(_.text)
      Some( (bgColor, rdText) )
    }
    optional(m)
      .transform(_.flatten, Option.apply)
  }


  /**
   * Сборка маппинга для шрифта.
   * @param withFontSizes Множество допустимых размеров шрифтов, если пусто то поле отключено.
   * @return Маппинг для AOFieldFont.
   */
  def getFontM(withFontSizes: Seq[FontSize]): Mapping[AOFieldFont] = {
    mapping(
      "color"  -> colorM,
      "size"   -> fontSizeOptM(withFontSizes),
      "align"  -> textAlignOptM,
      "family" -> fontFamilyOptM
    )
    {(color, fsz, align, family) =>
      AOFieldFont(
        color  = color,
        size   = fsz.map(_.size),
        align  = align,
        family = family,
        lineHeight = fsz.map(_.lineHeight)
      )
    }
    {aoff =>
      import aoff._
      val fsz: Option[FontSize] = aoff.size.flatMap { sz =>
        withFontSizes.find(_.size == sz)
      }
      Some((color, fsz, align, family))
    }
  }


  /** Маппер для активации и настройки покрывающей сетки-паттерна указанного цвета. */
  def coveringPatternM: Mapping[Option[String]] = {
    tuple(
      "enabled" -> boolean,
      "color"   -> optional(colorM)
    )
    .verifying("error.required", {m => m match {
      case (true, None)   => false
      case _              => true
    }})
    .transform[Option[String]] (
      { case (isEnabled, colorOpt) => colorOpt.filter(_ => isEnabled) },
      { colorOpt => (colorOpt.isDefined, colorOpt) }
    )
  }


  /** Парсер координаты. Координата может приходить нецелой, поэтому нужно округлить. */
  def coordM: Mapping[Int] = {
    // TODO Достаточно парсить первые цифры до $ или до десятичной точки/запятой, остальное отбрасывать.
    doubleM
      .transform[Int](_.toInt, _.toDouble)
      .verifying("error.coord.too.big", { _ <= 2048 })
      .verifying("error.coord.negative", { _ >= 0 })
  }
  def coords2DM: Mapping[Coords2D] = {
    // сохраняем маппинг в переменную на случай если coordM станет def вместо val.
    val _coordM = coordM
    mapping(
      "x" -> _coordM,
      "y" -> _coordM
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

  val COVERING_PATTERN_COLOR_FN = "cpc"

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


  /** apply-функция для формы добавления/редактировать рекламной карточки.
    * Вынесена за пределы генератора ad-маппингов во избежание многократного создания в памяти экземпляров функции. */
  def adFormApply(userCatId: Option[String], bmr: BlockMapperResult, pattern: Option[String], richDescrOpt: Option[RichDescr]): AdFormMResult = {
    val colors1: Map[String, String] = {
      if (pattern.isDefined)
        bmr.bd.colors + (COVERING_PATTERN_COLOR_FN -> pattern.get)
      else
        bmr.bd.colors
    }
    val mad = MAd(
      producerId  = null,
      offers      = bmr.bd.offers,
      blockMeta   = bmr.bd.blockMeta,
      colors      = colors1,
      imgs        = null,
      userCatId   = userCatId,
      richDescrOpt = richDescrOpt
    )
    mad -> bmr.bim
  }

  /** Функция разборки для маппинга формы добавления/редактирования рекламной карточки. */
  def adFormUnapply(applied: AdFormMResult): Option[(Option[String], BlockMapperResult, Option[String], Option[RichDescr])] = {
    val mad = applied._1
    val bmr = BlockMapperResult(mad, applied._2)
    val pattern = mad.colors.get(COVERING_PATTERN_COLOR_FN)
    Some( (mad.userCatId, bmr, pattern, mad.richDescrOpt) )
  }


  val OFFER_K = "offer"

  /**
   * Определить владельца категорий узла.
   * @param adnNode Узел рекламной сети.
   * @return id узла-владельца категорий.
   */
  def getCatOwnerId(adnNode: MAdnNode): String = {
    /*
    import AdNetMemberTypes._
    adnNode.adn.memberType match {
      case SHOP | RESTAURANT => adnNode.adn.supId getOrElse adnNode.id.get
      case MART | RESTAURANT_SUP => adnNode.id.get
    }
    */
    MMartCategory.DEFAULT_OWNER_ID
  }

}

