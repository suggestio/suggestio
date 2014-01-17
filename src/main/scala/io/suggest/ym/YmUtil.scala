package io.suggest.ym

import io.suggest.ym.OfferAgeUnits.OfferAgeUnit
import org.joda.time.Period
import io.suggest.ym.OfferCategoryIdTypes.OfferCategoryIdType

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.01.14 16:38
 * Description: Разная утиль, общая для разных парсеров.
 * Константы и их наборы собраны на основе [[http://partner.market.yandex.ru/pages/help/shops.dtd shops.dtd]].
 */

/** Словарь для работы с данными, подготовленными для отправки в маркет. */
object YmConstants {
  val TAG_CURRENCY              = "currency"

  val ATTR_ID                   = "id"
  val ATTR_RATE                 = "rate"
  val ATTR_PLUS                 = "plus"
  val ATTR_PARENT_ID            = "parentId"                // TODO Сделать toLowerCase?
  val ATTR_GROUP_ID             = "group_id"
  val ATTR_TYPE                 = "type"
  val ATTR_AVAILABLE            = "available"
  val ATTR_UNIT                 = "unit"
  //val ATTR_BID                = "bid"   // oсновная ставка товара для выборки по маркету.
}


object TopLevelFields extends Enumeration {
  type TopLevelField = Value
  val yml_catalog = Value
}


object YmlCatalogFields extends Enumeration {
  type YmlCatalogField = Value
  val shop = Value
}


object ShopFields extends Enumeration {
  type ShopFields = Value
  val name, company, url, phone, platform, version, agency, email, currencies, categories, store, pickup,
  delivery, deliveryIncluded, local_delivery_cost, adult, offers = Value

  val mandatoryFields = ValueSet(name, company, url, currencies, categories, offers)
}


object ShopCategoriesFields extends Enumeration {
  type ShopCategoriesField = Value
  val category = Value
}


object OffersFields extends Enumeration {
  type OffersField = Value
  val offer = Value
}

/** Все возможные поля всех офферов. Используется как словарь. Для всех handler'ов.
  * Описания конкретных наборов допустимых полей находятся на уровне конкретных реализаций. */
object AnyOfferFields extends Enumeration {
  type AnyOfferField = Value
  val url, buyurl, price, wprice, currencyId, xCategory, categoryId, market_category,
      picture, store, pickup, delivery, deliveryIncluded, local_delivery_cost, orderingTime,
      aliases, additional, description, sales_notes, promo, manufacturer_warranty,
      country_of_origin, downloadable, adult, age, barcode, param, related_offer,
      // vendor.model
      typePrefix, vendor, vendorCode, model, provider, tarifplan, seller_warranty, cpa, rec, expiry, weight, dimensions,
      // *book
      author, name, publisher, series, year, ISBN, volume, part, language, table_of_contents,
      // book
      binding, page_extent,
      // audiobook
      performed_by, performance_type, storage, format, recording_length,
      // artist.title (без year)
      artist, title, media, starring, director, originalName, country,
      // tour (без name, country)
      worldRegion, region, days, dataTour, hotel_stars, room, meal, included, transport, price_min, price_max, options,
      // ticket (deprecated by event-ticket) и сам event-ticket. Без name.
      place, hall, hall_part, date, is_premiere, is_kids
      = Value

  // TODO orderingTime в ignored потому что синтаксис поля нигде не описан.
  val ignoredFields = ValueSet(
    buyurl, wprice, xCategory, orderingTime, aliases, additional, promo, related_offer, barcode
  )

  /**
   * Является ли указанное поле игнорируемым?
   * @param fn Поле.
   * @return true, если поле есть в списке игнорируемых полей.
   */
  def isIgnoredField(fn: AnyOfferField) = ignoredFields contains fn
}


/**
 * Контейнер для прочитанного поля offer.categoryId.
 * @param categoryId id категории
 * @param idType Устаревший атрибут, описывающий тип задания категории.
 */
case class OfferCategoryId(categoryId: String, idType: OfferCategoryIdType)

/** Экземпляр, описывающий значение поля market category. Нужно ознакомиться с
  * [[http://help.yandex.ru/partnermarket/docs/market_categories.xls таблицей категорий маркета]]
  * @param catPath Путь в дереве категорий. Например List(Книги, Бизнес и экономика, ...)
  */
case class MarketCategory(catPath: List[String])


/**
 * Гарантия производителя на товар.
 * @param hasWarranty Есть ли гарантия вообще?
 * @param warrantyPeriod Срок гарантии. Если гарантии нет, то тут должно быть None.
 */
case class Warranty(
  hasWarranty: Boolean,
  warrantyPeriod: Option[Period] = None
)


object OfferAgeUnits extends Enumeration {
  type OfferAgeUnit = Value
  val year, month = Value
}

case class OfferAge(units: OfferAgeUnit, value: Int)


object OfferParamAttrs extends Enumeration {
  type OfferParamAttr = Value
  val name, unit = Value
}


/** Типы описаний предложений магазинам. SIMPLE используется для null-значения аттрибута типа. */
object OfferTypes extends Enumeration {
  type OfferType = Value
  val SIMPLE, `vendor.model`, book, audiobook, `artist.title`, tour, ticket, `event-ticket` = Value

  def default = SIMPLE
}


/** Тип задания categoryId в рамках предложения. Магазин может использовать свои категории (Own),
  * либо дерево категорий яндекса (Own), или некий Torg (доку найти не удалось).
  * Вроде бы это устаревшая вещь, и оставлена для совместимости. В актуальных случаях, надо использовать разные поля:
  * - categoryId для задания категории в рамках дерева категорий магазина
  * - market_category для задания категории из дерева категорий Yandex.Market.
  */
object OfferCategoryIdTypes extends Enumeration {
  type OfferCategoryIdType = Value
  val Yandex, Torg, Own = Value

  def default = Own
}


/**
 * Распарсенные размеры товара записываются в этот класс.
 * @param lenCm Длина в сантиметрах.
 * @param widthCm Ширина в сантиметрах.
 * @param heightCm Высота в сантиметрах.
 */
case class Dimensions(lenCm:Float, widthCm:Float, heightCm:Float) extends Serializable

