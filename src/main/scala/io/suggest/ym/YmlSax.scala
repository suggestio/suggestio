package io.suggest.ym

import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes
import cascading.tuple.TupleEntryCollector
import io.suggest.sax.SaxContentHandlerWrapper
import YmConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.01.14 14:49
 * Description: SAX-обработчик для Yandex Market XML (YML).
 * Обработчик работает в поточном режиме с минимальным потреблением RAM, поэтому в конструктор требуется передавать
 * коллектор результатов. Из ограничений такого подхода: offers должны идти самым последним тегом в теле тега shop.
 *
 * Парсер реализован в виде конечного автомата, который переключает свои обработчики по мере погружения в элементы.
 *
 * $ - [[http://help.yandex.ru/partnermarket/yml/about-yml.xml О формате YML]]
 * $ - [[http://help.yandex.ru/partnermarket/export/date-format.xml Форматы дат]]
 */

object YmlSax {
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

  protected def insertHandler(h: MyHandler) {
    handlersStack ::= h
  }


  //------------------------------------------- FSM States ------------------------------------------------

  /** Родительский класс всех хендлеров. */
  sealed abstract class MyHandler extends DefaultHandler {
    def myTag: String
    def myAttrs: Attributes

    /** Выход из текущего элемента у всех одинаковый. */
    override def endElement(uri: String, localName: String, qName: String) {
      if (localName == myTag  &&  handlersStack.head == this) {
        handlersStack = handlersStack.tail
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
        case TopLevelFields.yml_catalog => insertHandler(YmlCatalogHandler())
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
        case ShopFields.name                => new NameHandler
        case ShopFields.company             => new CompanyHandler
        case ShopFields.url                 => new UrlHandler
        case ShopFields.phone               => new PhoneHandler
        case e @ (ShopFields.platform | ShopFields.version | ShopFields.agency) =>
          new MyDummyHandler(e.toString, attributes)
        case ShopFields.email               => new EmailsHandler
        case ShopFields.currencies          => new CurrenciesHandler
        case ShopFields.categories          => new CategoriesHandler
        case ShopFields.store               => new StoreHandler
        case ShopFields.delivery            => new DeliveryHandler
        case ShopFields.pickup              => new PickupHandler
        case ShopFields.deliveryIncluded    => new DeliveryIncludedHandler
        case ShopFields.local_delivery_cost => new LocalDeliveryCostHandler
        case ShopFields.adult               => new AdultHandler
        case ShopFields.offers              => new OffersHandler(attributes, this)
      }
      insertHandler(nextHandler)
    }


    class NameHandler extends StringHandler {
      def myTag: String = ShopFields.email.toString
      def handleResult(s: String) { name = s }
    }

    class CompanyHandler extends StringHandler {
      def myTag: String = ShopFields.company.toString
      def handleResult(s: String) { company = s }
    }

    class UrlHandler extends StringHandler {
      def myTag: String = ShopFields.url.toString
      def handleResult(s: String) { url = s }
    }

    class PhoneHandler extends StringHandler {
      def myTag: String = ShopFields.phone.toString
      def handleResult(phoneStr: String) {
        // TODO Парсить телефон, проверять и сохранять в нормальном формате.
        phoneOpt = Some(phoneStr)
      }
    }

    class EmailsHandler extends StringHandler {
      def myTag = ShopFields.email.toString
      def handleResult(email: String) { emails ::= email }
    }

    /** Парсер списка валют магазина. */
    class CurrenciesHandler extends MyHandler {
      def myTag: String = ShopFields.currencies.toString
      def myAttrs: Attributes = EmptyAttrs

      // TODO Надо парсить валюты по-нормальному

      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        super.startElement(uri, localName, qName, attributes)
        if (localName == TAG_CURRENCY) {
          val maybeId = attributes.getValue(ATTR_ID)
          if (maybeId != null) {
            val id   = maybeId.trim.toUpperCase
            val rate = ShopCurrency.parseCurrencyAttr(attributes, ATTR_RATE, ShopCurrency.RATE_DFLT)
            val plus = ShopCurrency.parseCurrencyAttr(attributes, ATTR_PLUS, ShopCurrency.PLUS_DFLT)
            currencies ::= ShopCurrency(id, rate=rate, plus=plus)
          }
        }
      }
    }

    /** Парсер списка категорий, которые заданы самим магазином. */
    class CategoriesHandler extends MyHandler {
      def myTag: String = ShopFields.categories.toString
      def myAttrs: Attributes = EmptyAttrs

      var idCur: String = null
      var parentIdOptCur: Option[Int] = None
      var categoryDataAcc = new StringBuilder

      def reset() {
        idCur = null
        parentIdOptCur = None
        categoryDataAcc.clear()
      }

      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        super.startElement(uri, localName, qName, attributes)
        if (localName == TAG_CATEGORY) {
          val maybeId = attributes.getValue(ATTR_ID)
          if (maybeId != null) {
            idCur = maybeId.trim
            parentIdOptCur = Option(attributes.getValue(ATTR_PARENT_ID)).map(_.toInt)
          } else throw ExpectedAttrNotFoundException(tag=localName, attr = ATTR_ID)
        } else throw UnexpectedTagException(localName)
      }

      override def characters(ch: Array[Char], start: Int, length: Int) {
        categoryDataAcc.appendAll(ch, start, length)
      }

      /** Выход из текущего элемента у всех одинаковый. */
      override def endElement(uri: String, localName: String, qName: String) {
        if (localName == TAG_CATEGORIES) {
          super.endElement(uri, localName, qName)
        } else if (localName == TAG_CATEGORY) {
          categories ::= ShopCategory(id=idCur, parentId = parentIdOptCur, text=categoryDataAcc.toString())
          reset()
        }
      }
    }

    class StoreHandler extends BooleanHandler {
      def myTag = ShopFields.store.toString
      def handleResult(b: Boolean) { storeOpt = Some(b) }
    }

    class DeliveryHandler extends BooleanHandler {
      def myTag = ShopFields.delivery.toString
      def handleResult(b: Boolean) { deliveryOpt = Some(b) }
    }

    class PickupHandler extends BooleanHandler {
      def myTag = ShopFields.pickup.toString
      def handleResult(b: Boolean) { pickupOpt = Some(b) }
    }

    class DeliveryIncludedHandler extends BooleanHandler {
      def myTag = ShopFields.deliveryIncluded.toString
      def handleResult(b: Boolean) { deliveryIncludedOpt = Some(b) }
    }

    class LocalDeliveryCostHandler extends IntHandler {
      def myTag = ShopFields.local_delivery_cost.toString
      def handleResult(i: Int) { localDeliveryCostOpt = Some(i) }
    }

    class AdultHandler extends BooleanHandler {
      def myTag = ShopFields.adult.toString
      def handleResult(b: Boolean) { adultOpt = Some(b) }
    }
  }


  case class OffersHandler(myAttrs: Attributes = EmptyAttrs, myShop: ShopHandler) extends MyHandler {
    def myTag = ShopFields.offers.toString

    override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
      super.startElement(uri, localName, qName, attributes)
      OffersFields.withName(localName) match {
        case OffersFields.offer => ???
      }
    }
  }


  sealed trait OneOfferHandler extends MyHandler {
    def myTag = OffersFields.offer.toString

    val idOpt = Option(myAttrs.getValue(ATTR_ID))

    /**
     * Для корректного соотнесения всех вариантов товара с карточкой модели необходимо в описании каждого
     * товарного предложения использовать атрибут group_id. Значение атрибута числовое, задается произвольно.
     * Для всех предложений, которые необходимо отнести к одной карточке товара, должно быть указано одинаковое
     * значение атрибута group_id.
     * [[http://partner.market.yandex.ru/legal/clothes/ Цитата взята отсюда]]
     */
    val groupIdOpt = Option(myAttrs.getValue(ATTR_GROUP_ID))
    val availableOpt: Option[Boolean] = Option(myAttrs.getValue(ATTR_AVAILABLE)).map(_.toBoolean)
    def offerTypeAttrV: String
  }

  object OneOfferHandler {
    /**
     * По историческим причинам, в маркете есть несколько типов, описывающих коммерческое предложение магазина.
     * Нужно определить тип и сгенерить необходимый handler.
     * [[http://help.yandex.ru/partnermarket/offers.xml Описание всех типов offer]]
     * @param attrs Исходные аттрибуты тега offer.
     * @return Какой-то хандлер, пригодный для парсинга указанного коммерческого предложения.
     */
    def apply(attrs: Attributes): OneOfferHandler = {
      attrs.getValue(ATTR_TYPE) match {
        case null => new SimpleOfferHandler(attrs)
      }
    }
  }


  /** "Упрощенное описание" - исторический перво-формат яндекс-маркета.
    * Поле типа ещё не заимплеменчено, и есть некоторый фиксированный набор полей. */
  case class SimpleOfferHandler(myAttrs: Attributes) extends OneOfferHandler {
    def offerTypeAttrV = null

    ???
  }


  /** Враппер для дедубликации кода хандлеров, которые читают текст из тега и что-то делают с накопленным буффером. */
  abstract class SimpleValueHandler extends MyHandler {
    protected val sb = new StringBuilder
    def myAttrs: Attributes = EmptyAttrs

    override def characters(ch: Array[Char], start: Int, length: Int) {
      sb.appendAll(ch, start, length)
    }
  }
  
  abstract class StringHandler extends SimpleValueHandler {
    def handleResult(s: String)

    /** Выход из текущего элемента у всех одинаковый. */
    override def endElement(uri: String, localName: String, qName: String) {
      super.endElement(uri, localName, qName)
      val resultStr = sb.toString()
      handleResult(resultStr)
    }
  }

  abstract class BooleanHandler extends SimpleValueHandler {
    def handleResult(b: Boolean)

    /** Выход из текущего элемента у всех одинаковый. */
    override def endElement(uri: String, localName: String, qName: String) {
      super.endElement(uri, localName, qName)
      handleResult(sb.toBoolean)
    }
  }

  abstract class IntHandler extends SimpleValueHandler {
    def handleResult(i: Int)

    /** Выход из текущего элемента у всех одинаковый. */
    override def endElement(uri: String, localName: String, qName: String) {
      super.endElement(uri, localName, qName)
      handleResult(sb.toInt)
    }
  }

}


protected case object EmptyAttrs extends Attributes {
  def getLength: Int = 0
  def getURI(index: Int): String = null
  def getLocalName(index: Int): String = null
  def getQName(index: Int): String = null
  def getType(index: Int): String = null
  def getValue(index: Int): String = null
  def getIndex(uri: String, localName: String): Int = -1
  def getIndex(qName: String): Int = -1
  def getType(uri: String, localName: String): String = null
  def getType(qName: String): String = null
  def getValue(uri: String, localName: String): String = null
  def getValue(qName: String): String = null
}


object ShopCurrency {
  val RATE_DFLT = 1.0F
  val PLUS_DFLT = 0F

  def parseCurrencyAttr(attrs:Attributes, attrKey: String, default: Float): Float = {
    Option(attrs.getValue(attrKey)) map { _.toFloat } getOrElse default
  }
}
sealed case class ShopCurrency(
  id: String,
  rate: Float = ShopCurrency.RATE_DFLT,
  plus: Float = ShopCurrency.PLUS_DFLT
)


sealed case class ShopCategory(id: String, parentId: Option[Int], text: String)

case class UnexpectedTagException(tag: String) extends IllegalArgumentException("Unexpected tag: " + tag)
case class ExpectedAttrNotFoundException(tag:String, attr:String) extends IllegalArgumentException(s"Expected attribute '$attr' not found in tag '$tag'.")

