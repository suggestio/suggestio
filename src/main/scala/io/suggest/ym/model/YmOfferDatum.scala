package io.suggest.ym.model

import com.scaleunlimited.cascading.{Payload, BaseDatum, PayloadDatum}
import io.suggest.util.{MacroLogsImpl, CascadingFieldNamer}
import cascading.tuple.{TupleEntry, Tuple, Fields}
import scala.collection.JavaConversions._
import io.suggest.ym._, OfferTypes.OfferType
import io.suggest.model.PayloadHelpers
import io.suggest.ym.YmParsers._
import org.joda.time.DateTime
import cascading.tuple.coerce.Coercions.{LONG, STRING}
import io.suggest.ym.HotelStarsLevels.HotelStarsLevel
import scala.Some
import io.suggest.ym.Dimensions
import io.suggest.ym.HotelMealTypes.HotelMealType
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.14 10:19
 * Description: Датум для хранения распарсенного оффера магазина или поставщика услуг.
 * Каждый оффер содержит информацию о своём магазине и об одном товаре/услуге.
 * Офферы имеют динамическое число полей, поэтому часть полей (в т.ч. все param-поля) живут внутри Payload'а.
 */
object YmOfferDatum extends CascadingFieldNamer with YmDatumDeliveryStaticT with MacroLogsImpl with Serializable {
  import LOGGER._

  /** Чтобы не дублировать используемый промежуточный тип карты json-объекта, выносим его сюда. */
  type JsFields_t = List[(String, JsValue)]

  // На верхний уровень выносим поля из AnyOfferHandler, которые скорее всего есть в любых офферах.
  // Т.к. бОльшуя часть полей надо будет отправить в elasticsearch, то учитываем это в именовании полей:
  // - *_ESFN - короткое, оригинальное имя поле, под которым будет сохранение в ES и генерация полного имени (*_FN)
  // - *_FN - Полное имя, используемое в именование полей cascading-кортежей.
  // - Payload-ключи: Пока что используем короткие названия полей.

  /** Ссылка на оффер. Необязательно, это уникальная ссылка. */
  val URL_ESFN                  = "url"
  val URL_FN                    = fieldName(URL_ESFN)
  /** Необязательный id товара по мнению магазина. */
  val ID_FN                     = fieldName("id")
  val OFFER_TYPE_ESFN           = "offerType"
  val OFFER_TYPE_FN             = fieldName(OFFER_TYPE_ESFN)
  val GROUP_ID_ESFN             = "groupId"
  val GROUP_ID_FN               = fieldName(GROUP_ID_ESFN)
  val AVAILABLE_ESFN            = "available"
  val AVAILABLE_FN              = fieldName(AVAILABLE_ESFN)
  /** Поле с метаданными магазина. Заполняется кортежем из YmShopDatum. */
  val SHOP_META_FN              = fieldName("shopMeta")  // TODO Надо бы хранить это дело отдельно. Это будет более оптимально.
  /** id магазина. Генерируется на основе предыдущего поля. Удобно для группировки. */
  val SHOP_ID_ESFN              = "shopId"
  val SHOP_ID_FN                = fieldName(SHOP_ID_ESFN)
  val PRICE_ESFN                = "price"
  val PRICE_FN                  = fieldName(PRICE_ESFN)
  val CURRENCY_ID_ESFN          = "currencyId"
  val CURRENCY_ID_FN            = fieldName(CURRENCY_ID_ESFN)
  val CATEGORY_IDS_FN           = fieldName("categoryIds")
  val MARKET_CATEGORY_ESFN      = "marketCategory"
  val MARKET_CATEGORY_FN        = fieldName(MARKET_CATEGORY_ESFN)
  val PICTURES_ESFN             = "pictures"
  val PICTURES_FN               = fieldName(PICTURES_ESFN)
  val STORE_ESFN                = "store"
  val STORE_FN                  = fieldName(STORE_ESFN)
  val PICKUP_ESFN               = "pickup"
  val PICKUP_FN                 = fieldName(PICKUP_ESFN)
  val DELIVERY_ESFN             = "delivery"
  val DELIVERY_FN               = fieldName(DELIVERY_ESFN)
  val DELIVERY_INCLUDED_ESFN    = "deliveryIncluded"
  val DELIVERY_INCLUDED_FN      = fieldName(DELIVERY_INCLUDED_ESFN)
  val LOCAL_DELIVERY_COST_ESFN  = "localDeliveryCost"
  val LOCAL_DELIVERY_COST_FN    = fieldName(LOCAL_DELIVERY_COST_ESFN)
  val DESCRIPTION_ESFN          = "description"
  val DESCRIPTION_FN            = fieldName(DESCRIPTION_ESFN)
  val SALES_NOTES_ESFN          = "salesNotes"
  val SALES_NOTES_FN            = fieldName(SALES_NOTES_ESFN)
  val COUNTRY_OF_ORIGIN_ESFN    = "countryOfOrigin"
  val COUNTRY_OF_ORIGIN_FN      = fieldName(COUNTRY_OF_ORIGIN_ESFN)
  val MANUFACTURER_WARRANTY_ESFN= "manufacturerWarranty"
  val MANUFACTURER_WARRANTY_FN  = fieldName(MANUFACTURER_WARRANTY_ESFN)
  val DOWNLOADABLE_ESFN         = "downloadable"
  val DOWNLOADABLE_FN           = fieldName(DOWNLOADABLE_ESFN)
  val ADULT_ESFN                = "adult"
  val ADULT_FN                  = fieldName(ADULT_ESFN)
  val AGE_ESFN                  = "age"
  val AGE_FN                    = fieldName(AGE_ESFN)


