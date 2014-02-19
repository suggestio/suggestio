package io.suggest.ym

import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.{SAXParseException, Locator, Attributes}
import io.suggest.sax.SaxContentHandlerWrapper
import YmConstants._
import io.suggest.util.{MacroLogsImpl, UrlUtil}
import io.suggest.ym.AnyOfferFields.AnyOfferField
import io.suggest.util.MyConfig.CONFIG
import io.suggest.ym.OfferTypes.OfferType
import YmParsers._
import org.joda.time._
import java.net.URL
import io.suggest.ym.model._
import io.suggest.ym.ParamNames.ParamName
import io.suggest.ym.parsers._
import io.suggest.ym.cat.YmCatTranslator
import javax.xml.parsers.SAXParserFactory

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
  val OFFER_PICTURES_MAXCOUNT  = CONFIG.getInt("ym.sax.offer.pictures.count.max") getOrElse 10

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
  val EMAIL_MAXCOUNT = CONFIG.getInt("ym.sax.emails.count.max") getOrElse 3
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
  val OFFER_CATEGORY_IDS_MAX_COUNT = CONFIG.getInt("ym.sax.offer.category_ids.count.max") getOrElse 3

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
  val TOUR_DATES_MAXCOUNT   = CONFIG.getInt("ym.sax.offer.tour_dates.count.max") getOrElse 15

  // event-ticket
  val PLACE_MAXLEN          = CONFIG.getInt("ym.sax.offer.place.len.max")       getOrElse 512
  val HALL_MAXLEN           = CONFIG.getInt("ym.sax.offer.hall.len.max")        getOrElse 128
  val HALL_PART_MAXLEN      = CONFIG.getInt("ym.sax.offer.hall_part.len.max")   getOrElse 64
  val DATE_TIME_MAXLEN      = CONFIG.getInt("ym.sax.offer.dt.len.max")          getOrElse 40

  // param
  val PARAM_VALUE_MAXLEN    = CONFIG.getInt("ym.sax.offer.param.len.max")       getOrElse 200

  /** Регэксп, описывающий повторяющиеся пробелы. */
  private val MANY_SPACES_RE = "[ \\t]{2,}".r


  implicit def saxParseEx2ymParseEx(e: SAXParseException) = new YmSaxException(e)


  /** Собрать и настроить sax parser factory для парсеров, используемых в работе по
    * разбору всех этих кривых yml-файлов. */
  def getSaxFactory: SAXParserFactory = {
    val saxfac = SAXParserFactory.newInstance()
    saxfac.setValidating(false)
    saxfac.setFeature("http://xml.org/sax/features/validation", false)
    saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
    saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    saxfac.setFeature("http://xml.org/sax/features/external-general-entities", false)
    saxfac.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    saxfac
  }

}


import YmlSax._

/** SAX-парсер для прайсов в формате YML. Имеет абстрактные методы, которые зависят от контекста выполнения 
  * парсера: тесты, отладка через web, продакшен и т.д.
  * Реализован в виде трайта, т.к. функционал этого добра может в будущем неограничено расширятся и аргументы
  * типа priceShopId -- это далеко не предел.
  * Абстрактные методы сгруппированы в интерфейсы для возможности реализации их через другие подмешиваемые под-трайты.
  */
trait YmlSax extends DefaultHandler with SaxContentHandlerWrapper with YmSaxErrorHandler with YmSaxResultsHandler {

  /** id магазина. Вписывается в датумы. */
  def priceShopId: Int

  /** Счетчик пройденных офферов. Даже если оффер зафейлился, тут будет инкремент. */
  var totalOffersCounter = 0

  /** Счетчик пройденных магазинов. Как правило, тут после работы хранится число 1. */
  var totalShopCounter = 0

  /** Используемый анализатор имён параметров. */
  implicit protected val paramStrAnalyzer = new YmStringsAnalyzer

  /** Дата старта парсера. Используется для определения длительности работы и для определения каких-либо параметров
    * настоящего времени во вспомогательных парсерах, связанных с датами и временем. */
  val startedAt = DateTime.now()

  /** Экземпляр локатора, который позволяет определить координаты парсера в документе. */
  implicit var locator: Locator = null

  /** Стопка, которая удлиняется по мере погружения в XML теги. Текущий хэндлер наверху. */
  protected var handlersStack: List[MyHandler] = Nil

  /** Вернуть текущий ContentHandler. */
  def contentHandler = handlersStack.head

  /** Начало работы с документом. Нужно выставить дефолтовый обработчик в стек. */
  override def startDocument() {
    become(TopLevelHandler())
  }

  /** Стековое переключение состояние этого ContentHandler'а. */
  def become(h: MyHandler) {
    handlersStack ::= h
  }

  /** Извлечение текущего состояния из стека состояний. */
  def unbecome(): MyHandler = {
    val result = handlersStack.head
    handlersStack = handlersStack.tail
    result
  }

  override def setDocumentLocator(l: Locator) {
    locator = l
  }

  // Ошибки из JAXP или иного парсера надо перенаправлять в errHandler.
  override def fatalError(e: SAXParseException) = handleParsingFatalError(e)
  override def error(e: SAXParseException)      = handleParsingError(e)
  override def warning(e: SAXParseException)    = handleParsingWarn(e)


  //------------------------------------------- FSM States ------------------------------------------------

  /** Родительский класс всех хендлеров. */
  trait MyHandler extends DefaultHandler {
    def myTag: String
    def myAttrs: Attributes

