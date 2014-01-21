package io.suggest.ym

import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.{Locator, Attributes}
import cascading.tuple.TupleEntryCollector
import io.suggest.sax.SaxContentHandlerWrapper
import YmConstants._
import io.suggest.util.UrlUtil
import io.suggest.ym.AnyOfferFields.AnyOfferField
import io.suggest.util.MyConfig.CONFIG
import io.suggest.ym.OfferTypes.OfferType
import YmParsers._
import org.joda.time._
import io.suggest.ym.HotelMealTypes.HotelMealType
import io.suggest.ym.HotelStarsLevels.HotelStarsLevel
import java.net.URL

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.01.14 14:49
 * Description: SAX-обработчик для Yandex Market XML (YML).
 * Обработчик работает в поточном режиме с минимальным потреблением RAM, поэтому в конструктор требуется передавать
 * коллектор результатов. Из ограничений такого подхода: offers должны идти самым последним тегом в теле тега shop.
 *
 * Парсер реализован в виде конечного автомата со стеком состояний. Наращивание стека и переключение состояний
 * происходит по мере погружения в дерево.
 *
 * $ - [[http://help.yandex.ru/partnermarket/yml/about-yml.xml О формате YML]]
 * $ - [[http://help.yandex.ru/partnermarket/export/date-format.xml Форматы дат]]
 */

object YmlSax extends Serializable {
  
  /** Регэксп токенизации пути в общем дереве категорий маркета. */
  protected val MARKET_CATEGORY_PATH_SPLIT_RE = "\\s*/\\s*".r

  val ELLIPSIS_DFLT = "…"

  /** Максимальная длина строки в поле sales_notes. */
  val SALES_NOTES_MAXLEN = CONFIG.getInt("ym.sax.offer.sales_notes.len.max") getOrElse 50

  /** Используемое многоточие для укорачия sales_notes. */
  val SALES_NOTES_ELLIPSIS = CONFIG.getString("ym.sax.offer.sales_notes.ellipsis") getOrElse ELLIPSIS_DFLT

  /** Возможные значения поля shop.offer.age при unit=year. */
  val OFFER_AGE_YEAR_VALUES   = List(0, 6, 12, 18, 18)

  /** Возможные значения поля shop.offer.age при unit=month. */
  val OFFER_AGE_MONTH_VALUES  = 0 to 12

  // Параметры считывания штрихкодов. На деле - не используется, ибо хз зачем нужны штрихкоды.
  val MAX_BARCODE_LEN = CONFIG.getInt("ym.sax.offer.barcode.len.max") getOrElse 20
  val MAX_BARCODES_COUNT = CONFIG.getInt("ym.sax.offer.barcode.count.max") getOrElse 3

  val DESCRIPTION_LEN_MAX  = CONFIG.getInt("ym.sax.offer.description.len.max") getOrElse 4096
  val DESCRIPTION_ELLIPSIS = CONFIG.getString("ym.sax.offer.description.ellipsis") getOrElse ELLIPSIS_DFLT

  val URL_MAXLEN = CONFIG.getInt("ym.sax.url.len.max") getOrElse 512
  val SHOP_URL_MAXLEN = CONFIG.getInt("ym.sax.shop.url.len.max") getOrElse 128
  val OFFER_URL_MAXLEN = CONFIG.getInt("ym.sax.offer.url.len.max") getOrElse URL_MAXLEN
  val OFFER_PICTURE_URL_MAXLEN = CONFIG.getInt("ym.sax.offer.picture.len.max") getOrElse URL_MAXLEN

  val SHOP_CAT_VALUE_MAXLEN = CONFIG.getInt("ym.sax.shop.category.name.len.max") getOrElse 48
  val SHOP_CAT_COUNT_MAX    = CONFIG.getInt("ym.sax.shop.categories.count.max") getOrElse 2048
  val SHOP_CAT_ID_LEN_MAX   = CONFIG.getInt("ym.sax.shop.category.id.len.max") getOrElse SHOP_CAT_VALUE_MAXLEN
  val MARKET_CAT_VALUE_MAXLEN = CONFIG.getInt("ym.sax.offer.market_category.len.max") getOrElse 256

  val STRING_MAXLEN = 8192
  val BOOLEAN_MAXLEN = CONFIG.getInt("ym.sax.boolean.len.max") getOrElse 16
  val INT_MAXLEN = CONFIG.getInt("ym.sax.int.len.max") getOrElse 16
  val FLOAT_MAXLEN = CONFIG.getInt("ym.sax.float.len.max") getOrElse 25
  val PHONE_MAXLEN = CONFIG.getInt("ym.sax.phone.len.max") getOrElse 18
  val EMAIL_MAXLEN = CONFIG.getInt("ym.sax.email.len.max") getOrElse 128
  val SHOP_NAME_MAXLEN = CONFIG.getInt("ym.sax.shop.name.len.max") getOrElse 64
  val SHOP_COMPANY_MAXLEN = CONFIG.getInt("ym.sax.shop.company.len.max") getOrElse SHOP_NAME_MAXLEN

  val SHOP_CURRENCIES_COUNT_MAX = CONFIG.getInt("ym.sax.shop.currencies.count.max") getOrElse 32
  val SHOP_CURRENCY_ID_MAXLEN = CONFIG.getInt("ym.sax.shop.currency.id.len.max") getOrElse 32

  val OFFER_WARRANTY_MAXLEN = CONFIG.getInt("ym.sax.offer.warranty.len.max") getOrElse 32
  val COUNTRY_MAXLEN = CONFIG.getInt("ym.sax.country.len.max") getOrElse 32

  val VENDOR_MAXLEN = CONFIG.getInt("ym.sax.vendor.len.max") getOrElse SHOP_COMPANY_MAXLEN
  val VENDOR_CODE_MAXLEN = CONFIG.getInt("ym.sax.vendor_code.len.max") getOrElse 32

  val OFFER_NAME_MAXLEN = CONFIG.getInt("ym.sax.offer.name.len.max") getOrElse 128
  val OFFER_PARAM_MAXLEN = CONFIG.getInt("ym.sax.offer.param.len.max") getOrElse 128
  val OFFER_PARAMS_COUNT_MAX = CONFIG.getInt("ym.sax.offer.params.count.max") getOrElse 16
  val OFFER_MODEL_MAXLEN = CONFIG.getInt("ym.sax.offer.model.len.max") getOrElse 40

  val OFFER_TYPE_PREFIX_MAXLEN = CONFIG.getInt("ym.sax.offer.type_prefix.len.max") getOrElse 128
  
  val REC_LIST_MAX_CHARLEN = CONFIG.getInt("ym.sax.offer.rec.charlen.max") getOrElse 128
  val REC_LIST_MAX_LEN     = CONFIG.getInt("ym.sax.offer.rec.len.max") getOrElse 10
  val REC_LIST_SPLIT_RE = "\\s*,\\s*".r

  /** Регэксп для ключа offer.seller_warranty. В доках этот ключ написан с разными орф.ошибками,
    * поэтому его надо матчить по регэкспу. */
  val SELLER_WARRANTY_RE = "sell?er_[wv]arr?anty".r

  val OFFER_DIMENSIONS_MAXLEN = CONFIG.getInt("ym.sax.offer.dimensions.len.max") getOrElse 64

  // book
  val OFFER_AUTHOR_MAXLEN     = CONFIG.getInt("ym.sax.offer.author.len.max") getOrElse 128
  val OFFER_PUBLISHER_MAXLEN  = CONFIG.getInt("ym.sax.offer.publisher.len.max") getOrElse 40
  val OFFER_SERIES_MAXLEN     = CONFIG.getInt("ym.sax.offer.series.len.max") getOrElse OFFER_NAME_MAXLEN
  val OFFER_ISBN_MAXLEN       = CONFIG.getInt("ym.sax.offer.isbn.len.max") getOrElse 20
  val OFFER_LANGUAGE_MAXLEN   = CONFIG.getInt("ym.sax.offer.language.len.max") getOrElse 30
  val OFFER_TOC_MAXLEN        = CONFIG.getInt("ym.sax.offer.tableOfContents.len.max") getOrElse 4096
  val OFFER_BINDING_MAXLEN    = CONFIG.getInt("ym.sax.offer.binding.len.max") getOrElse 32
  val OFFER_FORMAT_MAXLEN     = CONFIG.getInt("ym.sax.offer.format.len.max") getOrElse 32

  // audiobook
  val OFFER_PERFORMED_BY_MAXLEN = CONFIG.getInt("ym.sax.offer.performedBy.len.max") getOrElse 64
  val OFFER_PERFORMANCE_TYPE_MAXLEN = CONFIG.getInt("ym.sax.offer.performanceType.len.max") getOrElse 32
  val OFFER_STORAGE_MAXLEN = CONFIG.getInt("ym.sax.offer.storage.len.max") getOrElse 20
  val OFFER_REC_LEN_MAXLEN = CONFIG.getInt("ym.sax.offer.recordingLen.len.max") getOrElse 8

  // artist.title
  val OFFER_ARTIST_MAXLEN   = CONFIG.getInt("ym.sax.offer.artist.len.max")    getOrElse OFFER_NAME_MAXLEN
  val OFFER_TITLE_MAXLEN    = CONFIG.getInt("ym.sax.offer.title.len.max")     getOrElse OFFER_NAME_MAXLEN
  val OFFER_MEDIA_MAXLEN    = CONFIG.getInt("ym.sax.offer.media.len.max")     getOrElse OFFER_STORAGE_MAXLEN
  val OFFER_STARRING_MAXLEN = CONFIG.getInt("ym.sax.offer.starring.len.max")  getOrElse 1024
  val OFFER_DIRECTOR_MAXLEN = CONFIG.getInt("ym.sax.offer.director.len.max")  getOrElse 64

  // tour
  val WORLD_REGION_MAXLEN   = CONFIG.getInt("ym.sax.offer.worldRegion.len.max") getOrElse 40
  val REGION_MAXLEN         = CONFIG.getInt("ym.sax.offer.region.len.max")      getOrElse WORLD_REGION_MAXLEN
  val HOTEL_STARS_MAXLEN    = CONFIG.getInt("ym.sax.offer.hotelStars.len.max")  getOrElse 10
  val ROOM_TYPE_MAXLEN      = CONFIG.getInt("ym.sax.offer.room.len.max")        getOrElse 20
  val MEAL_MAXLEN           = CONFIG.getInt("ym.sax.offer.meal.len.max")        getOrElse 32
  val TOUR_INCLUDED_MAXLEN  = CONFIG.getInt("ym.sax.offer.included.len.max")    getOrElse 2048
  val TRANSPORT_MAXLEN      = CONFIG.getInt("ym.sax.offer.transport.len.max")   getOrElse 128

  // event-ticket
  val PLACE_MAXLEN          = CONFIG.getInt("ym.sax.offer.place.len.max")       getOrElse 512
  val HALL_MAXLEN           = CONFIG.getInt("ym.sax.offer.hall.len.max")        getOrElse 128
  val HALL_PART_MAXLEN      = CONFIG.getInt("ym.sax.offer.hall_part.len.max")   getOrElse 64

  val DATE_TIME_MAXLEN      = CONFIG.getInt("ym.sax.offer.dt.len.max")          getOrElse 40

  /** Регэксп, описывающий повторяющиеся пробелы. */
  private val MANY_SPACES_RE = "[ \\t]{2,}".r
}


import YmlSax._

class YmlSax(outputCollector: TupleEntryCollector) extends DefaultHandler with SaxContentHandlerWrapper {

  /** Дата старта парсера. Используется для определения длительности работы и для определения каких-либо параметров
    * настоящего времени во вспомогательных парсерах, связанных с датами и временем. */
  val startedAt = DateTime.now()

  /** Экземпляр локатора, который позволяет определить координаты парсера в документе. */
  implicit var locator: Locator = null

  /** Стопка, которая удлиняется по мере погружения в XML теги. Текущий хэндлер наверху. */
  private var handlersStack: List[MyHandler] = Nil

  /** Вернуть текущий ContentHandler. */
  def contentHandler = handlersStack.head

  /** Начало работы с документом. Нужно выставить дефолтовый обработчик в стек. */
  override def startDocument() {
    handlersStack ::= TopLevelHandler()
  }

  /** Стековое переключение состояние этого ContentHandler'а. */
  protected def become(h: MyHandler) {
    handlersStack ::= h
  }

  /** Извлечение текущего состояния из стека состояний. */
  protected def unbecome(): MyHandler = {
    val result = handlersStack.head
    handlersStack = handlersStack.tail
    result
  }

  override def setDocumentLocator(l: Locator) {
    locator = l
  }


  //------------------------------------------- FSM States ------------------------------------------------

  /** Родительский класс всех хендлеров. */
  trait MyHandler extends DefaultHandler {
    def myTag: String
    def myAttrs: Attributes

    def getTagNameFor(uri:String, localName:String, qName:String): String = {
      if (localName.isEmpty) {
        if (uri.isEmpty)  qName  else  ???
      } else {
        localName
      }
    }

    def startTag(tagName:String, attributes: Attributes) {}

    override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
      // Т.к. нет нормального неймспейса, усредняем тут localName и qName.
      super.startElement(uri, localName, qName, attributes)
      val tagName = getTagNameFor(uri, localName, qName)
      startTag(tagName, attributes)
    }

    def endTag(tagName: String) {
      if (tagName == myTag  &&  handlersStack.head == this)
        unbecome()
    }

    /** Выход из текущего элемента у всех одинаковый. */
    override def endElement(uri: String, localName: String, qName: String) {
      super.endElement(uri, localName, qName)
      val tagName = getTagNameFor(uri, localName, qName)
      endTag(tagName)
    }
  }

  /** Когда ничего не интересно, и нужно просто дождаться выхода из тега. */
  sealed case class MyDummyHandler(myTag: String, myAttrs: Attributes) extends MyHandler


  /** Хэндлер всего документа. Умеет обрабатывать лишь один тег. */
  case class TopLevelHandler() extends MyHandler {
    def myTag: String = ???
    def myAttrs: Attributes = ???

    override def endTag(tagName: String) {}

    override def startTag(tagName: String, attributes: Attributes) {
      if (tagName == TopLevelFields.yml_catalog.toString) {
        become(YmlCatalogHandler(attributes))
      } else {
        ???
      }
    }
  }


  /**
   * Обработчик тега верхнего уровня.
   * @param myAttrs Атрибуты тега yml_catalog, которые обычно включают в себя date=""
   */
  case class YmlCatalogHandler(myAttrs: Attributes) extends MyHandler {
    def myTag = TopLevelFields.yml_catalog.toString

    /** Дата генерации прайс-листа, заданного в аттрибутах. */
    val dateOpt: Option[DateTime] = Option(myAttrs.getValue(ATTR_DATE)) flatMap { dateStr =>
      val parseResult = parse(DT_PARSER, dateStr)
      if (parseResult.successful) {
        Some(parseResult.get)
      } else {
        None
      }
    }

    override def startTag(tagName: String, attributes: Attributes) {
      super.startTag(tagName, attributes)
      val nextHandler = YmlCatalogFields.withName(tagName) match {
        case YmlCatalogFields.shop => new ShopHandler(attributes)
      }
      become(nextHandler)
    }
  }


  /**
   * Обработчик содержимого тега shop.
   * @param myAttrs Атрибуты тега shop, которые вроде бы всегда пустые.
   */
  case class ShopHandler(myAttrs: Attributes = EmptyAttrs) extends MyHandler with ShopHandlerState {
    def myTag = YmlCatalogFields.shop.toString
    implicit protected def myShop = this

    var name                : String = null
    var company             : String = null
    var url                 : String = null
    var phoneOpt            : Option[String] = None
    //var platformOpt       : Option[String] = None
    //var versionOpt        : Option[String] = None
    //var agencyOpt         : Option[String] = None
    var emails              : List[String] = Nil
    var currencies          : List[ShopCurrency] = Nil
    var categories          : List[ShopCategory] = Nil
    var storeOpt            : Option[Boolean] = None
    var pickupOpt           : Option[Boolean] = None
    var deliveryOpt         : Option[Boolean] = None
    var deliveryIncludedOpt : Option[Boolean] = None
    var localDeliveryCostOpt: Option[Float]   = None
    var adultOpt            : Option[Boolean] = None

    /** Обход элементов shop'а. */
    override def startTag(tagName: String, attributes: Attributes) {
      super.startTag(tagName, attributes)
      val nextHandler = ShopFields.withName(tagName) match {
        case ShopFields.name                => new ShopNameHandler
        case ShopFields.company             => new ShopCompanyHandler
        case ShopFields.url                 => new ShopUrlHandler
        case ShopFields.phone               => new PhoneHandler
        case e @ (ShopFields.platform | ShopFields.version | ShopFields.agency) =>
          new MyDummyHandler(e.toString, attributes)
        case ShopFields.email               => EmailsHandler
        case ShopFields.currencies          => new CurrenciesHandler
        case ShopFields.categories          => new ShopCategoriesHandler
        case ShopFields.store               => new ShopStoreHandler
        case ShopFields.delivery            => new ShopDeliveryHandler
        case ShopFields.pickup              => new ShopPickupHandler
        case ShopFields.deliveryIncluded    => new ShopDeliveryIncludedHandler
        case ShopFields.local_delivery_cost => new ShopLocalDeliveryCostHandler
        case ShopFields.adult               => new ShopAdultHandler
        case ShopFields.offers              => new OffersHandler(attributes)
      }
      become(nextHandler)
    }


    class ShopNameHandler extends StringHandler {
      def myTag = ShopFields.email.toString
      def maxLen: Int = SHOP_NAME_MAXLEN
      def handleString(s: String) { name = s }
    }

    class ShopCompanyHandler extends StringHandler {
      def myTag = ShopFields.company.toString
      def maxLen: Int = SHOP_COMPANY_MAXLEN
      def handleString(s: String) { company = s }
    }

    class ShopUrlHandler extends UrlHandler {
      def myTag  = ShopFields.url.toString
      override def maxLen: Int = SHOP_URL_MAXLEN
      def handleUrl(_url: String) { url = _url }
      def urlTooLong(s: String) {
        throw YmShopFieldException(s"Shop URL too long. Max length is $maxLen.")
      }
    }

    class PhoneHandler extends StringHandler {
      def myTag = ShopFields.phone.toString
      def maxLen: Int = PHONE_MAXLEN
      def handleString(phoneStr: String) {
        // TODO Парсить телефон, проверять и сохранять в нормальном формате.
        phoneOpt = Some(phoneStr)
      }
    }

    object EmailsHandler extends StringHandler {
      def myTag = ShopFields.email.toString
      def maxLen: Int = EMAIL_MAXLEN
      def handleString(email: String) { emails ::= email }
    }

    /** Парсер списка валют магазина.
      * [[http://help.yandex.ru/partnermarket/currencies.xml Документация по списку валют магазина]]. */
    class CurrenciesHandler extends MyHandler {
      def myTag = ShopFields.currencies.toString
      def myAttrs = EmptyAttrs
      var currCounter = 0

      override def startTag(tagName: String, attributes: Attributes) {
        if ((tagName equalsIgnoreCase TAG_CURRENCY)  &&  currCounter < SHOP_CURRENCIES_COUNT_MAX) {
          become(new ShopCurrencyHandler(attributes))
          currCounter += 1
        }
      }
    }

    /** Парсер одной валюты. */
    class ShopCurrencyHandler(attrs: Attributes) extends StringHandler {
      def myTag = TAG_CURRENCY
      def maxLen: Int = SHOP_CURRENCY_ID_MAXLEN
      def handleString(s: String) {
        // TODO Надо проверять курс валют, чтобы он отклонялся не более на 30% от курса ЦБ.
        val maybeId = attrs.getValue(ATTR_ID)
        if (maybeId != null) {
          var id = maybeId.trim
          if (id.length > SHOP_CURRENCY_ID_MAXLEN) {
            throw YmShopFieldException(s"Currency id too long. Max length is $SHOP_CURRENCY_ID_MAXLEN.")
          }
          id = id.toUpperCase
          val rate = Option(attrs.getValue(ATTR_RATE)) getOrElse ShopCurrency.RATE_DFLT
          val plus = Option(attrs.getValue(ATTR_PLUS)) getOrElse ShopCurrency.PLUS_DFLT
          currencies ::= ShopCurrency(id, rate=rate, plus=plus)
        }
      }
    }

    /** Парсер списка категорий, которые заданы самим магазином. */
    class ShopCategoriesHandler extends MyHandler {
      def myTag = ShopFields.categories.toString
      override def myAttrs = EmptyAttrs
      var categoriesCount = 0

      override def startTag(tagName: String, attributes: Attributes) {
        if ((tagName equalsIgnoreCase ShopCategoriesFields.category.toString)  &&  categoriesCount < SHOP_CAT_COUNT_MAX) {
          categoriesCount += 1
          become(new ShopCategoryHandler(attributes))
        }
      }
    }
    
    class ShopCategoryHandler(attrs: Attributes) extends StringHandler {
      def myTag = ShopCategoriesFields.category.toString
      override def maxLen: Int = SHOP_CAT_VALUE_MAXLEN

      def handleString(s: String) {
        if (hadOverflow)
          throw YmShopFieldException(s"Category name too long. Max.length is $maxLen.")
        // Парсим id
        val maybeId = attrs.getValue(ATTR_ID)
        if (maybeId != null) {
          val idCur = maybeId.trim
          verifyIdLen(ATTR_ID, idCur, s)
          // Парсим опциональный parent_id
          val parentIdOptCur = Option(attrs.getValue(ATTR_PARENT_ID))
          parentIdOptCur foreach {
            verifyIdLen(ATTR_PARENT_ID, _, s)
          }
          categories ::= ShopCategory(id=idCur, parentId = parentIdOptCur, text=s)
        } else {
          throw YmShopFieldException(s"Attribute '$ATTR_ID' undefined, but it should.")
        }
      }

      def verifyIdLen(attrName: String, id: String, catName:String) {
        if (id.length > SHOP_CAT_ID_LEN_MAX)
          throw YmShopFieldException(s"Category attribute '$attrName' too long. Max.length is $SHOP_CAT_ID_LEN_MAX. Category name is '$catName' and it is ok.")
      }
    }

    trait ShopBooleanHandler extends BooleanHandler {
      def booleanNotParsed(s: String) {
        throw YmShopFieldException("Failed to parse boolean value from " + s)
      }
    }

    class ShopStoreHandler extends ShopBooleanHandler {
      def myTag = ShopFields.store.toString
      def handleBoolean(b: Boolean) { storeOpt = Some(b) }
    }

    class ShopDeliveryHandler extends ShopBooleanHandler {
      def myTag = ShopFields.delivery.toString
      def handleBoolean(b: Boolean) { deliveryOpt = Some(b) }
    }

    class ShopPickupHandler extends ShopBooleanHandler {
      def myTag = ShopFields.pickup.toString
      def handleBoolean(b: Boolean) { pickupOpt = Some(b) }
    }

    class ShopDeliveryIncludedHandler extends ShopBooleanHandler {
      def myTag = ShopFields.deliveryIncluded.toString
      def handleBoolean(b: Boolean) { deliveryIncludedOpt = Some(b) }
    }

    class ShopLocalDeliveryCostHandler extends FloatHandler {
      def myTag = ShopFields.local_delivery_cost.toString
      def handleFloat(f: Float) { localDeliveryCostOpt = Some(f) }
    }

    class ShopAdultHandler extends ShopBooleanHandler {
      def myTag = ShopFields.adult.toString
      def handleBoolean(b: Boolean) { adultOpt = Some(b) }
    }
  }


  case class OffersHandler(myAttrs: Attributes = EmptyAttrs)(implicit myShop: ShopHandler) extends MyHandler {
    def myTag = ShopFields.offers.toString

    override def startTag(tagName: String, attributes: Attributes) {
      val nextHandler: MyHandler = OffersFields.withName(tagName) match {
        case OffersFields.offer => AnyOfferHandler(attributes)
      }
      become(nextHandler)
    }
  }


  /** Статическая часть общего кода всех обработчиков коммерческих предложений. */
  object AnyOfferHandler {
    /**
     * В маркете есть несколько типов описания предложений магазинов.
     * Нужно определить тип и сгенерить необходимый handler.
     * [[http://help.yandex.ru/partnermarket/offers.xml Описание всех типов offer]]
     * @param attrs Исходные аттрибуты тега offer.
     * @return Какой-то хандлер, пригодный для парсинга указанного коммерческого предложения.
     */
    def apply(attrs: Attributes)(implicit myShop: ShopHandler): AnyOfferHandler = {
      val offerTypeRaw = attrs.getValue(ATTR_TYPE)
      val offerType = Option(offerTypeRaw)
        .map {OfferTypes.withName}
        .getOrElse {OfferTypes.default}
      // TODO Надо как-то от warning тут избавится!
      offerType match {
        case OfferTypes.Simple          => new SimpleOfferHandler(attrs)
        case OfferTypes.VendorModel     => new VendorModelOfferHandler(attrs)
        case OfferTypes.Book            => new BookOfferHandler(attrs)
        case OfferTypes.AudioBook       => new AudioBookOfferHandler(attrs)
        case OfferTypes.ArtistTitle     => new ArtistTitleHandler(attrs)
        case OfferTypes.Tour            => new TourHandler(attrs)
        case OfferTypes.EventTicket     => new EventTicketOfferHandler(attrs)
      }
    }
  }


  /** Базовый класс для обработчиков коммерческих предложений.
    * Разные типы предложений имеют между собой много общего, поэтому основная часть
    * логики разборки предложений находится здесь. */
  trait AnyOfferHandler extends MyHandler with OfferHandlerState {
    def myTag = OffersFields.offer.toString
    implicit def myShop: ShopHandler
    implicit protected def selfOfferHandler = this

    /** Опциональный уникальный идентификатор комерческого предложения. */
    val idOpt = Option(myAttrs.getValue(ATTR_ID))

    /**
     * Для корректного соотнесения всех вариантов товара с карточкой модели необходимо в описании каждого
     * товарного предложения использовать атрибут group_id. Значение атрибута числовое, задается произвольно.
     * Для всех предложений, которые необходимо отнести к одной карточке товара, должно быть указано одинаковое
     * значение атрибута group_id.
     * [[http://partner.market.yandex.ru/legal/clothes/ Цитата взята отсюда]]
     */
    val groupIdOpt = Option(myAttrs.getValue(ATTR_GROUP_ID))

    /** Значение необязательного флага available, если таков выставлен. */
    val availableOpt: Option[Boolean] = Option(myAttrs.getValue(ATTR_AVAILABLE)).map(_.toBoolean)

    // Прочитанные элементы предложения. Тут часть offer-независимых полей.
    /** Ссылка на коммерческое предложение на сайте магазина. */
    var urlOpt: Option[String] = None
    /** Цена коммерческого предложения. */
    var price: Float = 0F
    /** Валюта цены коммерческого предложения */
    var currencyId: String = null
    /** Список категорий магазина, который должен быть непустым. */
    var categoryIds: List[OfferCategoryId] = Nil
    /** Необязательная категория в общем дереве категорий яндекс-маркета. */
    var marketCategoryOpt: Option[MarketCategory] = None
    /** Ссылки на картинки. Их может и не быть. */
    var pictures: List[String] = Nil
    /** store: Элемент позволяет указать возможность купить соответствующий товар в розничном магазине. */
    var storeOpt: Option[Boolean] = None
    /** pickup: Доступность резервирования с самовывозом. */
    var pickupOpt: Option[Boolean] = None
    /** delivery: Допустима ли доставка для указанного товара? */
    var deliveryOpt: Option[Boolean] = None
    /** Включена ли доставка в стоимость товара? */
    var deliveryIncludedOpt: Option[Boolean] = None
    /** Стоимость доставки данного товара в своем регионе. */
    var localDeliveryCostOpt: Option[Float] = None
    /** Описание товара. */
    var descriptionOpt: Option[String] = None
    /** Краткие дополнительные сведения по покупке/доставке. */
    var salesNotesOpt: Option[String] = None
    /** Гарантия производителя: true, false или P1Y2M10DT2H30M. */
    var manufacturerWarrantyOpt: Option[Warranty] = None
    /** Страна-производитель товара. */
    var countryOfOriginOpt: Option[String] = None
    /** Можно ли скачать указанный нематериальный товар? */
    var downloadableOpt: Option[Boolean] = None
    /** Это товар "для взрослых"? */
    var adultOpt: Option[Boolean] = None
    /** Возрастная категория товара. Есть немного противоречивой документации, но суть в том,
      * что есть аттрибут units и набор допустимых целочисленное значений для указанных единиц измерения. */
    var ageOpt: Option[OfferAge] = None
    /** Параметры, специфичные для конкретного товара. */
    var params: List[OfferParam] = Nil
    var paramsCounter = 0


    /** Тут базовый комбинируемый обработчик полей комерческого предложения.
      * Тут, в trait'е, нужно использовать def вместо val, т.к. это точно будет переопределено в под-классах. */
    def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = {
      case (f, attrs) if AnyOfferFields.isIgnoredField(f)   => MyDummyHandler(f.toString, attrs)
      case (AnyOfferFields.url, _)                          => new OfferUrlHandler
      // buyurl ignored
      case (AnyOfferFields.price, _)                        => new PriceHandler
      // wprice ignored
      case (AnyOfferFields.currencyId, _)                   => new CurrencyIdHandler
      // xCategory ignored
      case (AnyOfferFields.categoryId, attrs)               => new CategoryIdHandler(attrs)
      case (AnyOfferFields.market_category, _)              => new MarketCategoryHandler
      case (AnyOfferFields.picture, _)                      => PictureUrlHandler
      case (AnyOfferFields.store, _)                        => new OfferStoreHandler
      case (AnyOfferFields.pickup, _)                       => new OfferPickupHandler
      case (AnyOfferFields.delivery, _)                     => new OfferDeliveryHandler
      case (AnyOfferFields.deliveryIncluded, _)             => new OfferDeliveryIncludedHandler
      case (AnyOfferFields.local_delivery_cost, _)          => new OfferLocalDeliveryCostHandler
      case (AnyOfferFields.description, _)                  => new DescriptionHandler
      // orderingTime: ignored - нет документации;; aliases, additional - ignored.
      case (AnyOfferFields.sales_notes, _)                  => new SalesNotesHandler
      // promo: ignored
      case (AnyOfferFields.manufacturer_warranty, _)        => new ManufacturerWarrantyHandler
      case (AnyOfferFields.country_of_origin, _)            => new CountryOfOriginHandler
      case (AnyOfferFields.downloadable, _)                 => new DownloadableHandler
      case (AnyOfferFields.adult, _)                        => new OfferAdultHandler
      case (AnyOfferFields.age, attrs)                      => new AgeHandler(attrs)
      // Поля param указываются у SIMPLE и vendor.model, но не запрещены для остальных типов офферов.
      case (AnyOfferFields.param, attrs)                    => new ParamHandler(attrs)
      // barcode: ignored
    }

    /** Начинается поле оффера. В зависимости от тега, выбрать обработчик. */
    override def startTag(tagName: String, attributes: Attributes) {
      val offerField = AnyOfferFields.withName(tagName)
      val handler = getFieldsHandler(offerField, attributes)
      become(handler)
    }

    trait OfferBooleanHandler extends BooleanHandler {
      def booleanNotParsed(s: String) {
        throw YmOfferFieldException("Failed to parse boolean value from " + s)
      }
    }

    trait OfferAnyUrlHandler extends UrlHandler {
      def urlTooLong(s: String) {
        throw YmOfferFieldException(s"Url too long. Max len is $maxLen.")
      }
    }

    /** Обработчик тега url, вставляющий в состояние результирующую ссылку. */
    class OfferUrlHandler extends OfferAnyUrlHandler {
      def myTag = AnyOfferFields.url.toString
      override def maxLen: Int = OFFER_URL_MAXLEN
      def handleUrl(url: String) { urlOpt = Some(url) }
    }

    /** Обработчик количественной цены товара/услуги. */
    class PriceHandler extends FloatHandler {
      def myTag: String = AnyOfferFields.price.toString
      def handleFloat(f: Float) { price = f }
    }

    /** Обработчик валюты цены предложения. */
    class CurrencyIdHandler extends StringHandler {
      def myTag = AnyOfferFields.currencyId.toString
      override def maxLen: Int = SHOP_CURRENCY_ID_MAXLEN
      def handleString(s: String) {
        // Проверяем валюту по списку валют магазина.
        if (!myShop.currencies.exists(_.id == s))
          throw YmOfferFieldException(s"Unexpected currencyId: '$s'. Defined shop's currencies are: ${myShop.currencies.map(_.id).mkString(", ")}")
        if (hadOverflow)
          throw YmOfferFieldException(s"Too long currency id. Max length is $maxLen.")
        currencyId = s
      }
    }

    /** Чтение поля categoryId, которое может иметь устаревший аттрибут type. */
    class CategoryIdHandler(attrs: Attributes) extends StringHandler {
      def myTag: String = AnyOfferFields.categoryId.toString

      /** Максимальная кол-во байт, которые будут сохранены в аккамулятор. */
      override def maxLen: Int = SHOP_CAT_ID_LEN_MAX
      def handleString(s: String) {
        val catIdType = Option(attrs.getValue(ATTR_TYPE)) map {OfferCategoryIdTypes.withName} getOrElse OfferCategoryIdTypes.default
        // TODO Возможно, надо для типа Yandex закидывать эту категорию в market_category, а не в categoryIds.
        // Из-за дефицита документации по устаревшим фичам, этот момент остается не яснен.
        categoryIds ::= OfferCategoryId(s, catIdType)
      }
    }

    /** Парсер необязательной marketCategory. Она только одна. Её нужно парсить согласно
      * [[http://help.yandex.ru/partnermarket/docs/market_categories.xls таблице категорий маркета]].
      * Пример значения: Одежда/Женская одежда/Верхняя одежда/Куртки
      */
    class MarketCategoryHandler extends StringHandler {
      def myTag = AnyOfferFields.market_category.toString
      override def maxLen: Int = MARKET_CAT_VALUE_MAXLEN

      def handleString(s: String) {
        // При превышении длины строки, нельзя выставлять категорию
        if (hadOverflow) {
          // TODO Категорию маркета надо парсить и выверять согласно вышеуказанной доке. Все токены пути должны быть выставлены строго согласно таблице категорий.
          val catPath = MARKET_CATEGORY_PATH_SPLIT_RE.split(s).toList.filter(!_.isEmpty)
          marketCategoryOpt = Some(MarketCategory(catPath))
        } else {
          throw YmOfferFieldException("Value too long.")
        }
      }
    }

    /** Аккамулирование ссылок на картинки в pictures. Картинок может быть много, и хотя бы одна почти всегда имеется,
      * поэтому блокируем многократное конструирование этого объекта. */
    object PictureUrlHandler extends OfferAnyUrlHandler {
      def myTag = AnyOfferFields.picture.toString
      override def maxLen: Int = OFFER_PICTURE_URL_MAXLEN
      def handleUrl(pictureUrl: String) {
        pictures ::= pictureUrl
      }
    }

    /** store: Парсим поле store, которое говорит, можно ли купить этот товар в магазине. */
    class OfferStoreHandler extends OfferBooleanHandler {
      def myTag = AnyOfferFields.store.toString
      def handleBoolean(b: Boolean) {
        storeOpt = Some(b)
      }
    }

    /** pickup: Парсим поле, говорящее нам о том, возможно ли зарезервировать и самовывезти указанный товар. */
    class OfferPickupHandler extends OfferBooleanHandler {
      def myTag = AnyOfferFields.pickup.toString
      def handleBoolean(b: Boolean) {
        pickupOpt = Some(b)
      }
    }

    /** delivery: Поле указывает, возможна ли доставка указанного товара? */
    class OfferDeliveryHandler extends OfferBooleanHandler {
      def myTag = AnyOfferFields.delivery.toString
      def handleBoolean(b: Boolean) {
        deliveryOpt = Some(b)
      }
    }

    /** deliveryIncluded: Включена ли доставка до покупателя в стоимость товара? Если тег задан, то включена. */
    class OfferDeliveryIncludedHandler extends BooleanTagFlagHandler {
      def myTag = AnyOfferFields.deliveryIncluded.toString
      def handleTrue() {
        deliveryIncludedOpt = Some(true)
      }
    }

    /** local_delivery_cost: Поле описывает стоимость доставки с своём регионе. */
    class OfferLocalDeliveryCostHandler extends FloatHandler {
      def myTag = AnyOfferFields.local_delivery_cost.toString
      def handleFloat(f: Float) {
        localDeliveryCostOpt = Some(f)
      }
    }

    /** description: Поле с описанием товара. Возможно, multiline-поле. */
    class DescriptionHandler extends StringHandler {
      // TODO Нужно убедится, что переносы строк отрабатываются корректно. Возможно, их надо подхватывать через ignorableWhitespace().
      def myTag = AnyOfferFields.description.toString
      override def maxLen: Int = DESCRIPTION_LEN_MAX
      override def ellipsis: String = DESCRIPTION_ELLIPSIS

      def handleString(desc: String) {
        descriptionOpt = Some(desc)
      }
    }

    /** sales_notes: Какие-то доп.данные по товару, которые НЕ индексируются, но отображаются юзеру.
      * Например "Минимальное кол-во единиц в заказе: 4 шт.".
      * Допустимая длина текста в элементе — 50 символов. */
    class SalesNotesHandler extends StringHandler {
      def myTag = AnyOfferFields.sales_notes.toString
      override def ellipsis: String = SALES_NOTES_ELLIPSIS
      override def maxLen: Int = SALES_NOTES_MAXLEN

      def handleString(salesNotes: String) {
        salesNotesOpt = Some(salesNotes)
      }
    }

    /** manufacturer_warranty: Поле с информацией о гарантии производителя. */
    class ManufacturerWarrantyHandler extends WarrantyHandler {
      def myTag = AnyOfferFields.manufacturer_warranty.toString
      def handleWarranty(wv: Warranty) {
        manufacturerWarrantyOpt = Some(wv)
      }
    }

    /** Страна-производитель из [[http://partner.market.yandex.ru/pages/help/Countries.pdf таблицы стран маркета]].
      * Например "Бразилия". */
    class CountryOfOriginHandler extends StringHandler {
      def myTag = AnyOfferFields.country_of_origin.toString
      override def maxLen: Int = COUNTRY_MAXLEN
      def handleString(s: String) {
        // TODO Нужно брать список стран, проверять по нему страну (желательно через триграммы), с поддержкой стран на иных языках.
        countryOfOriginOpt = Some(s)
      }
    }

    /** downloadable предназначен для обозначения товара, который можно скачать. Нормальной документации маловато,
      * поэтому считаем что тут boolean. */
    class DownloadableHandler extends OfferBooleanHandler {
      def myTag = AnyOfferFields.downloadable.toString
      def handleBoolean(b: Boolean) {
        downloadableOpt = Some(b)
      }
    }

    /** adult: обязателен для обозначения товара, имеющего отношение к удовлетворению сексуальных потребностей, либо
      * иным образом эксплуатирующего интерес к сексу.
      * [[http://help.yandex.ru/partnermarket/adult.xml Документация по тегу adult]]. */
    class OfferAdultHandler extends OfferBooleanHandler {
      def myTag = AnyOfferFields.adult.toString
      def handleBoolean(b: Boolean) {
        adultOpt = Some(b)
      }
    }

    /** Возрастая категория товара. Единицы измерения заданы в аттрибутах. */
    class AgeHandler(attrs: Attributes) extends IntHandler {
      def myTag = AnyOfferFields.age.toString
      def handleInt(i: Int) {
        val unitV1 = Option(attrs.getValue(ATTR_UNIT)) map { unitV =>
          OfferAgeUnits.withName(unitV.trim.toLowerCase)
        } getOrElse OfferAgeUnits.year
        // Проверить значения, чтобы соответствовали интервалам допустимых значений.
        val possibleValues = unitV1 match {
          case OfferAgeUnits.month => OFFER_AGE_MONTH_VALUES
          case OfferAgeUnits.year  => OFFER_AGE_YEAR_VALUES
        }
        if (!(possibleValues contains i)) {
          throw YmOfferFieldException(s"Invalid value for unit=$unitV1: $i ; Possible values are: ${possibleValues.mkString(", ")}")
        }
        ageOpt = Some(OfferAge(units = unitV1, value = i))
      }
    }

    // TODO Надо параметры как-то по-лучше парсить. Параметры также используются для фасетных группировок.
    // TODO Надо проверять нормализовать параметры по списку готовых названий параметров.
    // TODO Надо парсить значения согласно заданным механизам.
    class ParamHandler(attrs: Attributes) extends StringHandler {
      def myTag = AnyOfferFields.param.toString
      override def maxLen: Int = OFFER_PARAM_MAXLEN
      private def ex(msg: String) = throw YmOfferFieldException(msg)
      def handleString(s: String) {
        if (paramsCounter <= OFFER_PARAMS_COUNT_MAX) {
          val nameAttrKey = OfferParamAttrs.name.toString
          val paramName = attrs.getValue(nameAttrKey)
          if (paramName == null) {
            ex(s"Mandatory attribute '$nameAttrKey' missing.")
          }
          if (s.isEmpty) {
            ex(s"Param '$paramName' value is empty.")
          }
          val unitOpt = Option(attrs.getValue(OfferParamAttrs.unit.toString)) map(_.trim) flatMap { str =>
            if (str.isEmpty)  None  else  Some(str)
          }
          params ::= OfferParam(name=paramName, unitRawOpt=unitOpt, rawValue=s)
        } else {
          ex("Too many params for one offer. Max is " + OFFER_PARAMS_COUNT_MAX)
        }
      }
    }
  }


  trait VendorInfoH extends AnyOfferHandler {
    /** Производитель. Не отображается в названии предложения. Например: HP. */
    var vendorOpt: Option[String] = None
    /** Код товара (указывается код производителя). Например: A1234567B. */
    var vendorCodeOpt: Option[String] = None


    /** Тут базовый комбинируемый обработчик полей комерческого предложения.
      * Тут, в trait'е, нужно использовать def вместо val, т.к. это точно будет переопределено в под-классах. */
    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = {
      super.getFieldsHandler orElse {
        case (AnyOfferFields.vendor, _)     => new VendorHandler
        case (AnyOfferFields.vendorCode, _) => new VendorCodeHandler
      }
    }

    /** vendor: поле содержит производителя товара. */
    class VendorHandler extends StringHandler {
      def myTag = AnyOfferFields.vendor.toString
      def maxLen: Int = VENDOR_MAXLEN
      def handleString(s: String) {
        // Базовая нормализация нужна, т.к. это поле не отображается юзеру и используется для группировки по брендам.
        // TODO Надо бы нормализовывать с синонимами, чтобы HP и Hewlett-packard были эквивалентны.
        val vendor = s.toLowerCase
        vendorOpt = Some(vendor)
      }
    }

    /** vendorCode: Номер изделия по мнению производителя. Не индексируется, не используется для группировки. */
    class VendorCodeHandler extends StringHandler {
      def myTag = AnyOfferFields.vendorCode.toString
      def maxLen: Int = VENDOR_CODE_MAXLEN
      def handleString(s: String) {
        vendorCodeOpt = Some(s)
      }
    }
  }


  /** Большинство офферов имеют поле name. Тут поддержка этого поля. */
  trait OfferNameH extends AnyOfferHandler {
    /** name обозначает какое-то отображаемое имя в зависимости от контекста. Везде оно обязательно. */
    var name: String = null

    /** Обработчик полей коммерческого предложения, который также умеет name. */
    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = {
      super.getFieldsHandler orElse {
        case (AnyOfferFields.name, _) => new NameHandler
      }
    }

    /** name: Поле описывает название комерческого предложения в зависимости от контекста. */
    class NameHandler extends StringHandler {
      def myTag = AnyOfferFields.name.toString
      override def maxLen: Int = OFFER_NAME_MAXLEN
      def handleString(s: String) {
        name = s
      }
    }
  }


  /** "Упрощенное описание" - исторический перво-формат яндекс-маркета.
    * Поле type ещё не заимплеменчено, и есть некоторый ограниченный и фиксированный набор полей.
    * Доп.поля: vendor, vendorCode. */
  case class SimpleOfferHandler(myAttrs: Attributes)(implicit shop: ShopHandler) extends VendorInfoH with OfferNameH {
    def offerType = OfferTypes.Simple
    def myShop = shop
  }

  /** Произвольный товар (vendor.model)
    * Этот тип описания является наиболее удобным и универсальным, он рекомендован для описания товаров из
    * большинства категорий Яндекс.Маркета.
    * Доп.поля: typePrefix, [vendor, vendorCode], model, provider, tarifPlan. */
  case class VendorModelOfferHandler(myAttrs: Attributes)(implicit shop: ShopHandler) extends VendorInfoH {
    def offerType = OfferTypes.VendorModel
    def myShop = shop
    /** Описание очень краткое. "Группа товаров / категория". Хз что тут и как. */
    var typePrefix: Option[String] = None
    /** Модель товара, хотя судя по примерам, там может быть и категория, а сама "модель". "Женская куртка" например. */
    var model: String = null
    /** Гарантия от продавца. Аналогично manufacturer_warranty. */
    var sellerWarrantyOpt: Option[Warranty] = None
    /** Список id рекомендуемых товаров к этому товару. */
    var recIdsList: List[String] = Nil
    /** Истечение срока годности: период или же дата. */
    var expiryOpt: Option[Either[DateTime, Period]] = None
    /** Вес товара с учётом упаковки. */
    var weightKgOpt: Option[Float] = None
    /** Размерности товара. */
    var dimensionsOpt: Option[Dimensions] = None

    /** Дополненный для vendor.model обработчик полей комерческого предложения. */
    override val getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = {
      super.getFieldsHandler orElse {
        case (f @ (AnyOfferFields.tarifplan | AnyOfferFields.provider | AnyOfferFields.cpa), attrs) =>
          MyDummyHandler(f.toString, attrs)
        case (AnyOfferFields.typePrefix, _)           => new TypePrefixHandler
        case (AnyOfferFields.model, _)                => new ModelHandler
        case (AnyOfferFields.seller_warranty, _)      => new SellerWarrantyHandler
        case (AnyOfferFields.rec, _)                  => new RecommendedOffersHandler
        case (AnyOfferFields.expiry, _)               => new ExpiryHandler
        case (AnyOfferFields.weight, _)               => new WeightHandler
        case (AnyOfferFields.dimensions, _)           => new DimensionsHandler
      }
    }


    /** Начинается поле оффера. Нужно исправить ошибки в некоторых полях. */
    override def startTag(tagName: String, attributes: Attributes) {
      // Костыль из-за ошибок в правописании названия ключа seller_warranty
      val tagName1 = if (SELLER_WARRANTY_RE.pattern.matcher(tagName).matches()) {
        AnyOfferFields.seller_warranty.toString
      } else {
        tagName
      }
      super.startTag(tagName1, attributes)
    }


    /** typePrefix: Поле с необязательным типом. "Принтер" например. */
    class TypePrefixHandler extends StringHandler {
      def myTag = AnyOfferFields.typePrefix.toString
      override def maxLen: Int = OFFER_TYPE_PREFIX_MAXLEN
      def handleString(s: String) {
        typePrefix = Some(s)
      }
    }

    /** model: Некая модель товара, т.е. правая часть названия. Например "Женская куртка" или "Deskjet D31337". */
    class ModelHandler extends StringHandler {
      def myTag = AnyOfferFields.model.toString
      def maxLen: Int = OFFER_MODEL_MAXLEN
      def handleString(s: String) {
        model = s
      }
    }

    /** Поле, описывающее гарантию от продавца. */
    class SellerWarrantyHandler extends WarrantyHandler {
      def myTag = AnyOfferFields.seller_warranty.toString
      def handleWarranty(wv: Warranty) {
        sellerWarrantyOpt = Some(wv)
      }
      override def endTag(tagName: String) {
        val tagName1 = if (SELLER_WARRANTY_RE.pattern.matcher(tagName).matches()) {
          myTag
        } else {
          throw YmOfferFieldException("Unexpected closing tag.")
        }
        super.endTag(tagName1)
      }
    }

    /** rec: Список id рекомендуемых к покупке товаров вместе с этим товаром. */
    class RecommendedOffersHandler extends StringHandler {
      def myTag = AnyOfferFields.rec.toString
      override def maxLen: Int = REC_LIST_MAX_CHARLEN
      def handleString(s: String) {
        var recIds: Seq[String] = REC_LIST_SPLIT_RE.split(s).toSeq
        if (recIds.size > REC_LIST_MAX_LEN) {
          recIds = recIds.slice(0, REC_LIST_MAX_LEN - 1)
        }
        recIdsList ++= recIds
      } 
    }

    /** expiry: Истечение срока годности/службы или чего-то подобного. Может быть как период, так и дата. */
    class ExpiryHandler extends StringHandler {
      def myTag = AnyOfferFields.expiry.toString
      override def maxLen: Int = 30
      def handleString(s: String) {
        val pr = parse(EXPIRY_PARSER, s)
        if (pr.successful) {
          expiryOpt = Some(pr.get)
        }
      }
    }

    /** weight: Обработчик поля, содержащего полный вес товара. */
    class WeightHandler extends FloatHandler {
      def myTag = AnyOfferFields.weight.toString
      def handleFloat(f: Float) {
        weightKgOpt = Some(f)
      }
    }

    /** Обработчик размерностей товара в формате: длина/ширина/высота. */
    class DimensionsHandler extends SimpleValueHandler {
      def myTag = AnyOfferFields.dimensions.toString
      override def maxLen: Int = OFFER_DIMENSIONS_MAXLEN
      def handleRawValue(sb: StringBuilder) {
        dimensionsOpt = Option(parse(DIMENSIONS_PARSER, sb) getOrElse null)
      }
    }
  }

  /** Добавка с годом издания указанной продукции народного творчества. */
  trait OfferYearH extends AnyOfferHandler {
    /** Год издания. */
    var yearOpt: Option[Int] = None

    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.year, _) => new YearHandler
    }

    class YearHandler extends IntHandler {
      def myTag = AnyOfferFields.year.toString
      def handleInt(i: Int) {
        if (i > 200 && i <= startedAt.getYear) {
          yearOpt = Some(i)
        }
      }
    }
  }


  /** Типы book и audiobook имеют много обищих полей:
   * author?, name, publisher?, series?, year?, ISBN?, volume?, part?, language?, table_of_contents?.
   * Тут трейт, который обеспечивает поддержку общих полей для обоих типов книг. */
  trait AnyBookOfferHandler extends AnyOfferHandler with OfferYearH with OfferNameH {
    def offerType = OfferTypes.Book
    /** Автор произведения, если есть. */
    var authorOpt: Option[String] = None
    /** Издательство. */
    var publisherOpt: Option[String] = None
    /** Серия. */
    var seriesOpt: Option[String] = None
    /** Код ISBN. */
    var isbnOpt: Option[String] = None
    /** Кол-во томов. */
    var volumesCountOpt: Option[Int] = None
    /** Номер тома. */
    var volumeOpt: Option[Int] = None
    /** Язык произведения. Пока что строка. */
    var languageOpt: Option[String] = None
    /** Краткое оглавление. Для сборника рассказов: список рассказов. */
    var tableOfContentsOpt: Option[String] = None

    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.author, _)             => new AuthorHandler
      case (AnyOfferFields.publisher, _)          => new PublisherHandler
      case (AnyOfferFields.series, _)             => new SeriesHandler
      case (AnyOfferFields.ISBN, _)               => new ISBNHandler
      case (AnyOfferFields.volume, _)             => new VolumesCountHandler
      case (AnyOfferFields.part, _)               => new VolumesPartHandler
      case (AnyOfferFields.language, _)           => new LanguageHandler
      case (AnyOfferFields.table_of_contents, _)  => new TableOfContentsHandler
    }

    class AuthorHandler extends StringHandler {
      def myTag = AnyOfferFields.author.toString
      override def maxLen: Int = OFFER_AUTHOR_MAXLEN
      def handleString(s: String) {
        authorOpt = Some(s)
      }
    }

    class PublisherHandler extends StringHandler {
      def myTag = AnyOfferFields.publisher.toString
      override def maxLen: Int = OFFER_PUBLISHER_MAXLEN
      def handleString(s: String) {
        publisherOpt = Some(s)
      }
    }

    class SeriesHandler extends StringHandler {
      def myTag = AnyOfferFields.series.toString
      override def maxLen: Int = OFFER_SERIES_MAXLEN
      def handleString(s: String) {
        seriesOpt = Some(s)
      }
    }

    class ISBNHandler extends StringHandler {
      def myTag = AnyOfferFields.ISBN.toString
      override def maxLen: Int = OFFER_ISBN_MAXLEN
      def handleString(s: String) {
        // TODO В конце номера контрольная цифра, её следует проверять. Также, следует выверять весь остальной формат.
        isbnOpt = Some(s)
      }
    }

    class VolumesCountHandler extends IntHandler {
      def myTag = AnyOfferFields.volume.toString
      def handleInt(i: Int) {
        volumesCountOpt = Some(i)
      }
    }
    
    class VolumesPartHandler extends IntHandler {
      def myTag = AnyOfferFields.part.toString
      def handleInt(i: Int) {
        volumeOpt = Some(i)
      } 
    }

    class LanguageHandler extends StringHandler {
      def myTag = AnyOfferFields.language.toString
      override def maxLen: Int = OFFER_LANGUAGE_MAXLEN
      def handleString(s: String) {
        languageOpt = Some(s)
      }
    }

    class TableOfContentsHandler extends StringHandler {
      def myTag = AnyOfferFields.table_of_contents.toString
      override def maxLen: Int = OFFER_TOC_MAXLEN
      def handleString(s: String) {
        tableOfContentsOpt = Some(s)
      }
    }
  }

  /**
   * Обработчик для типа book, т.е. бумажных книженций.
   * @param myAttrs Аттрибуты оффера.
   * @param shop Текущий магазин.
   */
  case class BookOfferHandler(myAttrs:Attributes)(implicit shop:ShopHandler) extends AnyBookOfferHandler {
    def myShop = shop
    /** Переплёт. */
    var bindingOpt: Option[String] = None
    /** Кол-во страниц в книге. */
    var pageExtentOpt: Option[Int] = None

    override val getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.binding, _)      => new BindingHandler
      case (AnyOfferFields.page_extent, _)  => new PageExtentHandler
    }

    class BindingHandler extends StringHandler {
      def myTag = AnyOfferFields.binding.toString
      override def maxLen: Int = OFFER_BINDING_MAXLEN
      def handleString(s: String) {
        bindingOpt = Some(s)
      }
    }

    class PageExtentHandler extends IntHandler {
      def myTag = AnyOfferFields.page_extent.toString
      def handleInt(i: Int) {
        pageExtentOpt = Some(i)
      }
    }
  }


  /**
   * Аудиокнига.
   * @param myAttrs Атрибуты оффера.
   * @param shop Текущий магазин.
   */
  case class AudioBookOfferHandler(myAttrs:Attributes)(implicit shop:ShopHandler) extends AnyBookOfferHandler {
    def myShop = shop

    /** Исполнитель. Если их несколько, перечисляются через запятую. */
    var performedByOpt: Option[String] = None
    /** Тип аудиокниги (радиоспектакль, произведение начитано, ...). */
    var performanceTypeOpt: Option[String] = None
    /** Носитель, на котором поставляется аудиокнига. */
    var storageOpt: Option[String] = None
    /** Формат аудиокниги. */
    var formatOpt: Option[String] = None
    /** Время звучания задается в формате mm.ss (минуты.секунды). */
    var recordingLenOpt: Option[Period] = None

    override val getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.performed_by, _)     => new PerformedByHandler
      case (AnyOfferFields.performance_type, _) => new PerformanceTypeHandler
      case (AnyOfferFields.storage, _)          => new StorageHandler
      case (AnyOfferFields.format, _)           => new FormatHandler
      case (AnyOfferFields.recording_length, _) => new RecordingLenHandler
    }

    class PerformedByHandler extends StringHandler {
      def myTag = AnyOfferFields.performed_by.toString
      override def maxLen: Int = OFFER_PERFORMED_BY_MAXLEN
      def handleString(s: String) {
        performedByOpt = Some(s)
      }
    }

    class PerformanceTypeHandler extends StringHandler {
      def myTag = AnyOfferFields.performance_type.toString
      override def maxLen: Int = OFFER_PERFORMANCE_TYPE_MAXLEN
      def handleString(s: String) {
        performanceTypeOpt = Some(s)
      }
    }

    class StorageHandler extends StringHandler {
      def myTag = AnyOfferFields.storage.toString
      override def maxLen: Int = OFFER_STORAGE_MAXLEN
      def handleString(s: String) {
        storageOpt = Some(s)
      }
    }

    class FormatHandler extends StringHandler {
      def myTag = AnyOfferFields.format.toString
      def maxLen: Int = OFFER_FORMAT_MAXLEN
      def handleString(s: String) {
        formatOpt = Some(s)
      }
    }

    class RecordingLenHandler extends StringHandler {
      def myTag = AnyOfferFields.recording_length.toString
      override def maxLen: Int = OFFER_REC_LEN_MAXLEN
      def handleString(s: String) {
        val parseResult = parse(RECORDING_LEN_PARSER, s)
        if (parseResult.successful) {
          recordingLenOpt = Some(parseResult.get)
        }
      }
    }
  }


  trait OfferCountryOptH extends AnyOfferHandler {
    var countryOpt: Option[String] = None

    /** Тут базовый комбинируемый обработчик полей комерческого предложения.
      * Тут, в trait'е, нужно использовать def вместо val, т.к. это точно будет переопределено в под-классах. */
    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.country, _) => new CountryHandler
    }

    class CountryHandler extends StringHandler {
      def myTag = AnyOfferFields.country.toString
      override def maxLen: Int = COUNTRY_MAXLEN
      def handleString(s: String) {
        // TODO Нужно проверять страну по списку оных.
        countryOpt = Some(s)
      }
    }
  }


  /**
   * Обработчик artist.title-офферов, содержащих аудио/видео хлам.
   * @param myAttrs Аттрибуты этого оффера.
   * @param shop Текущий магазин.
   */
  case class ArtistTitleHandler(myAttrs:Attributes)(implicit shop:ShopHandler) extends AnyOfferHandler with OfferCountryOptH with OfferYearH {
    def offerType = OfferTypes.ArtistTitle
    def myShop = shop

    /** Исполнитель, если есть. */
    var artistOpt: Option[String] = None
    /** Заголовок. */
    var title: String = null
    /** Носитель. */
    var mediaOpt: Option[String] = None
    /** Актёры. */
    var starringOpt: Option[String] = None
    /** Режиссер. */
    var directorOpt: Option[String] = None
    /** Оригинальное название. */
    var originalNameOpt: Option[String] = None


    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.artist, _)       => new ArtistHandler
      case (AnyOfferFields.title, _)        => new TitleHandler
      case (AnyOfferFields.media, _)        => new MediaHandler
      case (AnyOfferFields.starring, _)     => new StarringHandler
      case (AnyOfferFields.director, _)     => new DirectorHandler
      case (AnyOfferFields.originalName, _) => new OriginalNameHandler
    }

    class ArtistHandler extends StringHandler {
      def myTag = AnyOfferFields.artist.toString
      override def maxLen: Int = OFFER_ARTIST_MAXLEN
      def handleString(s: String) {
        artistOpt = Some(s)
      }
    }

    class TitleHandler extends StringHandler {
      def myTag = AnyOfferFields.title.toString
      override def maxLen: Int = OFFER_TITLE_MAXLEN
      def handleString(s: String) {
        title = s
      }
    }

    class MediaHandler extends StringHandler {
      def myTag = AnyOfferFields.media.toString
      override def maxLen: Int = OFFER_MEDIA_MAXLEN
      def handleString(s: String) {
        mediaOpt = Some(s)
      }
    }

    class StarringHandler extends StringHandler {
      def myTag = AnyOfferFields.starring.toString
      override def maxLen: Int = OFFER_STARRING_MAXLEN
      def handleString(s: String) {
        starringOpt = Some(s)
      }
    }

    class DirectorHandler extends StringHandler {
      def myTag = AnyOfferFields.director.toString
      override def maxLen: Int = OFFER_DIRECTOR_MAXLEN
      def handleString(s: String) {
        directorOpt = Some(s)
      }
    }
    
    class OriginalNameHandler extends StringHandler {
      def myTag = AnyOfferFields.originalName.toString
      override def maxLen: Int = OFFER_NAME_MAXLEN
      def handleString(s: String) {
        originalNameOpt = Some(s)
      }
    }
  }


  case class TourHandler(myAttrs:Attributes)(implicit shop:ShopHandler) extends AnyOfferHandler with OfferCountryOptH with OfferNameH {
    def offerType = OfferTypes.Tour
    def myShop = shop

    /** Регион мира (часть света, материк планеты и т.д.), к которому относится путёвка. "Африка" например. */
    var worldRegionOpt: Option[String] = None
    /** Курорт и город */
    var regionOpt: Option[String] = None
    /** Количество дней тура. */
    var days: Int = -1
    /** Даты заездов. */
    var tourDates: List[DateTime] = Nil
    /** Звёзды гостиницы. Формат - хз, поэтому возвращаем всырую. */
    var hotelStartsOpt: Option[HotelStarsLevel] = None
    /** Тип комнаты в гостинице (SGL, DBL, ...) */
    var roomOpt: Option[HotelRoomInfo] = None
    /** Тип питания в гостинице. */
    var mealOpt: Option[HotelMealType] = None
    /** Что включено в стоимость тура. Обязательнах. */
    var included: String = null
    /** Транспорт. Тоже обязательно. */
    var transport: String = null


    /** Тут базовый комбинируемый обработчик полей комерческого предложения.
      * Тут, в trait'е, нужно использовать def вместо val, т.к. это точно будет переопределено в под-классах. */
    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.worldRegion, _)  => new WorldRegionHandler
      case (AnyOfferFields.region, _)       => new RegionHandler
      case (AnyOfferFields.days, _)         => new DaysHandler
      case (AnyOfferFields.dataTour, _)     => new TourDatesHandler
      case (AnyOfferFields.hotel_stars, _)  => new HotelStarsHandler
      case (AnyOfferFields.room, _)         => new RoomHandler
      case (AnyOfferFields.meal, _)         => new MealHandler
      case (AnyOfferFields.included, _)     => new IncludedHandler
      case (AnyOfferFields.transport, _)    => new TransportHandler
      // Описания этих цен в доках нет, да и сложно это для нас. Поэтому их пропускаем.
      case (f @ (AnyOfferFields.price_min | AnyOfferFields.price_max), attrs) =>
        MyDummyHandler(f.toString, attrs)
    }

    class WorldRegionHandler extends StringHandler {
      def myTag = AnyOfferFields.worldRegion.toString
      override def maxLen: Int = WORLD_REGION_MAXLEN
      def handleString(s: String) {
        worldRegionOpt = Some(s)
      }
    }
    
    class RegionHandler extends StringHandler {
      override def maxLen: Int = REGION_MAXLEN
      def myTag = AnyOfferFields.region.toString
      def handleString(s: String) {
        regionOpt = Some(s)
      }
    }

    class DaysHandler extends IntHandler {
      def myTag = AnyOfferFields.days.toString
      def handleInt(i: Int) {
        days = i
      }
    }

    class TourDatesHandler extends DateTimeHandler {
      def myTag = AnyOfferFields.dataTour.toString
      def handleDateTime(dt: DateTime) {
        tourDates ::= dt
      }
    }

    class HotelStarsHandler extends StringHandler {
      def myTag = AnyOfferFields.hotel_stars.toString
      override def maxLen: Int = HOTEL_STARS_MAXLEN
      def handleString(s: String) {
        val parseResult = parse(HOTEL_STARS_PARSER, s)
        if (parseResult.successful) {
          hotelStartsOpt = Some(parseResult.get)
        }
      }
    }

    class RoomHandler extends StringHandler {
      def myTag = AnyOfferFields.room.toString
      def maxLen: Int = ROOM_TYPE_MAXLEN
      def handleString(s: String) {
        val parseResult = parse(HOTEL_ROOM_PARSER, s)
        if (parseResult.successful)
          roomOpt = Some(parseResult.get)
      }
    }

    class MealHandler extends StringHandler {
      def myTag = AnyOfferFields.meal.toString
      def maxLen: Int = MEAL_MAXLEN
      def handleString(s: String) {
        val parseResult = parse(HOTEL_MEAL_PARSER, s)
        if (parseResult.successful)
          mealOpt = Some(parseResult.get)
      } 
    }

    class IncludedHandler extends StringHandler {
      def myTag: String = AnyOfferFields.included.toString
      def maxLen: Int = TOUR_INCLUDED_MAXLEN
      def handleString(s: String) {
        included = s
      }
    }

    class TransportHandler extends StringHandler {
      def myTag = AnyOfferFields.transport.toString
      def maxLen: Int = TRANSPORT_MAXLEN
      def handleString(s: String) {
        transport = s
      }
    }
  }


  /**
   * Билет на мероприятие.
   * @param myAttrs Аттрибуты тега оффера.
   * @param shop Магазин.
   */
  case class EventTicketOfferHandler(myAttrs:Attributes)(implicit shop:ShopHandler) extends AnyOfferHandler with OfferNameH {
    def offerType = OfferTypes.EventTicket
    def myShop = shop

    /** Место проведения мероприятия. */
    var placeOpt: Option[String] = None
    /** Название зала и ссылка на изображение с планом зала. */
    var hallOpt: Option[String] = None
    var hallPlanUrlOpt: Option[String] = None
    /** hall_part: К какой части зала относится билет. */
    var hallPartOpt: Option[String] = None
    /** Дата и время сеанса. */
    var date: DateTime = null
    /** Признак премьерности мероприятия. */
    var isPremiereOpt: Option[Boolean] = None
    /** Признак детского мероприятия. */
    var isKidsOpt: Option[Boolean] = None


    /** Обработчик полей коммерческого предложения, который также умеет name. */
    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.place, _)        => new PlaceHandler
      case (AnyOfferFields.hall, attrs)     => new HallPlanHandler(attrs)
      case (AnyOfferFields.hall_part, _)    => new HallPartHandler
      case (AnyOfferFields.date, _)         => new EventDateHandler
      case (AnyOfferFields.is_premiere, _)  => new IsPremiereHandler
      case (AnyOfferFields.is_kids, _)      => new IsKidsHandler
    }

    class PlaceHandler extends StringHandler {
      def myTag = AnyOfferFields.place.toString
      def maxLen: Int = PLACE_MAXLEN
      def handleString(s: String) {
        // TODO Нужна география.
        placeOpt = Some(s)
      }
    }

    class HallPlanHandler(attrs: Attributes) extends StringHandler {
      def myTag = AnyOfferFields.hall.toString
      def maxLen: Int = HALL_MAXLEN
      def handleString(s: String) {
        if (!s.isEmpty) {
          hallOpt = Some(s)
        }
        hallPlanUrlOpt = Option(attrs.getValue(ATTR_PLAN))
          .map(_.trim)
          .filter { maybeUrl =>
            try {
              new URL(maybeUrl)
              true
            } catch {
              case ex: Exception => false
            }
          }.map { UrlUtil.normalize }
      }
    }

    class HallPartHandler extends StringHandler {
      def myTag = AnyOfferFields.hall_part.toString
      def maxLen: Int = HALL_PART_MAXLEN
      def handleString(s: String) {
        hallPartOpt = Some(s)
      }
    }

    class EventDateHandler extends DateTimeHandler {
      def myTag = AnyOfferFields.date.toString
      def handleDateTime(dt: DateTime) {
        date = dt
      }
    }

    class IsPremiereHandler extends OfferBooleanHandler {
      def myTag = AnyOfferFields.is_premiere.toString
      def handleBoolean(b: Boolean) {
        isPremiereOpt = Some(b)
      }
    }

    class IsKidsHandler extends OfferBooleanHandler {
      def myTag = AnyOfferFields.is_kids.toString
      def handleBoolean(b: Boolean) {
        isKidsOpt = Some(b)
      }
    }
  }


  /** Враппер для дедубликации кода хандлеров, которые читают текст из тега и что-то делают с накопленным буффером. */
  trait SimpleValueHandler extends MyHandler with SimpleValueT {
    implicit protected def selfFieldHandler: SimpleValueT = this
    /** Максимальная кол-во символов, которые будут сохранены в аккамулятор будущей строки. */
    def maxLen: Int
    def ellipsis: String = ELLIPSIS_DFLT
    protected var hadOverflow: Boolean = false
    protected val sb = new StringBuilder
    protected var sbLen = 0
    def myAttrs: Attributes = EmptyAttrs
    def handleRawValue(sb: StringBuilder)

    /** Сброс переменных накопления данных в исходное состояние. */
    def reset() {
      sbLen = 0
      sb.clear()
      hadOverflow = false
    }

    /** Считываем символы в буфер с ограничением по максимальной длине буффера. Иными словами, не считываем
      * ничего лишнего. Если переполнение, то выставить флаг и многоточие вместо последнего символа. */
    override def characters(ch: Array[Char], start: Int, length: Int) {
      val _maxLen = maxLen
      if (sbLen < _maxLen) {
        if (sbLen + length >= _maxLen) {
          val copyLen = _maxLen - sbLen - ellipsis.length
          sb.appendAll(ch, start, copyLen)
          sbLen = _maxLen
          sb append ellipsis
          hadOverflow = true
        } else {
          sb.appendAll(ch, start, length)
          sbLen += length
        }
      }
     }

    /** Выход из текущего элемента. Вызвать функцию, обрабатывающую собранный результат. */
    override def endTag(tagName: String) {
      handleRawValue(sb)
      super.endTag(tagName)
    }
  }

  /** Абстрактный обработчик поля, которое содержит какой-то текст. */
  trait StringHandler extends SimpleValueHandler {
    def handleString(s: String)

    def handleRawValue(sb: StringBuilder) {
      // TODO Нужно удалять из строки ненужные пробелы: проблемы около концов строк, например.
      val s = MANY_SPACES_RE.replaceAllIn(sb.toString().trim, " ")
      handleString(s)
    }
  }

  /** Обработчик ссылок. */
  trait UrlHandler extends StringHandler {
    def handleUrl(url: String)
    override def maxLen: Int = URL_MAXLEN
    def handleString(s: String) {
      if (hadOverflow) {
        urlTooLong(s)
      } else {
        // Необходимо сверять домен ссылки? По идее магазин платит деньги, значит кривая ссылка - это его проблема, и сверять не надо.
        val url1 = UrlUtil.normalize(s)
        handleUrl(url1)
      }
    }
    def urlTooLong(s: String)
  }

  /** Абстрактный обработчик полей, содержащих дату-время (или только дату). */
  trait DateTimeHandler extends StringHandler {
    override def maxLen: Int = DATE_TIME_MAXLEN
    def handleDateTime(dt: DateTime)
    def handleString(s: String) {
      val parseResult = parse(DT_PARSER, s)
      if (parseResult.successful) {
        handleDateTime(parseResult.get)
      }
    }
  }

  /** Хелпер для разбора поля с информацией о гарантии. */
  trait WarrantyHandler extends SimpleValueHandler {
    override def maxLen: Int = OFFER_WARRANTY_MAXLEN
    def handleWarranty(wv: Warranty)
    def handleRawValue(sb: StringBuilder) {
      val parseResult = parse(WARRANTY_PARSER, sb)
      if (parseResult.successful) {
        handleWarranty(parseResult.get)
      }
    }
  }


  /** Хелпер для парсинга boolean-значений в телах тегов. */
  trait BooleanHandler extends SimpleValueHandler {
    def handleBoolean(b: Boolean)
    override def maxLen: Int = BOOLEAN_MAXLEN
    def handleRawValue(sb: StringBuilder) {
      val parseResult = parse(BOOL_PARSER, sb)
      if (parseResult.successful) {
        handleBoolean(parseResult.get)
      } else {
        booleanNotParsed(sb.toString())
      }
    }
    def booleanNotParsed(s: String)
  }

  trait BooleanTagFlagHandler extends MyHandler {
    def handleTrue()
    def myAttrs = EmptyAttrs

    override def endTag(tagName: String) {
      handleTrue()
      super.endTag(tagName)
    }
  }

  /** Абстрактный обработчки полей, содержащий integer-значения. */
  trait IntHandler extends SimpleValueHandler {
    def handleInt(i: Int)
    override def maxLen: Int = INT_MAXLEN
    def handleRawValue(sb: StringBuilder) {
      handleInt(sb.toInt)
    }
  }
  
  /** Абстрактный обработчик полей, которые содержат float-значения. */
  trait FloatHandler extends SimpleValueHandler {
    def handleFloat(f: Float)
    override def maxLen: Int = FLOAT_MAXLEN
    def handleRawValue(sb: StringBuilder) {
      handleFloat(sb.toFloat)
    }
  }

}