  override val FIELDS = {
    var fieldsAcc = new Fields(
      URL_FN, ID_FN, OFFER_TYPE_FN, GROUP_ID_FN, AVAILABLE_FN, SHOP_META_FN, SHOP_ID_FN, PRICE_FN, CURRENCY_ID_FN,
      CATEGORY_IDS_FN, MARKET_CATEGORY_FN, PICTURES_FN,
      DESCRIPTION_FN, SALES_NOTES_FN, COUNTRY_OF_ORIGIN_FN, MANUFACTURER_WARRANTY_FN, DOWNLOADABLE_FN, AGE_FN
    )
    fieldsAcc = fieldsAcc append super.FIELDS
    fieldsAcc = fieldsAcc append BaseDatum.getSuperFields(classOf[YmOfferDatum])
    fieldsAcc
  }

  // Payload-поля, хранящиеся в PAYLOAD_FN, т.к. допустимы не для каждого товара или редко используются
  val NAME_PFN                  = payloadFieldName("name")
  val VENDOR_PFN                = payloadFieldName("vendor")
  val VENDOR_CODE_PFN           = payloadFieldName("vendorCode")
  val YEAR_PFN                  = payloadFieldName("year")
  // vendor.model
  val TYPE_PREFIX_PFN           = payloadFieldName("typePrefix")
  val MODEL_PFN                 = payloadFieldName("model")
  val SELLER_WARRANTY_PFN       = payloadFieldName("sellerWarranty")
  val REC_LIST_PFN              = payloadFieldName("rec")
  val WEIGHT_KG_PFN             = payloadFieldName("weight")
  val EXPIRY_PFN                = payloadFieldName("expiry")
  val DIMENSIONS_PFN            = payloadFieldName("dimensions")
  // *book
  val AUTHOR_PFN                = payloadFieldName("author")
  val PUBLISHER_PFN             = payloadFieldName("publisher")
  val SERIES_PFN                = payloadFieldName("series")
  val ISBN_PFN                  = payloadFieldName("isbn")
  val VOLUMES_COUNT_PFN         = payloadFieldName("volumesCount")
  val VOLUME_PFN                = payloadFieldName("volume")
  val LANGUAGE_PFN              = payloadFieldName("lang")
  val TABLE_OF_CONTENTS_PFN     = payloadFieldName("toc")
  // book
  val BINDING_PFN               = payloadFieldName("binding")
  val PAGE_EXTENT_PFN           = payloadFieldName("pageExtent")
  // audiobook
  val PERFORMED_BY_PFN          = payloadFieldName("performedBy")
  val PERFORMANCE_TYPE_PFN      = payloadFieldName("performanceType")
  val STORAGE_PFN               = payloadFieldName("storage")
  val FORMAT_PFN                = payloadFieldName("format")
  val RECORDING_LEN_PFN         = payloadFieldName("recordLen")
  // artist.title
  val COUNTRY_PFN               = payloadFieldName("country")
  val ARTIST_PFN                = payloadFieldName("artist")
  val TITLE_PFN                 = payloadFieldName("title")
  val MEDIA_PFN                 = payloadFieldName("media")
  val STARRING_PFN              = payloadFieldName("starring")
  val DIRECTOR_PFN              = payloadFieldName("director")
  val ORIGINAL_NAME_PFN         = payloadFieldName("origName")
  // tour
  val WORLD_REGION_PFN          = payloadFieldName("worldRegion")
  val REGION_PFN                = payloadFieldName("region")
  val DAYS_PFN                  = payloadFieldName("days")
  val TOUR_DATES_PFN            = payloadFieldName("tourDates")
  val HOTEL_STARS_PFN           = payloadFieldName("hotelStars")
  val HOTEL_ROOM_PFN            = payloadFieldName("room")
  val HOTEL_MEAL_PFN            = payloadFieldName("meal")
  val TOUR_INCLUDED_PFN         = payloadFieldName("included")
  val TOUR_TRANSPORT_PFN        = payloadFieldName("transport")
  // event.ticket
  val ET_PLACE_PFN              = payloadFieldName("place")
  val ET_HALL_PFN               = payloadFieldName("hall")
  val ET_HALL_PLAN_URL_PFN      = payloadFieldName("hallPlan")
  val ET_HALL_PART_PFN          = payloadFieldName("hallPart")
  val ET_DATE_PFN               = payloadFieldName("date")
  val ET_IS_PREMIERE_PFN        = payloadFieldName("isPremiere")
  val ET_IS_KIDS_PFN            = payloadFieldName("isKids")