    def getTagNameFor(uri:String, localName:String, qName:String): String = {
      if (localName.isEmpty) {
        if (uri.isEmpty)  qName  else  throw YmOtherException("Internal parser error. Cannot understand tag name: " + qName)
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
      if (tagName == myTag && handlersStack.head == this) {
        handlerFinish(tagName)
        unbecome()
      } else {
        // Надо остановиться и выругаться на неправильно закрытый тег. Наверху оно будет обработано через try-catch
        unexpectedClosingTag(tagName)
      }
    }

    def unexpectedClosingTag(tagName: String) {
      throw YmOtherException(s"Unexpected closing tag: '$tagName', but close-tag '$myTag' expected.")
    }

    /** Фунция вызывается, когда наступает пора завершаться.
      * В этот момент обычно отпавляются в коллектор накопленные данные. */
    def handlerFinish(tagName: String) {}

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
        throw YmOtherException(s"Unexpected tag: '$tagName'. 'yml_catalog' expected.")
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
        case YmlCatalogFields.shop =>
          totalShopCounter += 1
          new ShopHandler(attributes)
      }
      become(nextHandler)
    }
  }


  /**
   * Обработчик содержимого тега shop.
   * @param myAttrs Атрибуты тега shop, которые вроде бы всегда пустые.
   */
  case class ShopHandler(myAttrs: Attributes) extends MyHandler {
    def myTag = YmlCatalogFields.shop.toString

    val shopDatum = new YmShopDatum()
    import shopDatum._

    // Аккамуляторы повторяющихся значений. В commit() происходит сброс оных в датум.
    var emailsAcc: List[String] = Nil
    var emailsAccLen = emailsAcc.size

    var currenciesAcc: List[YmShopCurrency] = Nil
    var currenciesAccLen = currenciesAcc.size

    var categoriesAcc: List[YmShopCategory] = Nil
    var categoriesAccLen = categoriesAcc.size
    var catTransMap: Option[YmCatTranslator.ResultMap_t] = None

    /** Сброс всех аккамуляторов накопленных данных в текущий shop datum. */
    def commit() {
      shopDatum.emails     = emailsAcc.reverse
      shopDatum.currencies = currenciesAcc.reverse
      shopDatum.categories = {
        val catsMap = categoriesAcc
          .map { cat => cat.id -> cat}
          .toMap
        // Нужно обнулить parent_id, которые указывают на несуществующие категории.
        // Данные категорий изменяемы, поэтому проходимся через foreach
        categoriesAcc.foreach { cat =>
          cat.parentIdOpt.foreach { parentId =>
            if (catsMap.get(parentId).isEmpty || parentId == cat.id)
              cat.parentIdOpt = None
          }
        }
        // Отмаппить shop-категории на наше дерево категорий.
        if (!categoriesAcc.isEmpty) {
          val catTranslator = new YmCatTranslator
          try {
            categoriesAcc.foreach { catTranslator learn }
            catTransMap = Some(catTranslator.getResultMap)
          } catch {
            case ex: Exception =>
              handleParsingWarn(YmOtherException("Cannot compile category tree. Offers may be inproperly categorized.", ex))
          }
        }
        // TODO Надо ли устранить циклы в дереве и делать другие проверки тут?
        categoriesAcc.reverse
      }
      shopDatum.commit()
    }

    implicit def getShopContext = new ShopContext(
      datum = shopDatum,
      catTransMap = catTransMap,
      currencies = currenciesAcc.map { c => c.id -> c }.toMap
    )

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
        case ShopFields.email               => new EmailsHandler
        case ShopFields.currencies          => new ShopCurrenciesHandler
        case ShopFields.categories          => new ShopCategoriesHandler
        case ShopFields.store               => new ShopStoreHandler
        case ShopFields.delivery            => new ShopDeliveryHandler
        case ShopFields.pickup              => new ShopPickupHandler
        case ShopFields.deliveryIncluded    => new ShopDeliveryIncludedHandler
        case ShopFields.local_delivery_cost => new ShopLocalDeliveryCostHandler
        case ShopFields.adult               => new ShopAdultHandler
        case ShopFields.offers              =>
          commit()
          new OffersHandler(attributes)
      }
      become(nextHandler)
    }

    trait ShopSVHIgnoreFailure extends SVHIgnoreFailure {
      /** Возника ошибка при разборке значения. */
      override def handleValueFailure(ex: Throwable) {
        handleParsingWarn(YmShopFieldException("Failed to parse value of tag: " + ex.getMessage))
        super.handleValueFailure(ex)
      }

      /** Начало тега, а SVH не ожидает подтегов внутри значения. Игнорим весь тег. */
      override def startTag(tagName: String, attributes: Attributes) {
        handleParsingWarn(YmShopFieldException(s"Unexpected inner tag '$tagName', but value expected."))
        super.startTag(tagName, attributes)
      }
    }

    class ShopNameHandler extends StringHandler {
      def myTag = ShopFields.name.toString
      def maxLen: Int = SHOP_NAME_MAXLEN
      def handleString(s: String) { name = s }
    }

    class ShopCompanyHandler extends StringHandler with ShopSVHIgnoreFailure {
      def myTag = ShopFields.company.toString
      def maxLen: Int = SHOP_COMPANY_MAXLEN
      def handleString(s: String) { company = s }
    }

    class ShopUrlHandler extends UrlHandler with ShopSVHIgnoreFailure {
      def myTag  = ShopFields.url.toString
      override def maxLen: Int = SHOP_URL_MAXLEN
      def handleUrl(_url: String) { url = _url }
      def urlTooLong(s: String) {
        throw YmShopFieldException(s"Shop URL too long. Max length is $maxLen.")
      }
    }

    class PhoneHandler extends StringHandler with ShopSVHIgnoreFailure {
      def myTag = ShopFields.phone.toString
      def maxLen: Int = PHONE_MAXLEN
      def handleString(phoneStr: String) {
        // TODO Парсить телефон, проверять и сохранять в нормальном формате.
        phone = phoneStr
      }
    }

    class EmailsHandler extends StringHandler with ShopSVHIgnoreFailure {
      def myTag = ShopFields.email.toString
      def maxLen: Int = EMAIL_MAXLEN
      def handleString(email: String) {
        if (emailsAccLen < EMAIL_MAXCOUNT) {
          emailsAcc ::= email
          emailsAccLen += 1
        }
      }
    }

    /** Парсер списка валют магазина.
      * [[http://help.yandex.ru/partnermarket/currencies.xml Документация по списку валют магазина]]. */
    class ShopCurrenciesHandler extends MyHandler {
      def myTag = ShopFields.currencies.toString
      def myAttrs = EmptyAttrs

      override def startTag(tagName: String, attributes: Attributes) {
        if ((tagName equalsIgnoreCase TAG_CURRENCY)  &&  currenciesAccLen < SHOP_CURRENCIES_COUNT_MAX) {
          become(new ShopCurrencyHandler(attributes))
          currenciesAccLen += 1
        }
      }
    }

    /** Парсер одной валюты. */
    class ShopCurrencyHandler(attrs: Attributes) extends StringHandler with ShopSVHIgnoreFailure {
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
          currenciesAcc ::= new YmShopCurrency(id, rate=rate, plus=plus)
        }
      }
    }

    /** Парсер списка категорий, которые заданы самим магазином. */
    class ShopCategoriesHandler extends MyHandler {
      def myTag = ShopFields.categories.toString
      override def myAttrs = EmptyAttrs

      override def startTag(tagName: String, attributes: Attributes) {
        if ((tagName equalsIgnoreCase ShopCategoriesFields.category.toString)  &&  categoriesAccLen < SHOP_CAT_COUNT_MAX) {
          categoriesAccLen += 1
          become(new ShopCategoryHandler(attributes))
        }
      }
    }

    class ShopCategoryHandler(attrs: Attributes) extends StringHandler with ShopSVHIgnoreFailure {
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
          val ysc = new YmShopCategory(id=idCur, name=s, parentIdOpt=parentIdOptCur)
          categoriesAcc ::= ysc
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

    class ShopStoreHandler extends ShopBooleanHandler with ShopSVHIgnoreFailure {
      def myTag = ShopFields.store.toString
      def handleBoolean(b: Boolean) { isStore = b }
    }

    class ShopDeliveryHandler extends ShopBooleanHandler with ShopSVHIgnoreFailure {
      def myTag = ShopFields.delivery.toString
      def handleBoolean(b: Boolean) { isDelivery = b }
    }

    class ShopPickupHandler extends ShopBooleanHandler with ShopSVHIgnoreFailure {
      def myTag = ShopFields.pickup.toString
      def handleBoolean(b: Boolean) { isPickup = b }
    }

    class ShopDeliveryIncludedHandler extends ShopBooleanHandler with ShopSVHIgnoreFailure {
      def myTag = ShopFields.deliveryIncluded.toString
      def handleBoolean(b: Boolean) { isDeliveryIncluded = b }
    }

    class ShopLocalDeliveryCostHandler extends FloatHandler with ShopSVHIgnoreFailure {
      def myTag = ShopFields.local_delivery_cost.toString
      def handleFloat(f: Float) { localDeliveryCostOpt = Some(f) }
    }

    /** Обработка магазинного флага adult. Если оператор не смог ввести значение true/face,
      * вероятно он упорот или обе руки были заняты из-за созерцания ассортимента магазина. Следует споткнуться
      * тут, чтобы уточнили или удалили значение из этого поля. */
    class ShopAdultHandler extends ShopBooleanHandler {
      def myTag = ShopFields.adult.toString
      def handleBoolean(b: Boolean) { isAdult = b }
    }
  }


  case class OffersHandler(myAttrs: Attributes = EmptyAttrs)(implicit shopCtx: ShopContext) extends MyHandler {
    def myTag = ShopFields.offers.toString

    override def startTag(tagName: String, attributes: Attributes) {
      // Подготовить маппер категорий на market_category.

      // Включить обработчик offer-тега.
      val nextHandler: MyHandler = OffersFields.withName(tagName) match {
        case OffersFields.offer =>
          totalOffersCounter += 1
          AnyOfferHandler(attributes)
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
    def apply(attrs: Attributes)(implicit shopCtx: ShopContext): AnyOfferHandler = {
      val offerTypeRaw = attrs.getValue(ATTR_TYPE)
      val offerType = OfferTypes.withRawName(offerTypeRaw)
      offerType match {
        case OfferTypes.Simple          => new SimpleOfferHandler(attrs)
        case OfferTypes.VendorModel     => new VendorModelOfferHandler(attrs)
        case OfferTypes.Book            => new BookOfferHandler(attrs)
        case OfferTypes.AudioBook       => new AudioBookOfferHandler(attrs)
        case OfferTypes.ArtistTitle     => new ArtistTitleOfferHandler(attrs)
        case OfferTypes.Tour            => new TourOfferHandler(attrs)
        case OfferTypes.EventTicket     => new EventTicketOfferHandler(attrs)
      }
    }
  }


  /** Базовый класс для обработчиков коммерческих предложений.
    * Разные типы предложений имеют между собой много общего, поэтому основная часть
    * логики разборки предложений находится здесь. */
  trait AnyOfferHandler extends MyHandler {
    def myTag = OffersFields.offer.toString

    implicit val shopCtx: ShopContext

    implicit val offerDatum = new YmOfferDatum()
    import offerDatum._

    def shopCurrencies = shopCtx.currencies

    def myOfferType: OfferType

    // Заливаем данные аттрибутов в датум сразу в конструкторе.
    shopOfferIdOpt       = Option(myAttrs.getValue(ATTR_ID)).map(_.trim)
    groupIdOpt  = Option(myAttrs.getValue(ATTR_GROUP_ID)).map(_.trim)
    offerType   = myOfferType
    shopMeta    = shopCtx.datum
    shopId      = priceShopId

    isAvailable = {
      val maybeAvailable = myAttrs.getValue(ATTR_AVAILABLE)
      if (maybeAvailable == null) {
        true
      } else {
        val parseResult = parse(BOOL_PARSER, maybeAvailable)
        if (parseResult.successful) {
          parseResult.get
        } else {
          throw YmOfferAttributeException(ATTR_AVAILABLE, "Cannot understand value: " + maybeAvailable)
        }
      }
    }

    /** Если есть ошибки парсинга, то они могут переопределеить эту переменную,
      * чтобы избежать обработки последующих данных. */
    protected var isSkipping: Boolean = false

    /** Возникла какая-то проблема, нивелирующая весь труд по парсингу этого оффера. */
    protected def skipCurrentOffer(reason: YmParserException) {
      isSkipping = true
      handleParsingError(reason)
    }

    /** Аккамулятор списка категорий магазина, который должен быть непустым. */
    var categoryIdsAcc: List[OfferCategoryId] = Nil
    var categoryIdsAccLen = categoryIdsAcc.size

    /** Ссылки на картинки. Их может и не быть. */
    var picturesAcc: List[String] = Nil
    var picturesAccLen = picturesAcc.size

    var _marketCatPathOpt: Option[Seq[String]] = None


    /** Тут базовый комбинируемый обработчик полей комерческого предложения.
      * Тут, в trait'е, нужно использовать def вместо val, т.к. это точно будет переопределено в под-классах. */
    def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = {
      case (f, attrs) if AnyOfferFields.isIgnoredField(f)   => MyDummyHandler(f.toString, attrs)
      case (AnyOfferFields.url, _)                          => new OfferUrlHandler
      // buyurl ignored
      case (AnyOfferFields.price, _)                        => new PriceHandler
      case (AnyOfferFields.wprice, _)                       => ???
      // wprice ignored
      case (AnyOfferFields.currencyId, _)                   => new CurrencyIdHandler
      // xCategory ignored
      case (AnyOfferFields.categoryId, attrs)               => new CategoryIdHandler(attrs)
      case (AnyOfferFields.market_category, _)              => new MarketCategoryHandler
      case (AnyOfferFields.picture, _)                      => new PictureUrlHandler
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
      case (AnyOfferFields.param, attrs)                    => getParamHandlerOrIgnore(attrs)
      // barcode: ignored
    }

    /** Начинается поле оффера. В зависимости от тега, выбрать обработчик. */
    override def startTag(tagName: String, attributes: Attributes) {
      val offerField = AnyOfferFields.withName(tagName)
      val handler = if (!isSkipping) {
        getFieldsHandler(offerField, attributes)
      } else {
        MyDummyHandler(tagName, attributes)
      }
      become(handler)
    }

    /** Сброс накопленных аккамуляторов в датум. При override, нужно дергать super.commit() в конце, а не в начале. */
    def commit() {
      categoryIds = categoryIdsAcc.map(_.categoryId)
      pictures = picturesAcc.reverse
      // PAYLOAD внутри датума тоже требует отдельного коммита.
      offerDatum.commit()
      marketCategoryPath = _marketCatPathOpt
    }

    /** Фунция вызывается, когда наступает пора завершаться. В этот момент обычно отпавляются в коллектор накопленные данные. */
    override def handlerFinish(tagName: String) {
      super.handlerFinish(tagName)
      if (!isSkipping) {
        commit()
        // И вот та команда, ради которой написаны все остальные строки:
        handleOfferDatum(offerDatum, shopCtx.datum)
      }
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


    trait SVHOfferFailureControl extends SimpleValueHandler {
      override def wrapValueFailureException(ex: Throwable) = {
        YmOfferFieldException("Cannot parse offer field.", ex)
      }
    }

    trait SkipOfferOnFailure extends SVHOfferFailureControl {
      override def handleValueFailure(ex: Throwable) {
        skipCurrentOffer(maybeWrapFailureException(ex))
      }
    }

    trait IgnoreFieldFailure extends SVHOfferFailureControl with SVHIgnoreFailure {
      /** Начало тега, а SVH не ожидает подтегов внутри значения. Игнорим весь тег. */
      override def startTag(tagName: String, attributes: Attributes) {
        handleParsingWarn(YmOtherException(s"Unexpected tag $tagName, but value expected."))
        super.startTag(tagName, attributes)
      }

      override def handleValueFailure(ex: Throwable) {
        handleParsingWarn(maybeWrapFailureException(ex))
        super.handleValueFailure(ex)
      }
    }

    /** Обработчик тега url, вставляющий в состояние результирующую ссылку. */
    class OfferUrlHandler extends OfferAnyUrlHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.url.toString
      override def maxLen: Int = OFFER_URL_MAXLEN
      def handleUrl(_url: String) { url = _url }
    }

    /** Обработчик старой цены, которая в рамках yml задаётся как wprice.
      * Цитата из [[http://discontmart.ru/partners/]]:
      * | ... поддерживаем XML-файлы стандарта YML (Yandex Market Language).
      * | Для описания причины уценки используется элемент  < additional > с параметром 'name'.
      * | Пример описания причины уценки: < additional name="Причина уценки" >Описание< /additional >
      * | Для расчета скидки, необходимо в элемент < wprice > передавать цену товара до уценки.
      */
    class WPriceHandler extends FloatHandler with IgnoreFieldFailure {
      override def myTag: String = AnyOfferFields.wprice.toString
      override def handleFloat(f: Float) {
        oldPrices = Seq(f)
      }
    }

    /** Обработчик количественной цены товара/услуги. */
    class PriceHandler extends FloatHandler with SkipOfferOnFailure {
      def myTag: String = AnyOfferFields.price.toString
      def handleFloat(f: Float) { price = f }
    }

    /** Обработчик валюты цены предложения. */
    class CurrencyIdHandler extends StringHandler with SkipOfferOnFailure {
      def myTag = AnyOfferFields.currencyId.toString
      override def maxLen: Int = SHOP_CURRENCY_ID_MAXLEN
      def handleString(s: String) {
        // Проверяем валюту по списку валют магазина.
        if (!(shopCurrencies contains s))
          skipCurrentOffer( YmOfferFieldException(s"Unexpected currencyId: '$s'. Defined shop's currencies are: ${shopCurrencies.keys.mkString(", ")}") )
        if (hadOverflow)
          skipCurrentOffer( YmOfferFieldException(s"Too long currency id. Max length is $maxLen.") )
        currencyId = s
      }
    }

    /** Чтение поля categoryId, которое может иметь устаревший аттрибут type. */
    class CategoryIdHandler(attrs: Attributes) extends StringHandler with SkipOfferOnFailure {
      def myTag: String = AnyOfferFields.categoryId.toString

      /** Максимальная кол-во байт, которые будут сохранены в аккамулятор. */
      override def maxLen: Int = SHOP_CAT_ID_LEN_MAX
      def handleString(catId: String) {
        if (categoryIdsAccLen < OFFER_CATEGORY_IDS_MAX_COUNT) {
          val catIdType = Option(attrs.getValue(ATTR_TYPE))
            .map { OfferCategoryIdTypes.withName }
            .getOrElse { OfferCategoryIdTypes.default }
          // Из-за дефицита документации по устаревшим фичам, этот момент остается не яснен.
          if (_marketCatPathOpt.isEmpty  &&  shopCtx.catTransMap.isDefined) {
            shopCtx.catTransMap.get.get(catId) match {
              case Some(cat)  => _marketCatPathOpt = Some(cat.path)
              case None       => handleParsingWarn(YmOfferFieldException("Unknown category id: " + catId))
            }
          }
          categoryIdsAcc ::= OfferCategoryId(catId, catIdType)
          categoryIdsAccLen += 1
        } else {
          handleParsingWarn(YmOfferFieldException("Too many categories."))
        }
      }
    }

    /** Парсер необязательной marketCategory. Она только одна. Её нужно парсить согласно
      * [[http://help.yandex.ru/partnermarket/docs/market_categories.xls таблице категорий маркета]].
      * Пример значения: Одежда/Женская одежда/Верхняя одежда/Куртки
      */
    class MarketCategoryHandler extends StringHandler with SkipOfferOnFailure {
      def myTag = AnyOfferFields.market_category.toString
      override def maxLen: Int = MARKET_CAT_VALUE_MAXLEN
      def handleString(s: String) {
        // При превышении длины строки, нельзя выставлять категорию
        if (hadOverflow) {
          // TODO Категорию маркета надо парсить и выверять согласно вышеуказанной доке. Все токены пути должны быть выставлены строго согласно таблице категорий.
          val catPath = MARKET_CATEGORY_PATH_SPLIT_RE.split(s).toList.filter(!_.isEmpty)
          _marketCatPathOpt = Some(catPath)
        } else {
          throw YmOfferFieldException("Value too long.")
        }
      }
    }

    /** Аккамулирование ссылок на картинки в pictures. Картинок может быть много, и хотя бы одна почти всегда имеется,
      * поэтому блокируем многократное конструирование этого объекта. */
    class PictureUrlHandler extends OfferAnyUrlHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.picture.toString
      override def maxLen: Int = OFFER_PICTURE_URL_MAXLEN
      def handleUrl(pictureUrl: String) {
        if (picturesAccLen < OFFER_PICTURES_MAXCOUNT) {
          picturesAcc ::= pictureUrl
          picturesAccLen += 1
        } else {
          handleParsingWarn(YmOfferFieldException("Too many pictures. Some pictures ignored."))
        }
      }
    }

    /** store: Парсим поле store, которое говорит, можно ли купить этот товар в магазине. */
    class OfferStoreHandler extends OfferBooleanHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.store.toString
      def handleBoolean(b: Boolean) {
        isStore = b
      }
    }

    /** pickup: Парсим поле, говорящее нам о том, возможно ли зарезервировать и самовывезти указанный товар. */
    class OfferPickupHandler extends OfferBooleanHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.pickup.toString
      def handleBoolean(b: Boolean) {
        isPickup = b
      }
    }

    /** delivery: Поле указывает, возможна ли доставка указанного товара? */
    class OfferDeliveryHandler extends OfferBooleanHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.delivery.toString
      def handleBoolean(b: Boolean) {
        isDelivery = b
      }
    }

    /** deliveryIncluded: Включена ли доставка до покупателя в стоимость товара? Если тег задан, то включена. */
    class OfferDeliveryIncludedHandler extends BooleanTagFlagHandler {
      def myTag = AnyOfferFields.deliveryIncluded.toString
      def handleTrue() {
        isDeliveryIncluded = true
      }
    }

    /** local_delivery_cost: Поле описывает стоимость доставки с своём регионе. */
    class OfferLocalDeliveryCostHandler extends FloatHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.local_delivery_cost.toString
      def handleFloat(f: Float) {
        localDeliveryCostOpt = Some(f)
      }
    }

    /** description: Поле с описанием товара. Возможно, multiline-поле. */
    class DescriptionHandler extends StringHandler with IgnoreFieldFailure {
      // TODO Нужно убедится, что переносы строк отрабатываются корректно. Возможно, их надо подхватывать через ignorableWhitespace().
      def myTag = AnyOfferFields.description.toString
      override def maxLen: Int = DESCRIPTION_LEN_MAX
      override def ellipsis: String = DESCRIPTION_ELLIPSIS

      def handleString(desc: String) {
        description = desc
      }
    }

    /** sales_notes: Какие-то доп.данные по товару, которые НЕ индексируются, но отображаются юзеру.
      * Например "Минимальное кол-во единиц в заказе: 4 шт.".
      * Допустимая длина текста в элементе — 50 символов. */
    class SalesNotesHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.sales_notes.toString
      override def ellipsis: String = SALES_NOTES_ELLIPSIS
      override def maxLen: Int = SALES_NOTES_MAXLEN

      def handleString(_salesNotes: String) {
        salesNotes = _salesNotes
      }
    }

    /** manufacturer_warranty: Поле с информацией о гарантии производителя. */
    class ManufacturerWarrantyHandler extends WarrantyHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.manufacturer_warranty.toString
      def handleWarranty(wv: Warranty) {
        manufacturerWarranty = wv
      }
    }

    /** Страна-производитель из [[http://partner.market.yandex.ru/pages/help/Countries.pdf таблицы стран маркета]].
      * Например "Бразилия". */
    class CountryOfOriginHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.country_of_origin.toString
      override def maxLen: Int = COUNTRY_MAXLEN
      def handleString(s: String) {
        // TODO Нужно брать список стран, проверять по нему страну (желательно через триграммы), с поддержкой стран на иных языках.
        countryOfOrigin = s
      }
    }

    /** downloadable предназначен для обозначения товара, который можно скачать. Нормальной документации маловато,
      * поэтому считаем что тут boolean. */
    class DownloadableHandler extends OfferBooleanHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.downloadable.toString
      def handleBoolean(b: Boolean) {
        isDownloadable = b
      }
    }

    /** adult: обязателен для обозначения товара, имеющего отношение к удовлетворению сексуальных потребностей, либо
      * иным образом эксплуатирующего интерес к сексу.
      * [[http://help.yandex.ru/partnermarket/adult.xml Документация по тегу adult]]. */
    class OfferAdultHandler extends OfferBooleanHandler with SkipOfferOnFailure {
      def myTag = AnyOfferFields.adult.toString
      def handleBoolean(b: Boolean) {
        isAdult = b
      }
    }

    /** Возрастая категория товара. Единицы измерения заданы в аттрибутах. */
    class AgeHandler(attrs: Attributes) extends IntHandler with SkipOfferOnFailure {
      def myTag = AnyOfferFields.age.toString
      def handleInt(i: Int) {
        val unitV1 = Option(attrs.getValue(ATTR_UNIT)) map { unitV =>
          OfferAgeUnits.withName(unitV.trim.toLowerCase)
        } getOrElse OfferAgeUnits.Year
        // Проверить значения, чтобы соответствовали интервалам допустимых значений.
        val possibleValues = unitV1 match {
          case OfferAgeUnits.Month => OFFER_AGE_MONTH_VALUES
          case OfferAgeUnits.Year  => OFFER_AGE_YEAR_VALUES
        }
        if (!(possibleValues contains i)) {
          throw YmOfferFieldException(s"Invalid value for unit=$unitV1: $i ; Possible values are: ${possibleValues.mkString(", ")}")
        }
        ageOpt = Some(new YmOfferAge(units = unitV1, value = i))
      }
    }


    /** Масса может задаваться через param "вес" с указанием единиц измерения. Тут обработчик этого дела. */
    class WeightParamHandler(attrs: Attributes) extends FloatHandler with IgnoreFieldFailure {
      override def myTag: String = AnyOfferFields.param.toString
      override def maxLen: Int = PARAM_VALUE_MAXLEN
      def handleFloat(f: Float) {
        Option(attrs.getValue(ATTR_UNIT))
          .map { _.trim }
          .flatMap { str => if (str.isEmpty)  None  else  Some(str) }
          .flatMap { unitsStr =>
            val pmResult = implicitly[MassUnitParserAn].parseMassUnit(unitsStr)
            if (pmResult.successful)  Some(pmResult.get)  else  None
          }
          .foreach { units =>
            weightKgOpt = Some(MassUnits.toKg(f, units))
          }
      }
    }


    /** Partial-фунцкия, которая переопределяется в под-классах, чтобы добавить обработчиков. */
    def handleParam: PartialFunction[(ParamName, Attributes), MyHandler] = {
      case (ParamNames.Weight, attrs) => new WeightParamHandler(attrs)
    }

    /** Результирующая функция обработки параметров. */
    private lazy val paramHandler: PartialFunction[(ParamName, Attributes), MyHandler] = {
      handleParam orElse {
        case (pn, attrs) => MyDummyHandler(pn.toString, attrs)     // TODO Нужно репортить это всё наверх
      }
    }

    /**
     * Предложить обработчик указанного param-тега или заглушку, если параметр неизвестен.
     * @param attrs Аттрибуты тега param.
     * @return Обработчик тега.
     */
    def getParamHandlerOrIgnore(attrs: Attributes): MyHandler = {
      val paramName = attrs.getValue(ATTR_NAME)
      if (paramName == null || paramName.isEmpty) {
        MyDummyHandler(AnyOfferFields.param.toString, attrs)
        // TODO Нужен механизм репортинга наверх, чтобы на веб-морде могли увидеть сообщение о проблеме.
      } else {
        val nameParseResult = implicitly[ParamNameParserAn].parseParamName(paramName)
        if (nameParseResult.successful) {
          paramHandler(nameParseResult.get, attrs)
        } else {
          MyDummyHandler(AnyOfferFields.param.toString, attrs) // TODO см.выше
        }
      }
    }
  }


  trait VendorInfoH extends AnyOfferHandler {
    import offerDatum._

    /** Тут базовый комбинируемый обработчик полей комерческого предложения.
      * Тут, в trait'е, нужно использовать def вместо val, т.к. это точно будет переопределено в под-классах. */
    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = {
      super.getFieldsHandler orElse {
        case (AnyOfferFields.vendorCode, _) => new VendorCodeHandler
      }
    }

    /** vendor: поле содержит производителя товара. В зависимости от типа товара, он может быть обязательным или нет. */
    abstract class VendorHandler extends StringHandler {
      def myTag = AnyOfferFields.vendor.toString
      def maxLen: Int = VENDOR_MAXLEN
      def handleString(s: String) {
        // Базовая нормализация нужна, т.к. это поле не отображается юзеру и используется для группировки по брендам.
        // TODO Надо бы нормализовывать с синонимами, чтобы HP и Hewlett-packard были эквивалентны.
        vendor = s
      }
    }

    /** vendorCode: Номер изделия по мнению производителя. Не индексируется, не используется для группировки. */
    class VendorCodeHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.vendorCode.toString
      def maxLen: Int = VENDOR_CODE_MAXLEN
      def handleString(s: String) {
        vendorCode = s
      }
    }
  }


  /** Большинство офферов имеют поле name. Тут поддержка этого поля. */
  trait OfferNameH extends AnyOfferHandler {
    import offerDatum._

    /** Обработчик полей коммерческого предложения, который также умеет name. */
    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = {
      super.getFieldsHandler orElse {
        case (AnyOfferFields.name, _) => new NameHandler
      }
    }

    /** name: Поле описывает название комерческого предложения в зависимости от контекста. */
    class NameHandler extends StringHandler with SkipOfferOnFailure {
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
  case class SimpleOfferHandler(myAttrs: Attributes)(implicit val shopCtx: ShopContext) extends VendorInfoH with OfferNameH {
    def myOfferType = OfferTypes.Simple

    /** Обработчик полей коммерческого предложения, который также умеет name. */
    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      // vendor тут не обязательный, согласно dtd.
      case (AnyOfferFields.vendor, _)  => new VendorHandler with IgnoreFieldFailure
    }
  }

  /** Произвольный товар (vendor.model)
    * Этот тип описания является наиболее удобным и универсальным, он рекомендован для описания товаров из
    * большинства категорий Яндекс.Маркета.
    * Доп.поля: typePrefix, [vendor, vendorCode], model, provider, tarifPlan. */
  case class VendorModelOfferHandler(myAttrs: Attributes)(implicit val shopCtx: ShopContext) extends VendorInfoH {
    import offerDatum._

    def myOfferType = OfferTypes.VendorModel

    /** Список id рекомендуемых товаров к этому товару. */
    var recIdsAcc: List[String] = Nil
    var recIdsAccLen = recIdsAcc.size

    /** Сброс накопленных аккамуляторов в датум. */
    override def commit() {
      recIds = recIdsAcc
      super.commit()
    }

    /** Дополненный для vendor.model обработчик полей комерческого предложения. */
    override val getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (f @ (AnyOfferFields.tarifplan | AnyOfferFields.provider | AnyOfferFields.cpa), attrs) =>
        MyDummyHandler(f.toString, attrs)
      case (AnyOfferFields.typePrefix, _)           => new TypePrefixHandler
      case (AnyOfferFields.model, _)                => new ModelHandler
      case (AnyOfferFields.seller_warranty, _)      => new SellerWarrantyHandler
      case (AnyOfferFields.rec, _)                  => new RecommendedOffersHandler
      case (AnyOfferFields.expiry, _)               => new ExpiryHandler
      case (AnyOfferFields.weight, _)               => new WeightHandler
      case (AnyOfferFields.dimensions, _)           => new DimensionsHandler
      case (AnyOfferFields.vendor, _)               => new VendorHandler with SkipOfferOnFailure
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
    class TypePrefixHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.typePrefix.toString
      override def maxLen: Int = OFFER_TYPE_PREFIX_MAXLEN
      def handleString(s: String) {
        typePrefix = s
      }
    }

    /** model: Некая модель товара, т.е. правая часть названия. Например "Женская куртка" или "Deskjet D31337". */
    class ModelHandler extends StringHandler with SkipOfferOnFailure {
      def myTag = AnyOfferFields.model.toString
      def maxLen: Int = OFFER_MODEL_MAXLEN
      def handleString(s: String) {
        model = s
      }
    }

    /** Поле, описывающее гарантию от продавца. */
    class SellerWarrantyHandler extends WarrantyHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.seller_warranty.toString
      def handleWarranty(wv: Warranty) {
        sellerWarrantyOpt = wv
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
    class RecommendedOffersHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.rec.toString
      override def maxLen: Int = REC_LIST_MAX_CHARLEN
      def handleString(s: String) {
        var recIds: Seq[String] = REC_LIST_SPLIT_RE.split(s).toSeq
        while (recIdsAccLen < REC_LIST_MAX_LEN && !recIds.isEmpty) {
          recIdsAcc ::= recIds.head
          recIdsAccLen += 1
          recIds = recIds.tail
        }
      }
    }

    /** expiry: Истечение срока годности/службы или чего-то подобного. Может быть как период, так и дата. */
    class ExpiryHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.expiry.toString
      override def maxLen: Int = 30
      def handleString(s: String) {
        if (parse(EXPIRY_PARSER, s).successful) {
          expiryOptRaw = s
        } else {
          throw YmOfferFieldException(s"Cannot understand expiration date or period: '$s'.")
        }
      }
    }

    /** weight: Обработчик поля, содержащего полный вес товара. */
    class WeightHandler extends FloatHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.weight.toString
      def handleFloat(f: Float) {
        weightKgOpt = Some(f)
      }
    }

    /** Обработчик размерностей товара в формате: длина/ширина/высота. */
    class DimensionsHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.dimensions.toString
      override def maxLen: Int = OFFER_DIMENSIONS_MAXLEN
      def handleString(s: String) {
        if (parse(DIMENSIONS_PARSER, s).successful) {
          dimensionsRaw = s
        } else {
          throw YmOfferFieldException(s"Cannot understand this dimensions: '$s'. Please use 'length/width/height' (centimeters) format.")
        }
      }
    }
  }

  /** Добавка с годом издания указанной продукции народного творчества. */
  trait OfferYearH extends AnyOfferHandler {
    import offerDatum._

    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.year, _) => new YearHandler
    }

    class YearHandler extends IntHandler with IgnoreFieldFailure {
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
  trait AnyBookOfferHandler extends OfferYearH with OfferNameH {
    import offerDatum._

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

    class AuthorHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.author.toString
      override def maxLen: Int = OFFER_AUTHOR_MAXLEN
      def handleString(s: String) {
        author = s
      }
    }

    class PublisherHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.publisher.toString
      override def maxLen: Int = OFFER_PUBLISHER_MAXLEN
      def handleString(s: String) {
        publisher = s
      }
    }

    class SeriesHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.series.toString
      override def maxLen: Int = OFFER_SERIES_MAXLEN
      def handleString(s: String) {
        series = s
      }
    }

    class ISBNHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.ISBN.toString
      override def maxLen: Int = OFFER_ISBN_MAXLEN
      def handleString(s: String) {
        // TODO В конце номера контрольная цифра, её следует проверять. Также, следует выверять весь остальной формат.
        isbn = s
      }
    }

    class VolumesCountHandler extends IntHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.volume.toString
      def handleInt(i: Int) {
        volumesCount = Some(i)
      }
    }

    class VolumesPartHandler extends IntHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.part.toString
      def handleInt(i: Int) {
        volume = Some(i)
      }
    }

    class LanguageHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.language.toString
      override def maxLen: Int = OFFER_LANGUAGE_MAXLEN
      def handleString(s: String) {
        language = s
      }
    }

    class TableOfContentsHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.table_of_contents.toString
      override def maxLen: Int = OFFER_TOC_MAXLEN
      def handleString(s: String) {
        tableOfContents = s
      }
    }
  }

  /**
   * Обработчик для типа book, т.е. бумажных книженций.
   * @param myAttrs Аттрибуты оффера.
   * @param shopCtx Контекст текущего магазина.
   */
  case class BookOfferHandler(myAttrs: Attributes)(implicit val shopCtx:ShopContext) extends AnyBookOfferHandler {
    import offerDatum._
    def myOfferType = OfferTypes.Book

    override val getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.binding, _)      => new BindingHandler
      case (AnyOfferFields.page_extent, _)  => new PageExtentHandler
    }

    /** Переплёт. */
    class BindingHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.binding.toString
      override def maxLen: Int = OFFER_BINDING_MAXLEN
      def handleString(s: String) {
        binding = s
      }
    }

    /** Кол-во страниц. */
    class PageExtentHandler extends IntHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.page_extent.toString
      def handleInt(i: Int) {
        pageExtent = Some(i)
      }
    }
  }


  /**
   * Аудиокнига.
   * @param myAttrs Атрибуты оффера.
   * @param shopCtx Контекст текущего магазина.
   */
  case class AudioBookOfferHandler(myAttrs: Attributes)(implicit val shopCtx:ShopContext) extends AnyBookOfferHandler {
    import offerDatum._
    def myOfferType = OfferTypes.AudioBook

    override val getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.performed_by, _)     => new PerformedByHandler
      case (AnyOfferFields.performance_type, _) => new PerformanceTypeHandler
      case (AnyOfferFields.storage, _)          => new StorageHandler
      case (AnyOfferFields.format, _)           => new FormatHandler
      case (AnyOfferFields.recording_length, _) => new RecordingLenHandler
    }

    class PerformedByHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.performed_by.toString
      override def maxLen: Int = OFFER_PERFORMED_BY_MAXLEN
      def handleString(s: String) {
        performedBy = s
      }
    }

    class PerformanceTypeHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.performance_type.toString
      override def maxLen: Int = OFFER_PERFORMANCE_TYPE_MAXLEN
      def handleString(s: String) {
        performanceType = s
      }
    }

    class StorageHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.storage.toString
      override def maxLen: Int = OFFER_STORAGE_MAXLEN
      def handleString(s: String) {
        storage = s
      }
    }

    class FormatHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.format.toString
      def maxLen: Int = OFFER_FORMAT_MAXLEN
      def handleString(s: String) {
        format = s
      }
    }

    class RecordingLenHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.recording_length.toString
      override def maxLen: Int = OFFER_REC_LEN_MAXLEN
      def handleString(s: String) {
        if (parse(RECORDING_LEN_PARSER, s).successful) {
          recordingLenRaw = s
        } else {
          throw YmOfferFieldException("Recording length is unreadable. Please use 'mm.ss' format.")
        }
      }
    }
  }


  trait OfferCountryOptH extends AnyOfferHandler {
    import offerDatum._

    /** Тут базовый комбинируемый обработчик полей комерческого предложения.
      * Тут, в trait'е, нужно использовать def вместо val, т.к. это точно будет переопределено в под-классах. */
    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.country, _) => new CountryHandler
    }

    class CountryHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.country.toString
      override def maxLen: Int = COUNTRY_MAXLEN
      def handleString(s: String) {
        // TODO Нужно проверять страну по списку оных.
        country = s
      }
    }
  }


  /**
   * Обработчик artist.title-офферов, содержащих аудио/видео хлам.
   * @param myAttrs Аттрибуты этого оффера.
   * @param shopCtx Контекст текущего магазина.
   */
  case class ArtistTitleOfferHandler(myAttrs: Attributes)(implicit val shopCtx:ShopContext) extends OfferCountryOptH with OfferYearH {
    import offerDatum._

    def myOfferType = OfferTypes.ArtistTitle

    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.artist, _)       => new ArtistHandler
      case (AnyOfferFields.title, _)        => new TitleHandler
      case (AnyOfferFields.media, _)        => new MediaHandler
      case (AnyOfferFields.starring, _)     => new StarringHandler
      case (AnyOfferFields.director, _)     => new DirectorHandler
      case (AnyOfferFields.originalName, _) => new OriginalNameHandler
    }

    class ArtistHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.artist.toString
      override def maxLen: Int = OFFER_ARTIST_MAXLEN
      def handleString(s: String) {
        artist = s
      }
    }

    class TitleHandler extends StringHandler with SkipOfferOnFailure {
      def myTag = AnyOfferFields.title.toString
      override def maxLen: Int = OFFER_TITLE_MAXLEN
      def handleString(s: String) {
        title = s
      }
    }

    class MediaHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.media.toString
      override def maxLen: Int = OFFER_MEDIA_MAXLEN
      def handleString(s: String) {
        media = s
      }
    }

    class StarringHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.starring.toString
      override def maxLen: Int = OFFER_STARRING_MAXLEN
      def handleString(s: String) {
        starring = s
      }
    }

    class DirectorHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.director.toString
      override def maxLen: Int = OFFER_DIRECTOR_MAXLEN
      def handleString(s: String) {
        director = s
      }
    }

    class OriginalNameHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.originalName.toString
      override def maxLen: Int = OFFER_NAME_MAXLEN
      def handleString(s: String) {
        originalName = s
      }
    }
  }


  /**
   * Предложени тур-путёвки в далёкие края.
   * @param myAttrs Аттрибуты тега.
   * @param shopCtx Контекст текущего магазина.
   */
  case class TourOfferHandler(myAttrs: Attributes)(implicit val shopCtx:ShopContext) extends OfferCountryOptH with OfferNameH {
    import offerDatum._

    def myOfferType = OfferTypes.Tour

    /** Аккамулятор дат заездов. */
    var tourDatesAcc: List[DateTime] = Nil
    var tourDatesAccLen = tourDatesAcc.size

    /** Сброс накопленных аккамуляторов в датум. При override, нужно дергать super.commit() в конце, а не в начале. */
    override def commit() {
      tourDates = tourDatesAcc.reverse
      super.commit()
    }

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

    class WorldRegionHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.worldRegion.toString
      override def maxLen: Int = WORLD_REGION_MAXLEN
      def handleString(s: String) {
        worldRegion = s
      }
    }

    class RegionHandler extends StringHandler with IgnoreFieldFailure {
      override def maxLen: Int = REGION_MAXLEN
      def myTag = AnyOfferFields.region.toString
      def handleString(s: String) {
        region = s
      }
    }

    class DaysHandler extends IntHandler with SkipOfferOnFailure {
      def myTag = AnyOfferFields.days.toString
      def handleInt(i: Int) {
        days = Some(i)
      }
    }

    class TourDatesHandler extends DateTimeHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.dataTour.toString
      def handleDateTime(dt: DateTime) {
        if (tourDatesAccLen < TOUR_DATES_MAXCOUNT) {
          tourDatesAcc ::= dt
          tourDatesAccLen += 1
        }
      }
    }

    class HotelStarsHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.hotel_stars.toString
      override def maxLen: Int = HOTEL_STARS_MAXLEN
      def handleString(s: String) {
        val parseResult = parse(HOTEL_STARS_PARSER, s)
        if (parseResult.successful) {
          hotelStars = parseResult.get
        } else {
          throw YmOfferFieldException("Cannot understand hotel_stars value: " + s)
        }
      }
    }

    class RoomHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.room.toString
      def maxLen: Int = ROOM_TYPE_MAXLEN
      def handleString(s: String) {
        val parseResult = parse(HOTEL_ROOM_PARSER, s)
        if (parseResult.successful) {
          hotelRoom = parseResult.get
        } else {
          throw YmOfferFieldException("Cannot understand 'room' field value: " + s)
        }
      }
    }

    class MealHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.meal.toString
      def maxLen: Int = MEAL_MAXLEN
      def handleString(s: String) {
        val parseResult = parse(HOTEL_MEAL_PARSER, s)
        if (parseResult.successful) {
          hotelMeal = parseResult.get
        } else {
          throw YmOfferFieldException("Cannot understand 'meal' field value: " + s)
        }
      }
    }

    class IncludedHandler extends StringHandler with SkipOfferOnFailure {
      def myTag: String = AnyOfferFields.included.toString
      def maxLen: Int = TOUR_INCLUDED_MAXLEN
      def handleString(s: String) {
        tourIncluded = s
      }
    }

    class TransportHandler extends StringHandler with SkipOfferOnFailure {
      def myTag = AnyOfferFields.transport.toString
      def maxLen: Int = TRANSPORT_MAXLEN
      def handleString(s: String) {
        tourTransport = s
      }
    }
  }


  /**
   * Билет на мероприятие.
   * @param myAttrs Аттрибуты тега оффера.
   * @param shopCtx Контекст текущего магазина.
   */
  case class EventTicketOfferHandler(myAttrs: Attributes)(implicit val shopCtx:ShopContext) extends OfferNameH {
    import offerDatum._

    def myOfferType = OfferTypes.EventTicket

    /** Обработчик полей коммерческого предложения, который также умеет name. */
    override def getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = super.getFieldsHandler orElse {
      case (AnyOfferFields.place, _)        => new PlaceHandler
      case (AnyOfferFields.hall, attrs)     => new HallPlanHandler(attrs)
      case (AnyOfferFields.hall_part, _)    => new HallPartHandler
      case (AnyOfferFields.date, _)         => new EventDateHandler
      case (AnyOfferFields.is_premiere, _)  => new IsPremiereHandler
      case (AnyOfferFields.is_kids, _)      => new IsKidsHandler
    }

    class PlaceHandler extends StringHandler with SkipOfferOnFailure {
      def myTag = AnyOfferFields.place.toString
      def maxLen: Int = PLACE_MAXLEN
      def handleString(s: String) {
        // TODO Нужна география.
        etPlace = s
      }
    }

    class HallPlanHandler(attrs: Attributes) extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.hall.toString
      def maxLen: Int = HALL_MAXLEN
      def handleString(s: String) {
        if (!s.isEmpty) {
          etHall = s
        }
        // В аттрибуте тега может быть задана ссылка на план зала.
        Option(attrs.getValue(ATTR_PLAN))
          .map(_.trim)
          .filter { maybeUrl =>
            try {
              new URL(maybeUrl)
              true
            } catch {
              case ex: Exception => false
            }
          } foreach {
            url  =>  etHallPlanUrl = UrlUtil.normalize(url)
          }
      }
    }

    class HallPartHandler extends StringHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.hall_part.toString
      def maxLen: Int = HALL_PART_MAXLEN
      def handleString(s: String) {
        etHallPart = s
      }
    }

    class EventDateHandler extends DateTimeHandler with SkipOfferOnFailure {
      def myTag = AnyOfferFields.date.toString
      def handleDateTime(dt: DateTime) {
        etDate = dt
      }
    }

    class IsPremiereHandler extends OfferBooleanHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.is_premiere.toString
      def handleBoolean(b: Boolean) {
        etIsPremiere = b
      }
    }

    class IsKidsHandler extends OfferBooleanHandler with IgnoreFieldFailure {
      def myTag = AnyOfferFields.is_kids.toString
      def handleBoolean(b: Boolean) {
        etIsKids = b
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

    protected var isIgnore = false

    /** Сброс переменных накопления данных в исходное состояние. */
    def reset() {
      sbLen = 0
      sb.clear()
      hadOverflow = false
    }

    /** Считываем символы в буфер с ограничением по максимальной длине буффера. Иными словами, не считываем
      * ничего лишнего. Если переполнение, то выставить флаг и многоточие вместо последнего символа. */
    override def characters(ch: Array[Char], start: Int, length: Int) {
      if (!isIgnore) {
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
    }

    /** Выход из текущего элемента. Вызвать функцию, обрабатывающую собранный результат. */
    override def endTag(tagName: String) {
      if (!isIgnore) {
        try {
          handleRawValue(sb)
        } catch {
          case ex: Throwable => handleValueFailure(ex)
        }
      }
      super.endTag(tagName)
    }

    def wrapValueFailureException(ex: Throwable): YmParserException = {
      YmOtherException("Failed to parse simple value.", ex)
    }

    def maybeWrapFailureException(ex: Throwable): YmParserException = {
      ex match {
        case ymEx: YmParserException =>
          ymEx

        case _ =>
          wrapValueFailureException(ex)
      }
    }

    /** Возника ошибка при обработке значения. */
    def handleValueFailure(ex: Throwable) {
      throw maybeWrapFailureException(ex)
    }
  }

  /** Добавить в SimpleValueHandler функционал подавления ошибок. */
  trait SVHIgnoreFailure extends SimpleValueHandler {
    /** Начало тега, а SVH не ожидает подтегов внутри значения. Игнорим весь тег. */
    override def startTag(tagName: String, attributes: Attributes) {
      isIgnore = true
    }

    /** Возника ошибка при разборке значения. */
    override def handleValueFailure(ex: Throwable) {
      isIgnore = true
    }

    /** Возникла проблема при закрытии тега. Подавляем ошибку в ожидании нормального закрывающего тега. */
    override def unexpectedClosingTag(tagName: String) {
      isIgnore = true
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
  def name: String
}


/** Экспорт состояния оффера для возможности взаимодействия с ним извне. */
trait OfferHandlerState {
  /** Расaпарсенное значение аттрибута типа предложения. Для simple это SIMPLE. */
  def offerType: OfferType
  /** Опциональный уникальный идентификатор комерческого предложения по мнению магазина. */
  def shopOfferIdOpt: Option[String]
  /** Значение необязательного флага available, если таков выставлен. */
  def isAvailable: Boolean
}


trait SimpleValueT {
  def maxLen: Int
  def myTag: String
  def myAttrs: Attributes
}


/** Интерфейс обработчика ошибок SAX-парсеров ym-прайслистов. */
trait YmSaxErrorHandler {
  def handleParsingWarn(ex:  YmParserException)
  def handleParsingError(ex: YmParserException)
  def handleParsingFatalError(ex: YmParserException)
}

/** Реализация логгера, который ругается в log4j об ошибках вместе с длинными бактрейсами.
  * Бывает полезно при тестах. */
trait YmSaxErrorLogger extends YmSaxErrorHandler with MacroLogsImpl {
  def logPrefix: String

  def handleParsingWarn(ex: YmParserException) {
    LOGGER.warn(logPrefix, ex)
  }
  def handleParsingError(ex: YmParserException) {
    LOGGER.error(logPrefix, ex)
  }
  def handleParsingFatalError(ex: YmParserException) {
    LOGGER.error(logPrefix + " FATAL", ex)
  }
}

/** Логгер, который пишет в логи, но без бактрейсов. */
trait YmSaxOnlyMsgErrorLogger extends YmSaxErrorHandler with MacroLogsImpl {
  def logPrefix: String

  override def handleParsingFatalError(ex: YmParserException) {
    LOGGER.error(logPrefix + " FATAL " + ex.getMessage)
  }

  override def handleParsingError(ex: YmParserException) {
    LOGGER.error(logPrefix + ex.getMessage)
  }

  override def handleParsingWarn(ex: YmParserException) {
    LOGGER.warn(logPrefix + ex.getMessage)
  }
}


trait YmSaxResultsHandler {
  /** Завершена сборка одного датума комерческого предложения.
    * @param offerDatum Датум комерческого предложения.
    * @param currenShopDatum Текущий, ещё не законченный экземпляр [[io.suggest.ym.model.YmShopDatum]].
    */
  def handleOfferDatum(offerDatum: YmOfferDatum, currenShopDatum: YmShopDatum)

  /** Обработка тега shop завершена. Есть готовый [[io.suggest.ym.model.YmShopDatum]] и его можно отработать.
   * @param shopDatum Датум магазина.
   */
  def handleShopDatum(shopDatum: YmShopDatum)
}


/** Контекст текущего магазина. Служит для раскрытия магазина в офферах и других местах.
  * @param datum Датум.
  * @param catTransMap Карта отражения категорий.
  * @param currencies Карта валют магазина.
  */
case class ShopContext(
  datum: YmShopDatum,
  catTransMap: Option[YmCatTranslator.ResultMap_t],
  currencies: Map[String, YmShopCurrency]
) extends ShopHandlerState {
  def name: String = datum.name
}

