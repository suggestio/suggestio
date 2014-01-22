package io.suggest.ym.model

import com.scaleunlimited.cascading.{BaseDatum, PayloadDatum}
import io.suggest.util.CascadingFieldNamer
import cascading.tuple.{TupleEntry, Tuple, Fields}
import scala.collection.JavaConversions._
import io.suggest.ym.{Dimensions, OfferHandlerState, OfferTypes}, OfferTypes.OfferType
import io.suggest.model.PayloadHelpers
import io.suggest.ym.YmParsers._

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

  override val FIELDS = {
    var fieldsAcc = new Fields(
      URL_FN, ID_FN, OFFER_TYPE_FN, GROUP_ID_FN, AVAILABLE_FN, SHOP_META_FN, SHOP_ID_FN, PRICE_FN, CURRENCY_ID_FN,
      CATEGORY_IDS_FN, MARKET_CATEGORY_FN, PICTURES_FN,
      DESCRIPTION_FN, SALES_NOTES_FN, COUNTRY_OF_ORIGIN, MANUFACTURER_WARRANTY_FN, DOWNLOADABLE_FN, AGE_FN
    )
    fieldsAcc = fieldsAcc append super.FIELDS
    fieldsAcc = fieldsAcc append BaseDatum.getSuperFields(classOf[YmOfferDatum])
    fieldsAcc
  }

  // Payload-поля, хранящиеся в PAYLOAD_FN, т.к. допустимы не для каждого товара или редко используются
  val NAME_PFN                  = fieldName("name")
  val VENDOR_PFN                = fieldName("vendor")
  val VENDOR_CODE_PFN           = fieldName("vendorCode")
  val YEAR_PFN                  = fieldName("year")
  // vendor.model
  val TYPE_PREFIX_PFN           = fieldName("typePrefix")
  val MODEL_PFN                 = fieldName("model")
  val SELLER_WARRANTY_PFN       = fieldName("sellerWarranty")
  val REC_LIST_PFN              = fieldName("rec")
  val WEIGHT_KG_PFN             = fieldName("weight")
  val EXPIRY_PFN                = fieldName("expiry")
  val DIMENSIONS_PFN            = fieldName("dimensions")
  // *book
  val AUTHOR_PFN                = fieldName("author")
  val PUBLISHER_PFN             = fieldName("publisher")
  val SERIES_PFN                = fieldName("series")
  val ISBN_PFN                  = fieldName("isbn")
  val VOLUMES_COUNT_PFN         = fieldName("volumesCounta")
  val VOLUME_PFN                = fieldName("volume")
  val LANGUAGE_PFN              = fieldName("lang")
  val TABLE_OF_CONTENTS_PFN     = fieldName("toc")
  val BINDING_PFN               = fieldName("binding")
  val PAGE_EXTENT_PFN           = fieldName("pageExtent")

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

  def serializeRecommendedIds(rec: Seq[String]) = new Tuple(rec : _*)
  val deserializeRecommendedIds: PartialFunction[AnyRef, Seq[String]] = {
    case null     => Nil
    case t: Tuple => t.toSeq.asInstanceOf[Seq[String]]
  }
}


import YmOfferDatum._

class YmOfferDatum extends PayloadDatum(FIELDS) with OfferHandlerState with YmDatumDeliveryT with PayloadHelpers {
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


  /** Ссылка на коммерческое предложение на сайте магазина. */
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

  /**
   * Для корректного соотнесения всех вариантов товара с карточкой модели необходимо в описании каждого
   * товарного предложения использовать атрибут group_id. Значение атрибута числовое, задается произвольно.
   * Для всех предложений, которые необходимо отнести к одной карточке товара, должно быть указано одинаковое
   * значение атрибута group_id.
   * [[http://partner.market.yandex.ru/legal/clothes/ Цитата взята отсюда]]
   */
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

  /** Цена коммерческого предложения. */
  def price = _tupleEntry getFloat PRICE_FN
  def price_=(price: Float) {
    _tupleEntry.setFloat(PRICE_FN, price)
  }

  /** Валюта цены коммерческого предложения */
  def currencyId = _tupleEntry getString CURRENCY_ID_FN
  def currencyId_=(currencyId: String) {
    _tupleEntry.setString(CURRENCY_ID_FN, currencyId)
  }

  /** Список категорий магазина, который должен быть непустым. */
  def categoryIds = {
    val raw = _tupleEntry getObject CATEGORY_IDS_FN
    deserializeCategoryIds(raw)
  }
  def categoryIds_=(categoryIds: Seq[String]) {
    val t = serializeCategoryIds(categoryIds)
    _tupleEntry.setObject(CATEGORY_IDS_FN, t)
  }