  /** Генератор имён для payload-полей. Имена таких полей сохраняются прямо в payload-кортеж,
    * поэтому имеет смысл их сделать по-короче. */
  def payloadFieldName(fn: String) = fn


  /** Сериализация типа оффера. Используется int id, т.к. тут крайне редко изменяемая сущность. */
  def serializeType(t: OfferType) = t.id
  /** Десериализация id типа оффера, сгенеренного через serializeType(). */
  def deserializeType(tid: Int): OfferType = OfferTypes(tid)


  val deserializeShopMeta: PartialFunction[AnyRef, YmShopDatum] = {
    case null     => null
    case t: Tuple => new YmShopDatum(t)
  }
  def serializeShopMeta(ysd: YmShopDatum) = {
    if (ysd == null)  null  else  ysd.getTuple
  }


  def serializeCategoryIds(catIds: Seq[String]) = {
    if (catIds == null || catIds.isEmpty)  null  else  new Tuple(catIds : _*)
  }
  val deserializeCategoryIds: PartialFunction[AnyRef, Seq[String]] = {
    case null     => Nil
    case t: Tuple => t.toSeq.asInstanceOf[Seq[String]]
  }


  def serializeMarketCategoryPath(mcOpt: Option[Seq[String]]) = {
    if (mcOpt == null || mcOpt.isEmpty || mcOpt.get.isEmpty) {
      null
    } else {
      new Tuple(mcOpt.get : _*)
    }
  }
  val deserializeMarketCategoryPath: PartialFunction[AnyRef, Option[Seq[String]]] = {
    case null     => None
    case t: Tuple => if (t.isEmpty)  None  else  Some(t.toSeq.asInstanceOf[Seq[String]])
  }


  def serializePictures(pictures: Seq[String]) = {
    if (pictures == null || pictures.isEmpty)  null  else  new Tuple(pictures : _*)
  }
  val deserializePictures: PartialFunction[AnyRef, Seq[String]] = {
    case null     => Nil
    case t: Tuple => t.toSeq.asInstanceOf[Seq[String]]
  }


