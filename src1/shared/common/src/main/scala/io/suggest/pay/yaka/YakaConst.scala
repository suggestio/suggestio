package io.suggest.pay.yaka

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.02.17 21:20
  * Description: Константы яндекс-кассы.
  */
object YakaConst {

  // Имена полей яндекс-кассы, используемых в системе:

  def ACTION_FN     = "action"

  def ORDER_ID_FN   = "orderNumber"

  def PERSON_ID_FN  = "customerNumber"

  def INVOICE_ID_FN = "invoiceId"

  def SHOP_ID_FN    = "shopId"

  def SC_ID_FN      = "scid"

  /** Сумма платежа в рублях в форме запроса платежа. */
  def SUM_FN        = "sum"
  /** Сумма платежа в какой-то валюте в запросах check/payment/success/fail. */
  def AMOUNT_FN     = "orderSumAmount"

  def CURRENCY_FN   = "orderSumCurrencyPaycash"

  def BANK_ID_FN    = "orderSumBankPaycash"

  def MD5_FN        = "md5"

  /** email клиента, если известен. */
  def CLIENT_EMAIL_FN = "cps_email"


  /** Все кастомные поля имеют указанный sio-префикс. */
  def CUSTOM_FIELDS_PREFIX = "_sio_"

  /** Кастомное поле с id узла, с которого уходит юзер.
    * Передаётся в яндекс-кассу, чтобы знать, в какой ЛК надо возвращать юзера после оплаты.
    */
  def SIO_NODE_ID_FN  = CUSTOM_FIELDS_PREFIX + "nodeId"

}
