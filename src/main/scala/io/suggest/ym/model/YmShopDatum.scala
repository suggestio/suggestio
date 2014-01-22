package io.suggest.ym.model

import com.scaleunlimited.cascading.BaseDatum
import io.suggest.util.CascadingFieldNamer
import cascading.tuple.{TupleEntry, Tuple, Fields}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.14 10:41
 * Description: Кортеж с кратким описанием магазина, который предоставляет оффер.
 * Много необязательных полей, но они всё равно все в основном кортеже.
 */
object YmShopDatum extends CascadingFieldNamer with YmDatumDeliveryStaticT {

  val NAME_FN                 = fieldName("name")
  val COMPANY_FN              = fieldName("company")
  val URL_FN                  = fieldName("url")
  val PHONE_FN                = fieldName("phone")
  val EMAILS_FN               = fieldName("emails")
  val CURRENCIES_FN           = fieldName("currencies")
  val CATEGORIES_FN           = fieldName("categories")
  val STORE_FN                = fieldName("store")
  val PICKUP_FN               = fieldName("pickup")
  val DELIVERY_FN             = fieldName("delivery")
  val DELIVERY_INCLUDED_FN    = fieldName("deliveryIncluded")
  val LOCAL_DELIVERY_COST_FN  = fieldName("localDeliveryCost")
  val ADULT_FN                = fieldName("adult")

  val FIELDS = new Fields(
    NAME_FN, COMPANY_FN, URL_FN, PHONE_FN, EMAILS_FN, CURRENCIES_FN, CATEGORIES_FN,
    STORE_FN, PICKUP_FN, DELIVERY_FN, DELIVERY_INCLUDED_FN, LOCAL_DELIVERY_COST_FN, ADULT_FN
  )


  /** Сериализовать список email'ов. */
  def serializeEmails(emails: Seq[String]): Tuple = {
    new Tuple(emails : _*)
  }
  /** Десериализовать список email'ов, сериализованных через serializeEmails(). */
  val deserializeEmails: PartialFunction[AnyRef, Seq[String]] = {
    case null     => Nil
    case t: Tuple => t.toSeq.asInstanceOf[Seq[String]]
  }


  /** Десериализовать список валют, сериализованных через serializeCurrencies(). */
  val deserializeCurrencies: PartialFunction[AnyRef, Seq[YmShopCurrency]] = {
    case null     => Nil
    case t: Tuple => t.toSeq.asInstanceOf[Seq[Tuple]].map(new YmShopCurrency(_))
  }

  /** Сериализовать список валют. */
  def serializeCurrencies(currencies: Seq[YmShopCurrency]): Tuple = {
    val t = new Tuple
    currencies.foreach { ysc =>
      t add ysc.getTuple
    }
    t
  }


  /** Десериализовать список-дерево категорий магазина, сериазизованных через serializeCategories(). */
  val deserializeCategories: PartialFunction[AnyRef, Seq[YmShopCategory]] = {
    case null     => Nil
    case t: Tuple => t.toSeq.asInstanceOf[Seq[Tuple]].map(new YmShopCategory(_))
  }

  /** Сериализовать дерево-список категорий магазина. */
  def serializeCategories(categories: Seq[YmShopCategory]): Tuple = {
    val t = new Tuple
    categories.foreach { cat =>
      t add cat.getTuple
    }
    t
  }
}


import YmShopDatum._

class YmShopDatum extends BaseDatum(FIELDS) with YmDatumDeliveryT {

  def companion = YmShopDatum

  def this(t: Tuple) = {
    this
    setTuple(t)
  }

  def this(te: TupleEntry) = {
    this
    setTupleEntry(te)
  }

  def this(name: String, url:String) = {
    this
    setName(name)
    setUrl(url)
  }


  def getName = _tupleEntry getString NAME_FN
  def setName(name: String) = {
    _tupleEntry.setString(NAME_FN, name)
    this
  }

  def getCompany = _tupleEntry getString COMPANY_FN
  def setCompany(company: String) = {
    _tupleEntry.setString(COMPANY_FN, company)
    this
  }

  def getUrl = _tupleEntry getString URL_FN
  def setUrl(url: String) = {
    _tupleEntry.setString(URL_FN, url)
    this
  }

  def getPhone = Option(_tupleEntry getString PHONE_FN)
  def setPhone(phoneOpt: Option[String]) = {
    _tupleEntry.setString(PHONE_FN, phoneOpt getOrElse null)
    this
  }

  def getEmails: Seq[String] = {
    val raw = _tupleEntry.getObject(EMAILS_FN)
    deserializeEmails(raw)
  }
  def setEmails(emails: Seq[String]) = {
    val t = serializeEmails(emails)
    _tupleEntry.setObject(EMAILS_FN, t)
    this
  }

  def getCurrencies: Seq[YmShopCurrency] = {
    val raw = _tupleEntry getObject CURRENCIES_FN
    deserializeCurrencies(raw)
  }
  def setCurrencies(currencies: Seq[YmShopCurrency]) = {
    val t = serializeCurrencies(currencies)
    _tupleEntry.setObject(CURRENCIES_FN, t)
    this
  }

  def getCategories: Seq[YmShopCategory] = {
    val raw = _tupleEntry getObject CATEGORIES_FN
    deserializeCategories(raw)
  }
  def setCategories(categories: Seq[YmShopCategory]) = {
    val t = serializeCategories(categories)
    _tupleEntry.setObject(CATEGORIES_FN, t)
    this
  }

}
