package io.suggest.ym.model

import com.scaleunlimited.cascading.{BaseDatum, PayloadDatum}
import io.suggest.util.CascadingFieldNamer
import cascading.tuple.{TupleEntry, Tuple, Fields}
import scala.collection.JavaConversions._
import io.suggest.ym.OfferTypes, OfferTypes.OfferType

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.14 10:19
 * Description: Датум для хранения распарсенного оффера магазина или поставщика услуг.
 * Каждый оффер содержит информацию о своём магазине и об одном товаре/услуге.
 * Офферы имеют динамическое число полей, поэтому часть полей (в т.ч. все param-поля) живут внутри Payload'а.
 */
object YmOfferDatum extends CascadingFieldNamer with Serializable with YmDatumDeliveryStaticT {

  // Выбираем поля из AnyOfferHandler, которые скорее всего есть в любых офферах.
  /** Ссылка на оффер. Необязательно, это уникальная ссылка. */
  val URL_FN                    = fieldName("url")
  /** Необязательный id товара по мнению магазина. */
  val ID_FN                     = fieldName("id")
  val OFFER_TYPE_FN             = fieldName("offerType")
  val GROUP_ID_FN               = fieldName("groupId")
  val AVAILABLE_FN              = fieldName("available")
  /** Поле с метаданными магазина. Заполняется кортежем из YmShopDatum. */
  val SHOP_META_FN              = fieldName("shopMeta")  // TODO Надо бы хранить это дело отдельно. Это будет более оптимально.
  /** id магазина. Генерируется на основе предыдущего поля. Удобно для группировки. */
  val SHOP_ID_FN                = fieldName("shopId")
  val PRICE_FN                  = fieldName("price")
  val CURRENCY_ID_FN            = fieldName("currencyId")
  val CATEGORY_IDS_FN           = fieldName("categoryId")
  val MARKET_CATEGORY_FN        = fieldName("marketCategory")
  val PICTURES_FN               = fieldName("pictures")
  val STORE_FN                  = fieldName("store")
  val PICKUP_FN                 = fieldName("pickup")
  val DELIVERY_FN               = fieldName("delivery")
  val DELIVERY_INCLUDED_FN      = fieldName("deliveryIncluded")
  val LOCAL_DELIVERY_COST_FN    = fieldName("localDeliveryCost")
  val DESCRIPTION_FN            = fieldName("description")
  val SALES_NOTES_FN            = fieldName("salesNotes")
  val COUNTRY_OF_ORIGIN         = fieldName("countryOfOrigin")
  val MANUFACTURER_WARRANTY_FN  = fieldName("manufacturerWarranty")
  val DOWNLOADABLE_FN           = fieldName("downloadable")
  val ADULT_FN                  = fieldName("adult")
  val AGE_FN                    = fieldName("age")

  val FIELDS = {
    val thisFields = new Fields(
      URL_FN, ID_FN, OFFER_TYPE_FN, GROUP_ID_FN, AVAILABLE_FN, SHOP_META_FN, SHOP_ID_FN, PRICE_FN, CURRENCY_ID_FN,
      CATEGORY_IDS_FN, MARKET_CATEGORY_FN, PICTURES_FN,
      STORE_FN, PICKUP_FN, DELIVERY_FN, DELIVERY_INCLUDED_FN, LOCAL_DELIVERY_COST_FN,
      DESCRIPTION_FN, SALES_NOTES_FN, COUNTRY_OF_ORIGIN, MANUFACTURER_WARRANTY_FN, DOWNLOADABLE_FN, ADULT_FN, AGE_FN
    )
    val superFields = BaseDatum.getSuperFields(classOf[PayloadDatum])
    thisFields append superFields
  }

  def serializeType(t: OfferType) = t.id
  def deserializeType(tid: Int): OfferType = OfferTypes(tid)

  val deserializeShopMeta: PartialFunction[AnyRef, YmShopDatum] = {
    case null     => null
    case t: Tuple => new YmShopDatum(t)
  }
  def serializeShopMeta(ysd: YmShopDatum) = ysd.getTuple


  def serializeCategoryIds(catIds: Seq[String]) = new Tuple(catIds : _*)
  val deserializeCategoryIds: PartialFunction[AnyRef, Seq[String]] = {
    case null     => Nil
    case t: Tuple => t.toSeq.asInstanceOf[Seq[String]]
  }


  def serializeMarketCategory(mcOpt: Option[Seq[String]]) = {
    if (mcOpt.isDefined && !mcOpt.get.isEmpty)
      new Tuple(mcOpt.get : _*)
    else
      null
  }
  val deserializeMarketCategory: PartialFunction[AnyRef, Option[Seq[String]]] = {
    case null     => None
    case t: Tuple => if (t.isEmpty)  None  else  Some(t.toSeq.asInstanceOf[Seq[String]])
  }


  def serializePictures(pictures: Seq[String]) = new Tuple(pictures : _*)
  val deserializePictures: PartialFunction[AnyRef, Seq[String]] = {
    case null     => Nil
    case t: Tuple => t.toSeq.asInstanceOf[Seq[String]]
  }

  def serializeManufacturerWarranty(mw: Warranty): String = mw.raw
  val deserializeManufacturerWarranty: PartialFunction[AnyRef, Warranty] = {
    case null      => NoWarranty
    case s: String => Warranty(s)
  }

  def serializeAge(ageOpt: Option[YmOfferAge]) = {
    if (ageOpt.isDefined)  ageOpt.get.getTuple  else  null
  }
  val deserializeAge: PartialFunction[AnyRef, Option[YmOfferAge]] = {
    case null     => None
    case t: Tuple => Some(new YmOfferAge(t))
  }
}