  /** Необязательная категория в общем дереве категорий яндекс-маркета. */
  def marketCategoryOpt = {
    val raw = _tupleEntry getObject MARKET_CATEGORY_FN
    deserializeMarketCategory(raw)
  }
  def marketCategoryOpt_=(mcOpt: Option[Seq[String]]) {
    val t = serializeMarketCategory(mcOpt)
    _tupleEntry.setObject(MARKET_CATEGORY_FN, t)
  }

  /** Ссылки на картинки. Их может и не быть. */
  def pictures: Seq[String] = {
    val raw = _tupleEntry getObject PICTURES_FN
    deserializePictures(raw)
  }
  def pictures_=(pictures: Seq[String]) {
    val t = serializePictures(pictures)
    _tupleEntry.setObject(PICTURES_FN, t)
  }

  /** Описание товара. */
  def descriptionOpt = Option(_tupleEntry getString DESCRIPTION_FN)
  def descriptionOpt_=(descOpt: Option[String]) {
    _tupleEntry.setString(DESCRIPTION_FN, descOpt getOrElse null)
  }

  /** Краткие дополнительные сведения по покупке/доставке. */
  def salesNotesOpt = Option(_tupleEntry getString SALES_NOTES_FN)
  def salesNotesOpt_=(salesNotesOpt: Option[String]) {
    _tupleEntry.setString(SALES_NOTES_FN, salesNotesOpt getOrElse null)
  }

  /** Страна-производитель товара. */
  def countryOfOriginOpt = Option(_tupleEntry getString COUNTRY_OF_ORIGIN)
  def countryOfOriginOpt_=(cooOpt: Option[String]) {
    _tupleEntry.setString(COUNTRY_OF_ORIGIN, cooOpt getOrElse null)
  }

  /** Гарантия производителя: true, false или P1Y2M10DT2H30M. */
  def manufacturerWarranty: Warranty = {
    val raw = _tupleEntry getString MANUFACTURER_WARRANTY_FN
    deserializeManufacturerWarranty(raw)
  }
  def manufacturerWarranty_=(mw: Warranty) {
    val s = serializeManufacturerWarranty(mw)
    _tupleEntry.setString(MANUFACTURER_WARRANTY_FN, s)
  }

  /** Можно ли скачать указанный нематериальный товар? */
  def isDownloadable = _tupleEntry getBoolean DOWNLOADABLE_FN
  def isDownloadable_=(isDownloadable: Boolean) {
    _tupleEntry.setBoolean(DOWNLOADABLE_FN, isDownloadable)
  }

  /** Возрастная категория товара. Есть немного противоречивой документации, но суть в том,
    * что есть аттрибут units и набор допустимых целочисленное значений для указанных единиц измерения. */
  def ageOpt = deserializeAge(_tupleEntry getObject AGE_FN)
  def ageOpt_=(ageOpt: Option[YmOfferAge]) {
    val t = serializeAge(ageOpt)
    _tupleEntry.setObject(AGE_FN, t)
  }


  // =============================================================================================================
  // Payload-поля. Они встречаются не во всех типах офферов или же не часто используются.
  // Геттеры этих полей всегда возвращают Option[T]. Сеттеры принимают на вход T или Option[T] в зависимости от ситуации.
  // Чтобы стереть значение, надо передать null либо None соответственно.
  // Таким образом, для getter'ов используются явные option, для сеттеров - неявные. Это существенно упрощает код.

  /** Используется в офферах кроме vendor.model. По факту это значит в меньшинстве, но где он есть - всегда обязательный. */
  def name = getPayloadString(NAME_PFN)
  def name_=(name: String) = setPayloadValue(NAME_PFN, name)

  /** Производитель. Не отображается в названии предложения. Например: HP. */
  def vendor = getPayloadString(VENDOR_PFN)
  def vendor_=(vendor: String) = setPayloadValue(VENDOR_PFN, vendor)

  /** Код товара по прайсам производителя. (т.е. указывается код производителя). */
  def vendorCode = getPayloadString(VENDOR_CODE_PFN)
  def vendorCode_=(vendorCode: String) = setPayloadValue(VENDOR_CODE_PFN, vendorCode)

  /** Год издания (книги, диска и т.д.). */
  def yearOpt: Option[Int] = getPayloadInt(YEAR_PFN)
  def yearOpt_=(yearOpt: Option[Int]) {
    val v1 = yearOpt getOrElse null
    setPayloadValue(YEAR_PFN, v1)
  }

