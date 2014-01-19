package io.suggest.ym

import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes
import cascading.tuple.TupleEntryCollector
import io.suggest.sax.{EmptyAttributes, SaxContentHandlerWrapper}
import YmConstants._
import io.suggest.util.UrlUtil
import io.suggest.ym.AnyOfferFields.AnyOfferField
import io.suggest.util.MyConfig.CONFIG
import io.suggest.ym.OfferTypes.OfferType
import YmParsers._

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

object YmlSax {
  
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

  val OFFER_TYPE_PREFIX_MAXLEN = CONFIG.getInt("ym.sax.offer.type_prefix.len.max") getOrElse 128
  
  val REC_LIST_MAX_CHARLEN = CONFIG.getInt("ym.sax.offer.rec.charlen.max") getOrElse 128
  val REC_LIST_MAX_LEN     = CONFIG.getInt("ym.sax.offer.rec.len.max") getOrElse 10
  val REC_LIST_SPLIT_RE = "\\s*,\\s*".r

  /** Регэксп для ключа offer.seller_warranty. В доках этот ключ написан с разными орф.ошибками,
    * поэтому его надо матчить по регэкспу. */
  val SELLER_WARRANTY_RE = "sell?er_[wv]arr?anty".r

  val OFFER_DIMENSIONS_MAXLEN = CONFIG.getInt("ym.sax.offer.dimensions.len.max") getOrElse 64

}


import YmlSax._

class YmlSax(outputCollector: TupleEntryCollector) extends SaxContentHandlerWrapper {

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


  //------------------------------------------- FSM States ------------------------------------------------

  /** Родительский класс всех хендлеров. */
  trait MyHandler extends DefaultHandler {
    def myTag: String
    def myAttrs: Attributes

    /** Выход из текущего элемента у всех одинаковый. */
    override def endElement(uri: String, localName: String, qName: String) {
      if (localName == myTag  &&  handlersStack.head == this) {
        unbecome()
      }
    }
  }

  /** Когда ничего не интересно, и нужно просто дождаться выхода из тега. */
  sealed case class MyDummyHandler(myTag: String, myAttrs: Attributes) extends MyHandler


