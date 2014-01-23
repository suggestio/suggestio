package io.suggest.ym

import io.suggest.ym.OfferCategoryIdTypes.OfferCategoryIdType
import io.suggest.ym.HotelRoomTypes.HotelRoomType
import io.suggest.sax.EmptyAttributes

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
  val ATTR_DATE                 = "date"
  val ATTR_PLAN                 = "plan"
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


object OfferAgeUnits extends Enumeration {
  type OfferAgeUnit = Value
  // Порядок менять нельзя, удалять нельзя.
  val Year, Month = Value
}


object OfferParamAttrs extends Enumeration {
  type OfferParamAttr = Value
  val name, unit = Value
}


/** Типы описаний предложений магазинам. SIMPLE используется для null-значения аттрибута типа. */
object OfferTypes extends Enumeration {
  type OfferType = Value

  // /!\ Порядок менять нельзя! Можно только добавлять элементы в конец или немного обновлять эти.
  val Simple        = Value
  val VendorModel   = Value("vendor.model")
  val Book          = Value("book")
  val AudioBook     = Value("audiobook")
  val ArtistTitle   = Value("artist.title")
  val Tour          = Value("tour")
  val EventTicket   = Value("event-ticket")
  // /!\ Порядок менять нельзя! Можно только добавлять элементы в конец или немного обновлять эти.

  def default = Simple
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
case class Dimensions(lenCm:Float, widthCm:Float, heightCm:Float) extends Serializable {
  /** Для сериализации, можно использовать исходный формат. */
  def toRaw = s"$lenCm/$widthCm/$heightCm"
}


/** Тип гостиничной пещеры. Надо по-лучше разобраться в этом.
 * [[http://www.uatourist.com/docs/info.htm Нормальный словарь терминов]].
 */
object HotelRoomTypes extends Enumeration {
  type HotelRoomType = Value
  val SGL, DBL, TWN, TRPL, QDPL = Value
}

case class HotelRoomInfo(roomType: HotelRoomType, childrenCnt:Int = 0, exBedCnt:Int = 0) extends Serializable {
  def raw: String = {
    val sb = new StringBuilder(12, roomType.toString)
    if (childrenCnt > 0) {
      sb.append('-').append(childrenCnt).append("Child")
    }
    if (exBedCnt > 0) {
      sb.append('-').append(exBedCnt).append("ExB")
    }
    sb.toString()
  }
}


/**
 * Типы питания в гостиницах.
 * [[http://www.spikatour-egypt.ru/pitanie_egypt.php Жрачка в египетских гостиницах.]]
 * [[http://www.uatourist.com/docs/info.htm Обозначения жратвы по мнению салоедов.]]
 */
object HotelMealTypes extends Enumeration {
  type HotelMealType = Value
  val OB, Menu, HB, ExHB, BB, FB, ExFB, MiniAI, AI, UAI, HCAL = Value
}


/** Звездатость отеля. HV1 и 2 - это некие коттеджи. */
object HotelStarsLevels extends Enumeration {
  type HotelStarsLevel = Value
  // !!! ПЕРЕИМЕНОВЫВАТЬ СУЩЕСТВУЮЩИЕ УРОВНИ НЕЛЬЗЯ !!!
  val S1, S2, S3, S4, S5, HV1, HV2 = Value

  val forStarCount: PartialFunction[Int, HotelStarsLevel] = {
    case 1 => S1
    case 2 => S2
    case 3 => S3
    case 4 => S4
    case 5 => S5
  }

  val forHvLevel: PartialFunction[Int, HotelStarsLevel] = {
    case 1 => HV1
    case 2 => HV2
  }
}


/** Неизменяемый объект, описывающий пустые аттрибуты. Полезен как заглушка для пустых аттрибутов. */
case object EmptyAttrs extends EmptyAttributes


object ShopCurrency {
  val RATE_DFLT = "1.0"
  val PLUS_DFLT = "0.0"
}


/** Параметр предложения, специфичный для конкретной категории предложений. Потом надо будет сконвертить в трейт.
  * @param name Имя параметра.
  * @param unitRawOpt Единицы измерения значения, сырые, если заданы.
  * @param rawValue Сырое значение параметра в виде строки.
  */
case class OfferParam(
  name: String,
  unitRawOpt: Option[String],
  rawValue: String
) extends Serializable