  def serializeManufacturerWarranty(mw: Warranty): String = {
    if (mw == null || !mw.hasWarranty)  null  else  mw.raw
  }
  val deserializeManufacturerWarranty: PartialFunction[AnyRef, Warranty] = {
    case null      => NoWarranty
    case s: String => Warranty(s)
  }


  def serializeAge(ageOpt: Option[YmOfferAge]) = {
    if (ageOpt == null || ageOpt.isEmpty)  null  else  ageOpt.get.getTuple
  }
  val deserializeAge: PartialFunction[AnyRef, Option[YmOfferAge]] = {
    case null     => None
    case t: Tuple => Some(new YmOfferAge(t))
  }


  def serializeRecommendedIds(rec: Seq[String]) = {
    if (rec == null || rec.isEmpty)  null  else  new Tuple(rec : _*)
  }
  val deserializeRecommendedIds: PartialFunction[AnyRef, Seq[String]] = {
    case null     => Nil
    case t: Tuple => t.toSeq.asInstanceOf[Seq[String]]
  }


  def serializeTourDates(tourDates: Seq[DateTime]): Tuple = {
    if (tourDates == null || tourDates.isEmpty) {
      null
    } else {
      val t = new Tuple
      tourDates foreach { td =>
        t addLong td.getMillis
      }
      t
    }
  }
  val deserializeTourDates: PartialFunction[AnyRef, Seq[DateTime]] = {
    case null     => Nil
    case t: Tuple => t.toSeq.map { ts => new DateTime(LONG.coerce(ts)) }
  }


  def serializeHotelStars(hsl: HotelStarsLevel): String = {
    if (hsl == null)  null  else  hsl.toString
  }
  def deserializeHotelStars(hslRaw: AnyRef): HotelStarsLevel = {
    val hslStr = hslRaw match {
      case s: String  => s
      case other      => STRING.coerce(hslRaw)
    }
    HotelStarsLevels.withName(hslStr)
  }


  def serializeHotelRoom(hr: HotelRoomInfo): String = {
    if (hr == null)  null  else  hr.raw
  }
  val deserializeHotelRoom: PartialFunction[AnyRef, HotelRoomInfo] = {
    case s: String => parse(HOTEL_ROOM_PARSER, s).get
  }


  def serializeHotelMeal(m: HotelMealType): String = {
    if (m == null)  null  else  m.toString
  }
  def deserializeHotelMeal(mRaw: AnyRef): HotelMealType = {
    val mStr = mRaw match {
      case s: String  => s
      case other      => STRING.coerce(other)
    }
    parse(HOTEL_MEAL_PARSER, mStr).get
  }


  def serializeDateTime(dt: DateTime): java.lang.Long = dt.getMillis
  val deserializeDateTime: PartialFunction[AnyRef, Option[DateTime]] = {
    case null  => None 
    case other => Some(new DateTime(LONG coerce other))
  }


  /** Булёвы значения в json обычно представлены как null/true. Тут - экземпляр true. */
  private val JS_TRUE = JsBoolean(true)


  /** Перегнать содержимое payload'а в json-заготовку. Полезно при перегонке датума в json для отправки в ES.
    * @param payload Исходное содержимое поля payload.
    * @param acc0 Необязательный исходный аккамулятор.
    * @return Новый аккамулятор, пригодный для оборачивания в JsObject.
    */
  def payload2json(payload: Payload, acc0: JsFields_t = Nil): JsFields_t = {
    var acc1 = acc0
    val it = payload.iterator
    while(it.hasNext) {
      val kv @ (payloadKey, _) = it.next()
      val jsV = payloadV2jsV(kv)
      if (jsV != null) {
        acc1 = payloadKey -> jsV :: acc0
      }
    }
    acc1
  }

