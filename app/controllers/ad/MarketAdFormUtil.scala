package controllers.ad

import models._
import util.img.ImgFormUtil._
import util.FormUtil._
import play.api.data._, Forms._
import util.img._
import play.api.Play.current
import AOTextAlignValues.TextAlignValue
import io.suggest.ym.parsers.Price
import io.suggest.ym.model.common
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


  /** Генератор маппингов для шрифтов.
    * @param fontSizes Допустимые размеры шрифтов.
    * @return Маппинг, возвращающий int-размер шрифта.
    */
  def fontSizeM(fontSizes: Set[Int]): Mapping[Int] = {
    number(min = fontSizes.min, max = fontSizes.max)
     .verifying("error.unawailable.font.size", fontSizes.contains(_))
  }

  /**
   * Сборка маппинга для шрифта.
   * @param withFontColor Включить font color?
   * @param withFontSizes Множество допустимых размеров шрифтов, если пусто то поле отключено.
   * @param default Дефолтовое значение.
   * @return Маппинг для AOFieldFont.
   */
  def getFontM(withFontColor: Boolean = true, withFontSizes: Set[Int] = Set.empty, default: AOFieldFont): Mapping[AOFieldFont] = {
    val withFontSize = !withFontSizes.isEmpty
    (withFontColor, withFontSize) match {
      case (true, true) =>
        mapping(
          "color" -> colorM,
          "size"  -> fontSizeM(withFontSizes)
        )
        {(color, sz) =>
          AOFieldFont(color, Some(sz)) }
        {aoff =>
          val fontSz = aoff.size.orElse(default.size).getOrElse(10)
          Some((aoff.color, fontSz))}

      case (true, false) =>
        mapping("color" -> colorM)
        {color => AOFieldFont(color) }
        {aoff  => Some(aoff.color) }

      case (false, true) =>
        mapping("sz" -> fontSizeM(withFontSizes))
        {sz => AOFieldFont(default.color, Some(sz)) }
        {aoff => aoff.size }

      case (false, false) =>
        // Это заглушка, чтобы не было экзепшенов. Маловероятно, что до сюда будет доходить выполнение кода.
        mapping("_stub_" -> optional(text(maxLength = 1)))
        {_ => default }
        {_ => None }
    }
  }

  /** Маппим строковое поле с настройками шрифта. */
  def aoStringFieldM(m : Mapping[String], fontM: Mapping[AOFieldFont]) = mapping(
    "value" -> m,
    "font"  -> fontM
  )
  { AOStringField.apply }
  { AOStringField.unapply }

  /** Маппим числовое (Float) поле. */
  def aoFloatFieldM(m: Mapping[Float], fontM: Mapping[AOFieldFont]) = mapping(
    "value" -> m,
    "font"  -> fontM
  )
  { AOFloatField.apply }
  { AOFloatField.unapply }

  /** Поле с ценой. Является вариацией float-поля. */
  def mmaPriceM(fontM: Mapping[AOFieldFont]) = mapping(
    "value" -> priceStrictM,
    "font" -> fontM
  )
  {case ((rawPrice, price), font) =>
    AOPriceField(price.price, price.currency.getCurrencyCode, rawPrice, font) }
  {mmadp =>
    import mmadp._
    Some((orig, Price(value, currency)), font)
  }

  /** Поле с необязательной ценой. Является вариацией float-поля. Жуткий говнокод. */
  def aoPriceOptM(fontM: Mapping[AOFieldFont]) = mapping(
    "value" -> optional(priceStrictM),
    "font" -> fontM
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


  /** Маппим необязательное Float-поле. */
  def aoFloatFieldOptM(m: Mapping[Float], fontM: Mapping[AOFieldFont]) = mapping(
    "value" -> optional(m),
    "font" -> fontM
  )
  {(valueOpt, color) =>
    valueOpt map { AOFloatField(_, color) }
  }
  {_.map { mmaff =>
    (Option(mmaff.value), mmaff.font)
  }}


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


/** Мапперы для textAlign'ов. Пока не используются и живут тут. Потом может быть будут удалены. */
object MarketAdTextAlignUtil {

  /** Какие-то данные для text-align'a. */
  val textAlignRawM = nonEmptyText(maxLength = 16)
    .transform(strTrimSanitizeLowerF, strIdentityF)
    .transform[Option[TextAlignValue]](
      { AOTextAlignValues.maybeWithName },
      { tavOpt => tavOpt.map(_.toString) getOrElse "" }
    )
    .verifying("text.align.value.invalid", { _.isDefined })
    // Переводим результаты обратно в строки для более надежной работы reflections в TA-моделях.
    .transform(
      _.get.toString,
      { AOTextAlignValues.maybeWithName }
    )

  /** Маппинг для textAlign.phone -- параметры размещения текста на экране телефона. */
  val taPhoneM = textAlignRawM
    .transform[common.TextAlignPhone](
      { TextAlignPhone.apply },
      { taPhone => taPhone.align }
    )

  /** Маппинг для textAlign.tablet -- параметров размещения текста на планшете. */
  val taTabletM = mapping(
    "top"    -> textAlignRawM,
    "bottom" -> textAlignRawM
  )
  { TextAlignTablet.apply }
  { TextAlignTablet.unapply }

  /** Маппинг для всего textAlign. */
  val textAlignM = mapping(
    "phone"  -> taPhoneM,
    "tablet" -> taTabletM
  )
  { TextAlign.apply }
  { TextAlign.unapply }


  val textAlignKM = "textAlign" -> textAlignM
    .transform[Option[common.TextAlign]](
      Some.apply,
      { _ getOrElse TextAlign(TextAlignPhone(""), TextAlignTablet("", "")) }
    )

}

