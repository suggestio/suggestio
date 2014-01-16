package io.suggest.ym

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.01.14 16:38
 * Description: Разная утиль, общая для разных парсеров.
 * Константы и их наборы собраны на основе [[http://partner.market.yandex.ru/pages/help/shops.dtd shops.dtd]].
 */

/** Словарь для работы с данными, подготовленными для отправки в маркет. */
object YmConstants {
  val TAG_YML_CATALOG           = "yml_catalog"
  val TAG_SHOP                  = "shop"

  val TAG_NAME                  = "name"
  val TAG_COMPANY               = "company"
  val TAG_URL                   = "url"
  val TAG_PHONE                 = "phone"
  val TAG_PLATFORM              = "platform"
  val TAG_VERSION               = "version"
  val TAG_AGENCY                = "agency"
  val TAG_EMAIL                 = "email"
  val TAG_CURRENCIES            = "currencies"
  val TAG_CURRENCY              = "currency"
  val TAG_CATEGORIES            = "categories"
  val TAG_CATEGORY              = "category"
  val TAG_STORE                 = "store"
  val TAG_PICKUP                = "pickup"
  val TAG_DELIVERY              = "delivery"
  val TAG_DELIVERY_INCLUDED     = "deliveryIncluded"
  val TAG_LOCAL_DELIVERY_COST   = "local_delivery_cost"
  val TAG_ADULT                 = "adult"
  val TAG_OFFERS                = "offers"
  val TAG_OFFER                 = "offer"

  val ATTR_ID                   = "id"
  val ATTR_RATE                 = "rate"
  val ATTR_PLUS                 = "plus"
  val ATTR_PARENT_ID            = "parentId"
  val ATTR_GROUP_ID             = "group_id"
  val ATTR_TYPE                 = "type"
  val ATTR_AVAILABLE            = "available"
  //val ATTR_BID                = "bid"   // bid (oсновная ставка)

  // Значения для аттрибута type тега offer, описывающего товар в одной из нескольких форм.
  val ATTRV_OFTYPE_VENDOR_MODEL = "vendor.model"
  val ATTRV_OFTYPE_BOOK         = "book"
  val ATTRV_OFTYPE_AUDIOBOOK    = "audiobook"
  val ATTRV_OFTYPE_ARTIST_TITLE = "artist.title"
  val ATTRV_OFTYPE_TOUR         = "tour"
  val ATTRV_OFTYPE_TICKET       = "ticket"
  val ATTRV_OFTYPE_EVENT_TICKET = "event-ticket"
}


object TopLevelFields extends Enumeration {
  type TopLevelFields = Value
  val yml_catalog = Value
}


object YmlCatalogFields extends Enumeration {
  type YmlCatalogFields = Value
  val shop = Value
}


object ShopFields extends Enumeration {
  type ShopFields = Value
  val name, company, url, phone, platform, version, agency, email, currencies, categories, store, pickup,
  delivery, deliveryIncluded, local_delivery_cost, adult, offers = Value

  val mandatoryFields = ValueSet(name, company, url, currencies, categories, offers)
}


object OffersFields extends Enumeration {
  type OffersFields = Value
  val offer = Value
}

object SimpleOfferFields extends Enumeration {
  type SimpleOfferFields = Value
  val url, price, currencyId, categoryId, picture, store, pickup, delivery, local_delivery_cost, name, vendorCode,
  description, sales_notes, manufacturer_warranty, country_of_origin, barcode = Value
}