  /** Функция конвертации элементов payload в куски json'а. На вход название поля и исходное значение.
    * На выход js-строка или null, если значение не надо сохранять в json-результате. */
  val payloadV2jsV: PartialFunction[(String, AnyRef), JsValue] = {
    val strPfns = Set(
      NAME_PFN, VENDOR_PFN, VENDOR_CODE_PFN,
      /* vendor.model */  TYPE_PREFIX_PFN, MODEL_PFN, SELLER_WARRANTY_PFN, EXPIRY_PFN, DIMENSIONS_PFN,
      /* *book */         AUTHOR_PFN, PUBLISHER_PFN, SERIES_PFN, ISBN_PFN, LANGUAGE_PFN,
      /* book */          TABLE_OF_CONTENTS_PFN, BINDING_PFN,
      /* audiobook */     PERFORMED_BY_PFN, PERFORMANCE_TYPE_PFN, STORAGE_PFN, FORMAT_PFN, RECORDING_LEN_PFN,
      /* artist.title */  COUNTRY_PFN, ARTIST_PFN, TITLE_PFN, MEDIA_PFN, STARRING_PFN, DIRECTOR_PFN, ORIGINAL_NAME_PFN,
      /* tour */          WORLD_REGION_PFN, REGION_PFN, HOTEL_STARS_PFN, HOTEL_ROOM_PFN, HOTEL_MEAL_PFN, TOUR_INCLUDED_PFN, TOUR_TRANSPORT_PFN,
      /* event.ticket */  ET_PLACE_PFN, ET_HALL_PFN, ET_HALL_PLAN_URL_PFN, ET_HALL_PART_PFN
    )
    // Заинлайнить это добро? Использовать Set ради пяти элементов как-то непрактично.
    val intPfns = Set(YEAR_PFN, VOLUMES_COUNT_PFN, VOLUME_PFN, PAGE_EXTENT_PFN, DAYS_PFN)
    import cascading.tuple.coerce.Coercions._
    val resultF: PartialFunction[(String, AnyRef), JsValue] = {
      case (pfn, v) if strPfns contains pfn => JsString(STRING.coerce(v))
      case (pfn, v) if intPfns contains pfn => JsNumber(INTEGER.coerce(v).intValue)
      case (REC_LIST_PFN, v: Tuple)         => JsArray(v.toSeq.map { vo => JsString(STRING.coerce(vo)) })
      case (TOUR_DATES_PFN, v: Tuple)       => JsArray(v.toSeq.map { vl => JsNumber(LONG.coerce(vl).longValue)})
      case (ET_DATE_PFN, v)                 => JsNumber(LONG.coerce(v).longValue)
      case (k, v) =>
        warn(s"payloadV2jsV(): Skipping entry: $k -> $v")
        null
    }
    resultF
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
    this.url = url
    this.offerType = offerType
    this.shopMeta = shop
  }


  /** Ссылка на коммерческое предложение на сайте магазина. */
  def url = Option(_tupleEntry getString URL_FN)
  def url_=(url: String) {
    _tupleEntry.setString(URL_FN, url)
  }

  def idOpt = Option(_tupleEntry getString ID_FN)
  def idOpt_=(id: Option[String]) {
    _tupleEntry.setString(ID_FN, id getOrElse null)
  }

