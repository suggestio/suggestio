package models.ai

import java.util.Currency

import scala.beans.BeanProperty

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 20:03
 * Description: Описание валюты в рамках ai-моделей.
 */
case class AiCurrency(
  @BeanProperty charCode  : String,
  @BeanProperty course    : Float,
  numCodeOpt              : Option[String] = None,
  @BeanProperty count     : Int = 1,
  nameOpt                 : Option[String] = None
) {

  def getCurrency = Currency.getInstance(charCode)
  def getName = if (nameOpt.isDefined) nameOpt.get else ""
}


/** Аккамуляторная версия AiCurrency, т.е. созданная для пошагового наполнения данными. */
case class AiCurrencyAcc(
  var charCode    : String = null,
  var course      : Float = -1F,
  var numCodeOpt  : Option[String] = None,
  var count       : Int = 1,
  var nameOpt     : Option[String] = None
) {

  def toImmutable() = AiCurrency(
    charCode = charCode,
    course = course,
    numCodeOpt = numCodeOpt,
    count = count,
    nameOpt = nameOpt
  )

}


/** JavaBean, передаваемый в scalasti-шаблоны. */
trait CurrenciesInfoBeanT extends ContentHandlerResult {

  /** Карта, где ключ - это имя валюты (USD), а значение -- данные по валюте. */
  def getMap: Map[String, AiCurrency]

  def getCurrency(cc: String) = getMap(cc)

  // Быстрый доступ к валютам из scalasti-шаблонов. Если валюта отсутствует в карте, то будет ошибка.
  def getAud = getCurrency("AUD")
  def getAzn = getCurrency("AZN")
  def getGbr = getCurrency("GBP")
  def getAmd = getCurrency("AMD")
  def getByr = getCurrency("BYR")
  def getBgn = getCurrency("BGN")
  def getBrl = getCurrency("BRL")
  def getHuf = getCurrency("HUF")
  def getDkk = getCurrency("DKK")
  def getUsd = getCurrency("USD")
  def getEur = getCurrency("EUR")
  def getInr = getCurrency("INR")
  def getKzt = getCurrency("KZT")
  def getCad = getCurrency("CAD")
  def getKgs = getCurrency("KGS")
  def getCny = getCurrency("CNY")
  def getLtl = getCurrency("LTL")
  def getMdl = getCurrency("MDL")
  def getNok = getCurrency("NOK")
  def getPln = getCurrency("PLN")
  def getRon = getCurrency("RON")
  def getXdr = getCurrency("XDR")
  def getSgd = getCurrency("SGD")
  def getTjs = getCurrency("TJS")
  def getTry = getCurrency("TRY")
  def getTmt = getCurrency("TMT")
  def getUzs = getCurrency("UZS")
  def getUah = getCurrency("UAH")
  def getCzk = getCurrency("CZK")
  def getSek = getCurrency("SEK")
  def getChf = getCurrency("CHF")
  def getZar = getCurrency("ZAR")
  def getKrw = getCurrency("KRW")
  def getJpy = getCurrency("JPY")

}

