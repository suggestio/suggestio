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

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 10:15
 * Description: Общая утиль для работы с разными ad-формами: preview и обычными.
 */
object MarketAdFormUtil {

  type AdFormM = Form[(ImgIdKey, LogoOpt_t, MAd)]

  type FormDetected_t = Option[(AdOfferType, AdFormM)]


  // Есть шаблоны для шаблона скидки. Они различаются по id. Тут min и max для допустимых id.
  val DISCOUNT_TPL_ID_MIN = current.configuration.getInt("ad.discount.tpl.id.min") getOrElse 1
  val DISCOUNT_TPL_ID_MAX = current.configuration.getInt("ad.discount.tpl.id.max") getOrElse 6

  /** Шрифт пока что характеризуется только цветом. Поэтому маппим поле цвета на шрифт. */
  val fontColorM = colorM
    .transform(
      { AOFieldFont.apply },
      { mmAdFont: AOFieldFont => mmAdFont.color }
    )

  /** Маппим строковое поле с настройками шрифта. */
  def mmaStringFieldM(m : Mapping[String]) = mapping(
    "value" -> m,
    "color" -> fontColorM
  )
  { AOStringField.apply }
  { AOStringField.unapply }

  /** Маппим числовое (Float) поле. */
  def mmaFloatFieldM(m: Mapping[Float]) = mapping(
    "value" -> m,
    "color" -> fontColorM
  )
  { AOFloatField.apply }
  { AOFloatField.unapply }

  /** Поле с ценой. Является вариацией float-поля. */
  val mmaPriceM = mapping(
    "value" -> priceStrictM,
    "color" -> fontColorM
  )
  {case ((rawPrice, price), font) =>
    AOPriceField(price.price, price.currency.getCurrencyCode, rawPrice, font) }
  {mmadp =>
    import mmadp._
    Some((orig, Price(value, currency)), font)
  }

  /** Поле с необязательной ценой. Является вариацией float-поля. Жуткий говнокод. */
  val mmaPriceOptM = mapping(
    "value" -> optional(priceStrictM),
    "color" -> fontColorM
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
  def mmaFloatFieldOptM(m: Mapping[Float]) = mapping(
    "value" -> optional(m),
    "color" -> fontColorM
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
  def adFormApply(userCatId: Option[String], bd: BlockData): MAd = {
    MAd(
      producerId  = null,
      offers      = bd.offers,
      blockMeta   = bd.blockMeta,
      imgOpt         = null,
      userCatId   = userCatId
    )
  }

  /** Функция разборки для маппинга формы добавления/редактирования рекламной карточки. */
  def adFormUnapply(mmad: MAd): Option[(Option[String], BlockData)] = {
    Some((mmad.userCatId, mmad))
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