/** Экспорт интерфейса ShopHandler для возможности работы с ним извне. */
trait ShopHandlerState {
  def name                : String
  def company             : String
  def url                 : String
  def phoneOpt            : Option[String]
  def emails              : List[String]
  def currencies          : List[ShopCurrency]
  def categories          : List[ShopCategory]
  def storeOpt            : Option[Boolean]
  def pickupOpt           : Option[Boolean]
  def deliveryOpt         : Option[Boolean]
  def deliveryIncludedOpt : Option[Boolean]
  def localDeliveryCostOpt: Option[Float]
  def adultOpt            : Option[Boolean]
}


/** Экспорт состояния оффера для возможности взаимодействия с ним извне. */
trait OfferHandlerState {
  /** Расaпарсенное значение аттрибута типа предложения. Для simple это SIMPLE. */
  def offerType: OfferType
  /** Опциональный уникальный идентификатор комерческого предложения. */
  def idOpt: Option[String]
  /**
   * Для корректного соотнесения всех вариантов товара с карточкой модели необходимо в описании каждого
   * товарного предложения использовать атрибут group_id. Значение атрибута числовое, задается произвольно.
   * Для всех предложений, которые необходимо отнести к одной карточке товара, должно быть указано одинаковое
   * значение атрибута group_id.
   * [[http://partner.market.yandex.ru/legal/clothes/ Цитата взята отсюда]]
   */
  def groupIdOpt: Option[String]
  /** Значение необязательного флага available, если таков выставлен. */
  def availableOpt: Option[Boolean]
  /** Ссылка на коммерческое предложение на сайте магазина. */
  def urlOpt: Option[String]
  /** Цена коммерческого предложения. */
  def price: Float
  /** Валюта цены коммерческого предложения */
  def currencyId: String
  /** Список категорий магазина, который должен быть непустым. */
  def categoryIds: List[OfferCategoryId]
  /** Необязательная категория в общем дереве категорий яндекс-маркета. */
  def marketCategoryOpt: Option[MarketCategory]
  /** Ссылки на картинки. Их может и не быть. */
  def pictures: List[String]
  /** store: Элемент позволяет указать возможность купить соответствующий товар в розничном магазине. */
  def storeOpt: Option[Boolean]
  /** pickup: Доступность резервирования с самовывозом. */
  def pickupOpt: Option[Boolean]
  /** delivery: Допустима ли доставка для указанного товара? */
  def deliveryOpt: Option[Boolean]
  /** Включена ли доставка в стоимость товара? */
  def deliveryIncludedOpt: Option[Boolean]
  /** Стоимость доставки данного товара в своем регионе. */
  def localDeliveryCostOpt: Option[Float]
  /** Описание товара. */
  def descriptionOpt: Option[String]
  /** Краткие дополнительные сведения по покупке/доставке. */
  def salesNotesOpt: Option[String]
  /** Гарантия производителя: true, false или P1Y2M10DT2H30M. */
  def manufacturerWarrantyOpt: Option[Warranty]
  /** Страна-производитель товара. */
  def countryOfOriginOpt: Option[String]
  /** Можно ли скачать указанный нематериальный товар? */
  def downloadableOpt: Option[Boolean]
  /** Это товар "для взрослых"? */
  def adultOpt: Option[Boolean]
  /** Возрастная категория товара. Есть немного противоречивой документации, но суть в том,
    * что есть аттрибут units и набор допустимых целочисленное значений для указанных единиц измерения. */
  def ageOpt: Option[OfferAge]
  /** Параметры, специфичные для конкретного товара. */
  def params: List[OfferParam]
  def paramsCounter: Int
}


trait SimpleValueT {
  def maxLen: Int
  def myTag: String
  def myAttrs: Attributes
}