  /** vendor.model: Предисловие названия, очень короткое и даже необязательное. "Группа товаров / категория". */
  def typePrefix: Option[String] = getPayloadString(TYPE_PREFIX_PFN)
  def typePrefix_=(tp: String) = setPayloadValue(TYPE_PREFIX_PFN, tp)

  /** vendor.model: Модель товара, хотя судя по примерам, там может быть и категория, а сама "модель". "Женская куртка" например. */
  def model = getPayloadString(MODEL_PFN)
  def model_=(model: String) = setPayloadValue(MODEL_PFN, model)

  /** vendor.model: Гарантия от продавца, если есть. Аналогично manufacturer_warranty. */
  def sellerWarrantyOpt = getPayloadString(SELLER_WARRANTY_PFN) map { Warranty(_) }
  def sellerWarrantyOpt_=(sw: Warranty) = setPayloadValue(SELLER_WARRANTY_PFN, sw.raw)

  /** vendor.model: Список id рекомендуемых товаров к этому товару. */
  def recIds = deserializeRecommendedIds(getPayloadValue(REC_LIST_PFN))
  def recIds_=(recIds: Seq[String]) = setPayloadValue(REC_LIST_PFN, serializeRecommendedIds(recIds))

  /** vendor.model: Вес товара с учётом упаковки, в килограммах. */
  def weightKgOpt = getPayloadFloat(WEIGHT_KG_PFN)
  def weightKgOpt_=(weightOpt: Option[Float]) = setPayloadValue(WEIGHT_KG_PFN, weightOpt getOrElse null)

  /** vendor.model: Истечение срока годности: период или же дата. */
  def rawExpiryOpt = getPayloadString(EXPIRY_PFN)
  def rawExpiryOpt_=(rawExpiry: String) = setPayloadValue(EXPIRY_PFN, rawExpiry)
  def expiryOpt = rawExpiryOpt.map(parse(EXPIRY_PARSER, _).get)

  /** vendor.model: Размерности товара: ширина, длина и высота. */
  def rawDimensions = getPayloadString(DIMENSIONS_PFN)
  def rawDimensions_=(rawDims: String) = setPayloadValue(DIMENSIONS_PFN, rawDims)
  def dimensions = rawDimensions.map(parse(DIMENSIONS_PARSER, _).get)
  def dimensions_=(dims: Dimensions) = setPayloadValue(DIMENSIONS_PFN, dims.toRaw)


  /** *book: Автор произведения, если есть. */
  def author = getPayloadString(AUTHOR_PFN)
  def author_=(author: String) = setPayloadValue(AUTHOR_PFN, author)

  /** *book: Издательство. */
  def publisher = getPayloadString(PUBLISHER_PFN)
  def publisher_=(publisher: String) = setPayloadValue(PUBLISHER_PFN, publisher)

  /** *book: Серия. */
  def series = getPayloadString(SERIES_PFN)
  def series_=(series: String) = setPayloadValue(SERIES_PFN, series)

  /** *book: Код ISBN. */
  def isbn = getPayloadString(ISBN_PFN)
  def isbn_=(isbn: String) = setPayloadValue(ISBN_PFN, isbn)

  /** *book: Кол-во томов. */
  def volumesCount = getPayloadInt(VOLUMES_COUNT_PFN)
  def volumesCount_=(vcOpt: Option[Int]) = setPayloadValue(VOLUMES_COUNT_PFN, vcOpt getOrElse null)

  /** *book: Номер тома. */
  def volume = getPayloadInt(VOLUME_PFN)
  def volume_=(v: Option[Int]) = setPayloadValue(VOLUME_PFN, v getOrElse null)

  /** *book: Язык произведения. */
  def language = getPayloadString(LANGUAGE_PFN)
  def language_=(lang: String) = setPayloadValue(LANGUAGE_PFN, lang)

  /** *book: Краткое оглавление. Для сборника рассказов: список рассказов. */
  def tableOfContents = getPayloadString(TABLE_OF_CONTENTS_PFN)
  def tableOfContents_=(toc: String) = setPayloadValue(TABLE_OF_CONTENTS_PFN, toc)


  /** book: Переплёт. */
  def binding = getPayloadString(BINDING_PFN)
  def binding_=(binding: String) = setPayloadValue(BINDING_PFN, binding)

  /** book: Кол-во страниц в книге. */
  def pageExtent = getPayloadInt(PAGE_EXTENT_PFN)
  def pageExtent_=(peOpt: Option[Int]) = setPayloadValue(PAGE_EXTENT_PFN, peOpt getOrElse null)
}