  def offerTypeRaw = _tupleEntry getInteger OFFER_TYPE_FN
  def offerTypeRaw_=(otId: Int) = _tupleEntry.setInteger(OFFER_TYPE_FN, otId)
  def offerType: OfferType = deserializeType(offerTypeRaw)
  def offerType_=(ot: OfferType) {
    offerTypeRaw = serializeType(ot)
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

  /** id магазина генерится силами RDBMS, а тут его целый id. */
  def shopId: Int = _tupleEntry getInteger SHOP_ID_FN
  def shopId_=(shopId: Int) =_tupleEntry.setInteger(SHOP_ID_FN, shopId)

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
  def marketCategoryPath = {
    val raw = _tupleEntry getObject MARKET_CATEGORY_FN
    deserializeMarketCategoryPath(raw)
  }
  def marketCategoryPath_=(mcOpt: Option[Seq[String]]) {
    val t = serializeMarketCategoryPath(mcOpt)
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
  def description = Option(_tupleEntry getString DESCRIPTION_FN)
  def description_=(desc: String) {
    _tupleEntry.setString(DESCRIPTION_FN, desc)
  }

  /** Краткие дополнительные сведения по покупке/доставке. */
  def salesNotes = Option(_tupleEntry getString SALES_NOTES_FN)
  def salesNotes_=(salesNotes: String) {
    _tupleEntry.setString(SALES_NOTES_FN, salesNotes)
  }

  /** Страна-производитель товара. */
  def countryOfOrigin = Option(_tupleEntry getString COUNTRY_OF_ORIGIN_FN)
  def countryOfOrigin_=(coo: String) {
    _tupleEntry.setString(COUNTRY_OF_ORIGIN_FN, coo)
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
  def expiryOptRaw = getPayloadString(EXPIRY_PFN)
  def expiryOptRaw_=(rawExpiry: String) = setPayloadValue(EXPIRY_PFN, rawExpiry)
  def expiryOpt = expiryOptRaw.map(parse(EXPIRY_PARSER, _).get)

  /** vendor.model: Размерности товара: ширина, длина и высота. */
  def dimensionsRaw = getPayloadString(DIMENSIONS_PFN)
  def dimensionsRaw_=(rawDims: String) = setPayloadValue(DIMENSIONS_PFN, rawDims)
  def dimensions = dimensionsRaw.map(parse(DIMENSIONS_PARSER, _).get)
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


  /** audiobook: Исполнитель. Если их несколько, перечисляются через запятую. */
  def performedBy = getPayloadString(PERFORMED_BY_PFN)
  def performedBy_=(by: String) = setPayloadValue(PERFORMED_BY_PFN, by)

  /** audiobook: Тип аудиокниги (радиоспектакль, произведение начитано, ...). */
  def performanceType = getPayloadString(PERFORMANCE_TYPE_PFN)
  def performanceType_=(pt: String) = setPayloadValue(PERFORMANCE_TYPE_PFN, pt)

  /** audiobook: Носитель, на котором поставляется аудиокнига. */
  def storage = getPayloadString(STORAGE_PFN)
  def storage_=(storage: String) = setPayloadValue(STORAGE_PFN, storage)

  /** audiobook: Формат аудиокниги. */
  def format = getPayloadString(FORMAT_PFN)
  def format_=(fmt: String) = setPayloadValue(FORMAT_PFN, fmt)

  /** audiobook: Время звучания задается в формате mm.ss (минуты.секунды). */
  def recordingLen = recordingLenRaw.map { parse(RECORDING_LEN_PARSER, _).get }
  def recordingLenRaw = getPayloadString(RECORDING_LEN_PFN)
  def recordingLenRaw_=(rawRl: String) = setPayloadValue(RECORDING_LEN_PFN, rawRl)


  /** artist.title, tour: Страна, с которой связан сий товар. */
  def country = getPayloadString(COUNTRY_PFN)
  def country_=(c: String) = setPayloadValue(COUNTRY_PFN, c)

  /** artist.title: Исполнитель, если есть. */
  def artist = getPayloadString(ARTIST_PFN)
  def artist_=(artist: String) = setPayloadValue(ARTIST_PFN, artist)

  /** artist.title: Название альбома/кина/etc. */
  def title = getPayloadString(TITLE_PFN)
  def title_=(title: String) = setPayloadValue(TITLE_PFN, title)

  /** artist.title: Носитель, на котором записан альбом или распространяется кино. */
  def media = getPayloadString(MEDIA_PFN)
  def media_=(m: String) = setPayloadValue(MEDIA_PFN, m)

  /** artist.title: Актёры или исполнители. */
  def starring = getPayloadString(STARRING_PFN)
  def starring_=(s: String) = setPayloadValue(STARRING_PFN, s)

  /** artist.title: Режиссер. */
  def director = getPayloadString(DIRECTOR_PFN)
  def director_=(d: String) = setPayloadValue(DIRECTOR_PFN, d)

  /** artist.title: Оригинальное название. */
  def originalName = getPayloadString(ORIGINAL_NAME_PFN)
  def originalName_=(on: String) = setPayloadValue(ORIGINAL_NAME_PFN, on)


  /** tour: Регион мира (часть света, материк планеты и т.д.), к которому относится путёвка. "Африка" например. */
  def worldRegion = getPayloadString(WORLD_REGION_PFN)
  def worldRegion_=(wr: String) = setPayloadValue(WORLD_REGION_PFN, wr)

  /** tour: Курорт и город */
  def region = getPayloadString(REGION_PFN)
  def region_=(r: String) = setPayloadValue(REGION_PFN, r)

  /** tour: Количество дней тура. */
  def days = getPayloadInt(DAYS_PFN)
  def days_=(daysOpt: Option[Int]) = setPayloadValue(DAYS_PFN, daysOpt getOrElse null)

  /** tour: Даты заездов в прямом (исходном) порядке. */
  def tourDatesRaw = getPayloadValue(TOUR_DATES_PFN)
  def tourDatesRaw_=(t: Tuple) = setPayloadValue(TOUR_DATES_PFN, t)
  def tourDates = deserializeTourDates(tourDatesRaw)
  def tourDates_=(tds: Seq[DateTime]) { tourDatesRaw = serializeTourDates(tds) }

  /** tour: Звёзды гостиницы. Базовый формат взят [[http://www.uatourist.com/docs/info.htm отсюда]] и немного расширен. */
  def hotelStarsRaw = getPayloadString(HOTEL_STARS_PFN)
  def hotelStarsRaw_=(hslRaw: String) = setPayloadValue(HOTEL_STARS_PFN, hslRaw)
  def hotelStars = hotelStarsRaw map deserializeHotelStars
  def hotelStars_=(hsl: HotelStarsLevel) { hotelStarsRaw = serializeHotelStars(hsl) }

  /** tour: Тип комнаты в гостинице (SGL, DBL+1Chld, ...) */
  def hotelRoomRaw = getPayloadString(HOTEL_ROOM_PFN)
  def hotelRoomRaw_=(hrRaw: String) = setPayloadValue(HOTEL_ROOM_PFN, hrRaw)
  def hotelRoom = hotelRoomRaw map deserializeHotelRoom
  def hotelRoom_=(hr: HotelRoomInfo) { hotelRoomRaw = serializeHotelRoom(hr) }

  /** tour: Тип питания в гостинице. */
  def hotelMealRaw = getPayloadString(HOTEL_MEAL_PFN)
  def hotelMealRaw_=(hmRaw: String) = setPayloadValue(HOTEL_MEAL_PFN, hmRaw)
  def hotelMeal = hotelMealRaw map deserializeHotelMeal
  def hotelMeal_=(hm: HotelMealType) { hotelMealRaw = serializeHotelMeal(hm) }
  
  /** tour: Что включено в стоимость тура. По спекам яндекса, это -- обязательное значение. */
  def tourIncluded = getPayloadString(TOUR_INCLUDED_PFN)
  def tourIncluded_=(ti: String) = setPayloadValue(TOUR_INCLUDED_PFN, ti)

  /** tour: Транспорт. Обязательно. */
  def tourTransport = getPayloadString(TOUR_TRANSPORT_PFN)
  def tourTransport_=(tt: String) = setPayloadValue(TOUR_TRANSPORT_PFN, tt)


  /** event.ticket: Место проведения мероприятия. */
  def etPlace = getPayloadString(ET_PLACE_PFN)
  def etPlace_=(place: String) = setPayloadValue(ET_PLACE_PFN, place)

  /** event.ticket: Название зала и ссылка на изображение с планом зала. */
  def etHall = getPayloadString(ET_HALL_PFN)
  def etHall_=(hall: String) = setPayloadValue(ET_HALL_PFN, hall)

  /** event.ticket: Ссылка на изображение с планом зала. */
  def etHallPlanUrl = getPayloadString(ET_HALL_PLAN_URL_PFN)
  def etHallPlanUrl_=(hallPlanUrl: String) = setPayloadValue(ET_HALL_PLAN_URL_PFN, hallPlanUrl)

  /** event.ticket: hall_part - К какой части зала относится этот билет. */
  def etHallPart = getPayloadString(ET_HALL_PART_PFN)
  def etHallPart_=(hallPart: String) = setPayloadValue(ET_HALL_PART_PFN, hallPart)

  /** event.ticket: Дата сеанса/мероприятия/события/перфоманса. */
  def etDateRaw = getPayloadJLong(ET_DATE_PFN)
  def etDateRaw_=(millis: java.lang.Long) = setPayloadValue(ET_DATE_PFN, millis)
  def etDate = etDateRaw flatMap deserializeDateTime
  def etDate_=(dt: DateTime) { etDateRaw = serializeDateTime(dt) }

  /** event.ticket: Признак премьерности мероприятия. */
  def etIsPremiere = getPayloadBoolean(ET_IS_PREMIERE_PFN)
  def etIsPremiere_=(isPremiere: Boolean) = setPayloadValue(ET_IS_PREMIERE_PFN, isPremiere)

  /** event.ticket: Признак детского мероприятия. */
  def etIsKids = getPayloadBoolean(ET_IS_KIDS_PFN)
  def etIsKids_=(isKids: Boolean) = setPayloadValue(ET_IS_KIDS_PFN, isKids)


  /** Потребности сохранения в ES покрываются поддержкой генерации es-документов прямо в этом классе. */
  def toJsonFields: JsFields_t = {
    var acc: JsFields_t = Nil
    // url
    val urlOpt = url
    if (urlOpt.isDefined)
      acc ::= URL_ESFN -> JsString(urlOpt.get)
    // offerType
    acc ::= OFFER_TYPE_ESFN -> JsNumber(offerTypeRaw)
    // groupId
    val groupIdOpt = this.groupIdOpt
    if (groupIdOpt.isDefined)
      acc ::= GROUP_ID_ESFN -> JsString(groupIdOpt.get)
    // available, shopId, price, currencyId
    acc = AVAILABLE_ESFN -> JsBoolean(isAvailable) ::
      SHOP_ID_ESFN -> JsNumber(shopId) ::
      PRICE_ESFN   -> JsNumber(price) ::
      CURRENCY_ID_ESFN -> JsString(currencyId) ::   // TODO Нужно какой-то гарантированно нормализованный id, а не магазинный.
      acc
    // market_category
    val mcOpt = marketCategoryPath
    if (mcOpt.isDefined)
      acc ::= MARKET_CATEGORY_ESFN -> JsString(mcOpt.get.mkString("/"))
    // pictures
    val picts = pictures
    if (!picts.isEmpty)
      acc ::= PICTURES_ESFN -> JsArray(picts map JsString)
    // store, pickup, delivery, deliveryIncluded
    if (isStore)
      acc ::= STORE_ESFN -> JS_TRUE
    if (isPickup)
      acc ::= PICKUP_ESFN -> JS_TRUE
    if (isDelivery)
      acc ::= DELIVERY_ESFN -> JS_TRUE
    if (isDeliveryIncluded)
      acc ::= DELIVERY_INCLUDED_ESFN -> JS_TRUE
    // localDeliveryCost
    val ldcOpt = localDeliveryCostOpt
    if (ldcOpt.isDefined)
      acc ::= LOCAL_DELIVERY_COST_ESFN -> JsNumber(ldcOpt.get)
    // description
    val descr = description
    if (description.isDefined)
      acc ::= DESCRIPTION_ESFN -> JsString(descr.get)
    // sales_notes
    val snOpt = salesNotes
    if (snOpt.isDefined)
      acc ::= SALES_NOTES_ESFN -> JsString(snOpt.get)
    // country of origin
    val cofOpt = countryOfOrigin
    if (cofOpt.isDefined)
      acc ::= COUNTRY_OF_ORIGIN_ESFN -> JsString(cofOpt.get)
    // manufacturer warranty
    val mw = manufacturerWarranty
    if (mw.hasWarranty)
      acc ::= MANUFACTURER_WARRANTY_ESFN -> JsString(mw.raw)
    // downloadable
    val isDl = isDownloadable
    if (isDl)
      acc ::= DOWNLOADABLE_ESFN -> JS_TRUE
    // adult
    if (isAdult)
      acc ::= ADULT_ESFN -> JS_TRUE
    // age
    val age = this.ageOpt
    if (age.isDefined)
      acc ::= AGE_ESFN -> JsString(age.get.toString)
    // Основные поля в аккамуляторе. Теперь пора запилить payload. Там всё проще: берешь и заливаешь.
    acc = payload2json(getPayload, acc)
    acc
  }

  def toJson = JsObject(toJsonFields)
}