import YmOfferDatum._

class YmOfferDatum extends PayloadDatum(FIELDS) with YmDatumDeliveryT {
  def companion = YmOfferDatum

  def this(t: Tuple) = {
    this
    setTuple(t)
    getTupleEntry
  }

  def this(te: TupleEntry) = {
    this
    setTupleEntry(te)
  }

  def this(url: String, offerType:OfferType, shop: YmShopDatum) = {
    this
    this.urlOpt = Some(url)
    this.offerType = offerType
    this.shopMeta = shop
  }


  def urlOpt = Option(_tupleEntry getString URL_FN)
  def urlOpt_=(urlOpt: Option[String]) {
    _tupleEntry.setString(URL_FN, urlOpt getOrElse null)
  }

  def idOpt = Option(_tupleEntry getString ID_FN)
  def idOpt_=(id: Option[String]) {
    _tupleEntry.setString(ID_FN, id getOrElse null)
  }

  def offerType: OfferType = deserializeType(_tupleEntry getInteger OFFER_TYPE_FN)
  def offerType_=(ot: OfferType) {
    val i = serializeType(ot)
    _tupleEntry.setInteger(OFFER_TYPE_FN, i)
  }

  def groupIdOpt = Option(_tupleEntry getString GROUP_ID_FN)
  def groupIdOpt_=(groupIdOpt: Option[String]) {
    _tupleEntry.setString(GROUP_ID_FN, groupIdOpt getOrElse null)
  }

  def isAvailable = _tupleEntry getBoolean AVAILABLE_FN
  def isAvailable_=(isAvailable: Boolean) = {
    _tupleEntry.setBoolean(AVAILABLE_FN, isAvailable)
  }

  def shopMeta: YmShopDatum = {
    val raw = _tupleEntry.getObject(SHOP_META_FN)
    deserializeShopMeta(raw)
  }
  def shopMeta_=(ysd: YmShopDatum) {
    val t = serializeShopMeta(ysd)
    _tupleEntry.setObject(SHOP_META_FN, t)
    // TODO Обновлять тут поле shop_id?
  }

  def shopId = _tupleEntry getString SHOP_ID_FN
  def shopId_=(shopId: String) {
    _tupleEntry.setString(SHOP_ID_FN, shopId)
  }

  def price = _tupleEntry getFloat PRICE_FN
  def price_=(price: Float) {
    _tupleEntry.setFloat(PRICE_FN, price)
  }

  def currencyId = _tupleEntry getString CURRENCY_ID_FN
  def currencyId_=(currencyId: String) {
    _tupleEntry.setString(CURRENCY_ID_FN, currencyId)
  }

  def categoryIds = {
    val raw = _tupleEntry getObject CATEGORY_IDS_FN
    deserializeCategoryIds(raw)
  }
  def categoryId_=(categoryIds: Seq[String]) {
    val t = serializeCategoryIds(categoryIds)
    _tupleEntry.setObject(CATEGORY_IDS_FN, t)
  }

  def marketCategoryOpt = {
    val raw = _tupleEntry getObject MARKET_CATEGORY_FN
    deserializeMarketCategory(raw)
  }
  def marketCategoryOpt_=(mcOpt: Option[Seq[String]]) {
    val t = serializeMarketCategory(mcOpt)
    _tupleEntry.setObject(MARKET_CATEGORY_FN, t)
  }

  def pictures: Seq[String] = {
    val raw = _tupleEntry getObject PICTURES_FN
    deserializePictures(raw)
  }
  def pictures_=(pictures: Seq[String]) {
    val t = serializePictures(pictures)
    _tupleEntry.setObject(PICTURES_FN, t)
  }

  def descriptionOpt = Option(_tupleEntry getString DESCRIPTION_FN)
  def descriptionOpt_=(descOpt: Option[String]) {
    _tupleEntry.setString(DESCRIPTION_FN, descOpt getOrElse null)
  }

  def salesNotesOpt = Option(_tupleEntry getString SALES_NOTES_FN)
  def salesNotesOpt_=(salesNotesOpt: Option[String]) {
    _tupleEntry.setString(SALES_NOTES_FN, salesNotesOpt getOrElse null)
  }

  def countryOfOriginOpt = Option(_tupleEntry getString COUNTRY_OF_ORIGIN)
  def countryOfOriginOpt_=(cooOpt: Option[String]) {
    _tupleEntry.setString(COUNTRY_OF_ORIGIN, cooOpt getOrElse null)
  }

  def manufacturerWarranty: Warranty = {
    val raw = _tupleEntry getString MANUFACTURER_WARRANTY_FN
    deserializeManufacturerWarranty(raw)
  }
  def manufacturerWarranty_=(mw: Warranty) {
    val s = serializeManufacturerWarranty(mw)
    _tupleEntry.setString(MANUFACTURER_WARRANTY_FN, s)
  }

  def isDownloadable = _tupleEntry getBoolean DOWNLOADABLE_FN
  def isDownloadable_=(isDownloadable: Boolean) {
    _tupleEntry.setBoolean(DOWNLOADABLE_FN, isDownloadable)
  }

  def ageOpt = deserializeAge(_tupleEntry getObject AGE_FN)
  def ageOpt_=(ageOpt: Option[YmOfferAge]) {
    val t = serializeAge(ageOpt)
    _tupleEntry.setObject(AGE_FN, t)
  }

}