  /** Хэндлер всего документа. Умеет обрабатывать лишь один тег. */
  case class TopLevelHandler() extends MyHandler {
    def myTag: String = ???
    def myAttrs: Attributes = ???

    override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
      TopLevelFields.withName(localName) match {
        case TopLevelFields.yml_catalog => become(YmlCatalogHandler())
      }
    }
  }


  /**
   * Обработчик тега верхнего уровня.
   * @param myAttrs Атрибуты тега yml_catalog, которые обычно включают в себя date=""
   */
  case class YmlCatalogHandler(myAttrs: Attributes = EmptyAttrs) extends MyHandler {
    def myTag = TopLevelFields.yml_catalog.toString

    override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
      YmlCatalogFields.withName(localName) match {
        case YmlCatalogFields.shop =>
          handlersStack ::= new ShopHandler(attributes)
      }
    }
  }


  /**
   * Обработчик содержимого тега shop.
   * @param myAttrs Атрибуты тега shop, которые вроде бы всегда пустые.
   */
  case class ShopHandler(myAttrs: Attributes = EmptyAttrs) extends MyHandler {
    def myTag = YmlCatalogFields.shop.toString

    var name                : String = null
    var company             : String = null
    var url                 : String = null
    var phoneOpt            : Option[String] = None
    //var platformOpt         : Option[String] = None
    //var versionOpt          : Option[String] = None
    //var agencyOpt           : Option[String] = None
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
    override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
      val nextHandler = ShopFields.withName(localName) match {
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
        case ShopFields.offers              => new OffersHandler(attributes, this)
      }
      become(nextHandler)
    }


    class ShopNameHandler extends StringHandler {
      def myTag = ShopFields.email.toString
      override def maxLen: Int = SHOP_NAME_MAXLEN
      def handleString(s: String) { name = s }
    }

    class ShopCompanyHandler extends StringHandler {
      def myTag = ShopFields.company.toString
      override def maxLen: Int = SHOP_COMPANY_MAXLEN
      def handleString(s: String) { company = s }
    }

    class ShopUrlHandler extends UrlHandler {
      def myTag  = ShopFields.url.toString
      override def maxLen: Int = SHOP_URL_MAXLEN
      def handleUrl(_url: String) { url = _url }
    }

    class PhoneHandler extends StringHandler {
      def myTag = ShopFields.phone.toString
      override def maxLen: Int = PHONE_MAXLEN
      def handleString(phoneStr: String) {
        // TODO Парсить телефон, проверять и сохранять в нормальном формате.
        phoneOpt = Some(phoneStr)
      }
    }

    object EmailsHandler extends StringHandler {
      def myTag = ShopFields.email.toString
      override def maxLen: Int = EMAIL_MAXLEN
      def handleString(email: String) { emails ::= email }
    }

    /** Парсер списка валют магазина.
      * [[http://help.yandex.ru/partnermarket/currencies.xml Документация по списку валют магазина]]. */
    class CurrenciesHandler extends MyHandler {
      def myTag = ShopFields.currencies.toString
      def myAttrs = EmptyAttrs
      var currCounter = 0

      // TODO Надо парсить валюты по-нормальному
      // TODO Надо проверять курс валют, чтобы он отклонялся не более на 30% от курса ЦБ.

      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        if ((localName equalsIgnoreCase TAG_CURRENCY)  &&  currCounter < SHOP_CURRENCIES_COUNT_MAX) {
          val maybeId = attributes.getValue(ATTR_ID)
          if (maybeId != null) {
            var id = maybeId.trim
            if (id.length > SHOP_CURRENCY_ID_MAXLEN)
              throw CurrencyIdTooLongException(id)
            id = id.toUpperCase
            val rate = ShopCurrency.parseCurrencyAttr(attributes, ATTR_RATE, ShopCurrency.RATE_DFLT)
            val plus = ShopCurrency.parseCurrencyAttr(attributes, ATTR_PLUS, ShopCurrency.PLUS_DFLT)
            currencies ::= ShopCurrency(id, rate=rate, plus=plus)
            currCounter += 1
          }
        }
      }
    }

    /** Парсер списка категорий, которые заданы самим магазином. */
    class ShopCategoriesHandler extends MyHandler {
      def myTag = ShopFields.categories.toString
      override def myAttrs = EmptyAttrs
      var categoriesCount = 0

      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        if ((localName equalsIgnoreCase ShopCategoriesFields.category.toString)  &&  categoriesCount < SHOP_CAT_COUNT_MAX) {
          categoriesCount += 1
          become(new ShopCategoryHandler(attributes))
        }
      }
    }
    
    class ShopCategoryHandler(attrs: Attributes) extends StringHandler {
      def myTag = ShopCategoriesFields.category.toString
      override def maxLen: Int = SHOP_CAT_VALUE_MAXLEN

      def handleString(s: String) {
        if (hadOverflow) {
          throw ShopCategoryTooLongException(s)
        }
        val maybeId = attrs.getValue(ATTR_ID)
        if (maybeId != null) {
          val idCur = maybeId.trim
          if (idCur.length > SHOP_CAT_ID_LEN_MAX)
            throw ShopCategoryTooLongException(idCur)
          val parentIdOptCur = Option(attrs.getValue(ATTR_PARENT_ID)).map(_.toInt)
          categories ::= ShopCategory(id=idCur, parentId = parentIdOptCur, text=sb.toString())
        } else
          throw ExpectedAttrNotFoundException(tag=myTag, attr=ATTR_ID)
      }
    }

    class ShopStoreHandler extends BooleanHandler {
      def myTag = ShopFields.store.toString
      def handleBoolean(b: Boolean) { storeOpt = Some(b) }
    }

    class ShopDeliveryHandler extends BooleanHandler {
      def myTag = ShopFields.delivery.toString
      def handleBoolean(b: Boolean) { deliveryOpt = Some(b) }
    }

    class ShopPickupHandler extends BooleanHandler {
      def myTag = ShopFields.pickup.toString
      def handleBoolean(b: Boolean) { pickupOpt = Some(b) }
    }

    class ShopDeliveryIncludedHandler extends BooleanHandler {
      def myTag = ShopFields.deliveryIncluded.toString
      def handleBoolean(b: Boolean) { deliveryIncludedOpt = Some(b) }
    }

    class ShopLocalDeliveryCostHandler extends FloatHandler {
      def myTag = ShopFields.local_delivery_cost.toString
      def handleFloat(f: Float) { localDeliveryCostOpt = Some(f) }
    }

    class ShopAdultHandler extends BooleanHandler {
      def myTag = ShopFields.adult.toString
      def handleBoolean(b: Boolean) { adultOpt = Some(b) }
    }
  }


  case class OffersHandler(myAttrs: Attributes = EmptyAttrs, myShop: ShopHandler) extends MyHandler {
    def myTag = ShopFields.offers.toString

    override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
      OffersFields.withName(localName) match {
        case OffersFields.offer => become(AnyOfferHandler(attributes, myShop))
      }
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
    def apply(attrs: Attributes, myShop: ShopHandler): AnyOfferHandler = {
      val offerType = Option(attrs.getValue(ATTR_TYPE)) map {OfferTypes.withName} getOrElse OfferTypes.default
      offerType match {
        case OfferTypes.SIMPLE          => new SimpleOfferHandler(attrs, myShop)
        case OfferTypes.`vendor.model`  => new VendorModelOfferHandler(attrs, myShop)
      }
    }
  }


  /** Базовый класс для обработчиков коммерческих предложений.
    * Разные типы предложений имеют между собой много общего, поэтому основная часть
    * логики разборки предложений находится здесь. */
  trait AnyOfferHandler extends MyHandler {
    def myTag = OffersFields.offer.toString
    def myShop: ShopHandler

    /** Сырое значение типа аттрибута. Для simple это null, в остальных случаях - это одна из ATTRV_OFTYPE_*. */
    def offerType: OfferType

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
      case (AnyOfferFields.param, attrs)                    => new ParamHandler(attrs)
      // barcode: ignored
    }

    /** Начинается поле оффера. В зависимости от тега, выбрать обработчик. */
    override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
      val handler = getFieldsHandler(AnyOfferFields.withName(localName), attributes)
      become(handler)
    }

    /** Обработчик тега url, вставляющий в состояние результирующую ссылку. */
    class OfferUrlHandler extends UrlHandler {
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
          throw UndefinedCurrencyIdException(s, myShop.currencies)
        if (hadOverflow)
          throw CurrencyIdTooLongException(s)
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
        categoryIds ::= OfferCategoryId(s.trim, catIdType)
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
          val catPath = MARKET_CATEGORY_PATH_SPLIT_RE.split(s.trim).toList.filter(!_.isEmpty)
          marketCategoryOpt = Some(MarketCategory(catPath))
        } else {
          throw MarketCategoryTooLongException(s)
        }
      }
    }

    /** Аккамулирование ссылок на картинки в pictures. Картинок может быть много, и хотя бы одна почти всегда имеется,
      * поэтому блокируем многократное конструирование этого объекта. */
    object PictureUrlHandler extends UrlHandler {
      def myTag = AnyOfferFields.picture.toString
      override def maxLen: Int = OFFER_PICTURE_URL_MAXLEN
      def handleUrl(pictureUrl: String) {
        pictures ::= pictureUrl
      }
    }

    /** store: Парсим поле store, которое говорит, можно ли купить этот товар в магазине. */
    class OfferStoreHandler extends BooleanHandler {
      def myTag = AnyOfferFields.store.toString
      def handleBoolean(b: Boolean) {
        storeOpt = Some(b)
      }
    }

    /** pickup: Парсим поле, говорящее нам о том, возможно ли зарезервировать и самовывезти указанный товар. */
    class OfferPickupHandler extends BooleanHandler {
      def myTag = AnyOfferFields.pickup.toString
      def handleBoolean(b: Boolean) {
        pickupOpt = Some(b)
      }
    }

    /** delivery: Поле указывает, возможна ли доставка указанного товара? */
    class OfferDeliveryHandler extends BooleanHandler {
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
    class DownloadableHandler extends BooleanHandler {
      def myTag = AnyOfferFields.downloadable.toString
      def handleBoolean(b: Boolean) {
        downloadableOpt = Some(b)
      }
    }

    /** adult: обязателен для обозначения товара, имеющего отношение к удовлетворению сексуальных потребностей, либо
      * иным образом эксплуатирующего интерес к сексу.
      * [[http://help.yandex.ru/partnermarket/adult.xml Документация по тегу adult]]. */
    class OfferAdultHandler extends BooleanHandler {
      def myTag = AnyOfferFields.adult.toString
      def handleBoolean(b: Boolean) {
        adultOpt = Some(b)
      }
    }

    /** Возрастая категория товара. Единицы измерения заданы в аттрибутах. */
    class AgeHandler(attrs: Attributes) extends IntHandler {
      def myTag = AnyOfferFields.age.toString
      private def ex(msg: String) = throw OfferAgeInvalidException(myShop.name, idOpt, msg)
      def handleInt(i: Int) {
        attrs.getValue(ATTR_UNIT) match {
          case null => ex("Attribute 'unit' missing.")
          case unitV =>
            val unitV1 = OfferAgeUnits.withName(unitV.trim.toLowerCase)
            val possibleValues = unitV1 match {
              case OfferAgeUnits.month => OFFER_AGE_MONTH_VALUES
              case OfferAgeUnits.year  => OFFER_AGE_YEAR_VALUES
            }
            if (!(possibleValues contains i)) {
              ex(s"Invalid value for unit=$unitV1: $i ; Possible values are: ${possibleValues.mkString(", ")}")
            }
            ageOpt = Some(OfferAge(units = unitV1, value = i))
        }
      }
    }

    // TODO Надо параметры как-то по-лучше парсить. Параметры также используются для фасетных группировок.
    // TODO Надо проверять нормализовать параметры по списку готовых названий параметров.
    // TODO Надо парсить значения согласно заданным механизам.
    class ParamHandler(attrs: Attributes) extends StringHandler {
      def myTag = AnyOfferFields.param.toString
      override def maxLen: Int = OFFER_PARAM_MAXLEN
      private def ex(msg: String) = throw OfferParamInvalidException(myShop.name, idOpt, msg)
      def handleString(s: String) {
        if (paramsCounter <= OFFER_PARAMS_COUNT_MAX) {
          val nameAttrKey = OfferParamAttrs.name.toString
          val paramName = attrs.getValue(nameAttrKey)
          if (paramName == null) {
            ex(s"Mandatory attribute '$nameAttrKey' missing.")
          }
          val s1 = s.trim
          if (s1.isEmpty) {
            ex(s"Param '$paramName' value is empty.")
          }
          val unitOpt = Option(attrs.getValue(OfferParamAttrs.unit.toString)) map(_.trim) flatMap { str =>
            if (str.isEmpty)  None  else  Some(str)
          }
          params ::= OfferParam(name=paramName, unitRawOpt=unitOpt, rawValue=s1)
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
      def handleString(s: String) {
        // Базовая нормализация нужна, т.к. это поле не отображается юзеру и используется для группировки по брендам.
        // TODO Надо бы нормализовывать с синонимами, чтобы HP и Hewlett-packard были эквивалентны.
        val vendor = s.trim.toLowerCase
        vendorOpt = Some(vendor)
      }
    }

    /** vendorCode: Номер изделия по мнению производителя. Не индексируется, не используется для группировки. */
    class VendorCodeHandler extends StringHandler {
      def myTag = AnyOfferFields.vendorCode.toString
      def handleString(s: String) {
        vendorCodeOpt = Some(s.trim)
      }
    }
  }


  /** Большинство офферов имеют поле name. Тут поддержка этого поля. */
  trait OfferNameH extends AnyOfferHandler {
    /** name обозначает какое-то отображаемое имя в зависимости от контекста. Везде оно обязательно. */
    var name: String = null

    /** Обработчик полей коммерческого предложения, который также умеет name. */
    override val getFieldsHandler: PartialFunction[(AnyOfferField, Attributes), MyHandler] = {
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
  case class SimpleOfferHandler(myAttrs: Attributes, myShop: ShopHandler) extends VendorInfoH with OfferNameH {
    def offerType = OfferTypes.SIMPLE
  }

  /** Произвольный товар (vendor.model)
    * Этот тип описания является наиболее удобным и универсальным, он рекомендован для описания товаров из
    * большинства категорий Яндекс.Маркета.
    * Доп.поля: typePrefix, [vendor, vendorCode], model, provider, tarifPlan. */
  case class VendorModelOfferHandler(myAttrs: Attributes, myShop: ShopHandler) extends VendorInfoH {
    def offerType = OfferTypes.`vendor.model`
    /** Описание очень краткое. "Группа товаров / категория". Хз что тут и как. */
    var typePrefix: Option[String] = None
    /** Модель товара, хотя судя по примерам, там может быть и категория, а сама "модель". "Женская куртка" например. */
    var model: String = null
    /** Гарантия от продавца. Аналогично manufacturer_warranty. */
    var sellerWarrantyOpt: Option[Warranty] = None
    /** Список id рекомендуемых товаров к этому товару. */
    var recIdsList: List[String] = Nil
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
        case (AnyOfferFields.expiry, _)               => ???
        case (AnyOfferFields.weight, _)               => new WeightHandler
      }
    }


    /** Начинается поле оффера. Нужно исправить ошибки в некоторых полях. */
    override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
      // Костыль из-за ошибок в правописании названия ключа seller_warranty
      val ln1 = if (SELLER_WARRANTY_RE.pattern.matcher(localName).matches()) {
        AnyOfferFields.seller_warranty.toString
      } else {
        localName
      }
      super.startElement(uri, ln1, qName, attributes)
    }


    /** typePrefix: Поле с необязательным типом. "Принтер" например. */
    class TypePrefixHandler extends StringHandler {
      def myTag = AnyOfferFields.typePrefix.toString
      override def maxLen: Int = OFFER_TYPE_PREFIX_MAXLEN
      def handleString(s: String) {
        typePrefix = Some(s.trim)
      }
    }

    /** model: Некая модель товара, т.е. правая часть названия. Например "Женская куртка" или "Deskjet D31337". */
    class ModelHandler extends StringHandler {
      def myTag = AnyOfferFields.model.toString
      def handleString(s: String) {
        model = s.trim
      }
    }

    /** Поле, описывающее гарантию от продавца. */
    class SellerWarrantyHandler extends WarrantyHandler {
      def myTag = AnyOfferFields.seller_warranty.toString
      def handleWarranty(wv: Warranty) {
        sellerWarrantyOpt = Some(wv)
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
      def handleRawValue(sb: StringBuilder) {
        dimensionsOpt = Option(parse(DIMENSIONS_PARSER, sb) getOrElse null)
      }
    }
  }



  /** Враппер для дедубликации кода хандлеров, которые читают текст из тега и что-то делают с накопленным буффером. */
  trait SimpleValueHandler extends MyHandler {
    /** Максимальная кол-во символов, которые будут сохранены в аккамулятор будущей строки. */
    def maxLen: Int = STRING_MAXLEN
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
    override def endElement(uri: String, localName: String, qName: String) {
      handleRawValue(sb)
      super.endElement(uri, localName, qName)
    }
  }

  /** Абстрактный обработчик поля, которое содержит какой-то текст. */
  trait StringHandler extends SimpleValueHandler {
    def handleString(s: String)

    def handleRawValue(sb: StringBuilder) {
      handleString(sb.toString)
    }
  }

  /** Обработчик ссылок. */
  trait UrlHandler extends StringHandler {
    def handleUrl(url: String)
    override def maxLen: Int = URL_MAXLEN
    def handleString(s: String) {
      if (hadOverflow) {
        throw UrlTooLongException(s, maxLen)
      } else {
        // Необходимо сверять домен ссылки? По идее магазин платит деньги, значит кривая ссылка - это его проблема, и сверять не надо.
        val url1 = UrlUtil.normalize(s)
        handleUrl(url1)
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
      handleBoolean(sb.toBoolean)
    }
  }

  trait BooleanTagFlagHandler extends MyHandler {
    def handleTrue()
    def myAttrs = EmptyAttrs

    override def endElement(uri: String, localName: String, qName: String) {
      handleTrue()
      super.endElement(uri, localName, qName)
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


/** Неизменяемый объект, описывающий пустые аттрибуты. Полезен как заглушка для пустых аттрибутов. */
case object EmptyAttrs extends EmptyAttributes


object ShopCurrency {
  val RATE_DFLT = 1.0F
  val PLUS_DFLT = 0F

  def parseCurrencyAttr(attrs:Attributes, attrKey: String, default: Float): Float = {
    Option(attrs.getValue(attrKey)) map { _.toFloat } getOrElse default
  }
}

case class ShopCurrency(
  id    : String,
  rate  : Float = ShopCurrency.RATE_DFLT,
  plus  : Float = ShopCurrency.PLUS_DFLT
) extends Serializable


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


case class ShopCategory(id: String, parentId: Option[Int], text: String) extends Serializable


