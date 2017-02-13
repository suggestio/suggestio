package util.pay.yaka

import models.mpay.yaka.{IYakaReqSigned, MYakaAction, MYakaActions, MYakaReq}
import play.api.data._
import Forms._
import com.google.inject.Inject
import io.suggest.bill.{MCurrencies, MCurrency, MPrice}
import io.suggest.common.empty.EmptyUtil
import io.suggest.es.model.IEsModelDiVal
import io.suggest.text.util.TextHashUtil
import io.suggest.util.logs.MacroLogsImpl
import org.apache.commons.codec.digest.DigestUtils
import util.{FormUtil, TplDataFormatUtil}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.17 16:26
  * Description: Разная утиль для взаимодействия с яндекс-кассой, чтобы не перегружать контроллер лишним кодом.
  */
class YakaUtil @Inject() (mCommonDi: IEsModelDiVal) extends MacroLogsImpl {

  import mCommonDi.configuration

  /** id магазина в системе яндекс-кассы.
    * По идее, всегда одинков, т.к. это номер единственного аккаунта по БД яндекса.
    */
  def SHOP_ID = 84780L

  /** 548806 -- демо-витрина, выданная при первом подключении. */
  private def DEMO_SC_ID = 548806L

  private def CONF_PREFIX = "sio.pay.yaka."

  /** id витрины. На продакшене задаётся в конфиге. */
  val SC_ID: Long = {
    configuration.getLong( CONF_PREFIX + "scid").getOrElse {
      val demoScId = DEMO_SC_ID
      LOGGER.info("DEMO pay mode, scid = " + demoScId)
      demoScId
    }
  }

  /** Флаг демо-режима, по умолчанию = true. */
  val IS_DEMO: Boolean = configuration.getBoolean(CONF_PREFIX + "demo").getOrElse(true)


  object ErrorCodes {
    def NO_ERROR    = 0
    def MD5_ERROR   = 1
    def ORDER_ERROR = 100
    def BAD_REQUEST = 200
  }


  /** Пароль для подписывания данных при MD5-режиме. Задаётся только в конфиге. */
  private val YAKA_MD5_PASSWORD: Option[String] = {
    val ck = CONF_PREFIX + "password"
    val resOpt = configuration.getString(ck)
    if (resOpt.isEmpty)
      LOGGER.error("Yandex.Kassa password is not defined in application.conf: " + ck)
    resOpt
  }


  def isEnabled: Boolean = YAKA_MD5_PASSWORD.nonEmpty
  def assertEnabled(): Unit = {
    if (!isEnabled)
      throw new IllegalStateException("Yandex.kassa is NOT configured/enabled.")
  }


  def yakaActionOptM: Mapping[Option[MYakaAction]] = {
    nonEmptyText(minLength = 5, maxLength = 16)
      .transform [Option[MYakaAction]] (
        MYakaActions.withNameOption,
        _.fold("")(_.strId)
      )
  }

  def yakaActionM: Mapping[MYakaAction] = {
    yakaActionOptM
      .verifying("error.required", _.nonEmpty)
      .transform[MYakaAction](
        EmptyUtil.getF,
        EmptyUtil.someF
      )
  }

  def amountM: Mapping[Double] = {
    FormUtil.doubleM
      .verifying("error.too.big", {x => Math.abs(x) <= Math.pow(10, 8)})
      .verifying("error.zero", _ != 0)
  }

  def shopIdM: Mapping[Long] = {
    longNumber(min = 1000)
  }

  def invoiceIdM: Mapping[Long] = {
    longNumber
  }

  def personIdM: Mapping[String] = {
    FormUtil.esIdM
  }

  def md5M: Mapping[String] = {
    val len = TextHashUtil.HASH_LEN_MD5_HEX
    nonEmptyText(len, len)
  }


  def currencyM: Mapping[MCurrency] = {
    // В демо-режиме только демо-рубли допустимы. И их код 10643.
    // https://tech.yandex.ru/money/doc/payment-solution/deposition/test-data-docpage/
    // В обычном режиме -- используется стандартный код валюты.
    val currencyOffset = if (IS_DEMO) 10000 else 0
    FormUtil.currency_iso4217(currencyOffset)
  }

  /** Маппинг для данных запроса check и aviso. */
  def md5Form: Form[MYakaReq] = {
    val m = mapping(
      "action"                    -> yakaActionM,
      "orderSumAmount"            -> amountM,
      "orderSumCurrencyPaycash"   -> currencyM,
      "orderSumBankPaycash"       -> number,
      "shopId"                    -> shopIdM,
      "invoiceId"                 -> invoiceIdM,
      "customerNumber"            -> personIdM,
      "md5"                       -> md5M,
      "orderNumber"               -> longNumber
    )
    { MYakaReq.apply }
    { MYakaReq.unapply }

    Form(m)
  }


  /**
    * Посчитать md5-сумму на основе подписываемых полей запроса и рассчётной стоимости заказа.
    * @param yReq Подписываемые поля реквеста яндекс-кассы.
    * @param price Рассчётная стоимость заказа.
    * @return Рассчётная строка.
    */
  def getMd5(yReq: IYakaReqSigned, price: MPrice): String = {
    assertEnabled()
    // MD5(action;суммазаказа;orderSumCurrencyPaycash;orderSumBankPaycash;shopId;invoiceId;customerNumber;shopPassword)
    val amount = TplDataFormatUtil.formatPriceAmountPlain(price)
    val str = s"${yReq.action};$amount;${price.currency.iso4217};${yReq.bankId};${yReq.shopId};${yReq.invoiceId};${yReq.personId};${YAKA_MD5_PASSWORD.get}"
    DigestUtils.md5Hex(str)
  }


  /**
    * Проверить собранные биллингом цены на предмет возможности оплаты такого ордера через яндекс-кассу.
    *
    * @param payPrices Собранные цены в валютах. По идее тут только одна цена в одной валюте.
    * @return Инстанс MPrice с ценником.
    *         Exception, если хоть одна из проверок не пройдена.
    */
  def assertPricesForPay(payPrices: Seq[MPrice]): MPrice = {
    lazy val logPrefix = s"_getPayPrice():"
    val ppsSize = payPrices.length
      if (ppsSize == 0) {
        // Нечего платить. По идее, деньги должны были списаться на предыдущем шаге биллинга.
        throw new IllegalStateException(s"$logPrefix Nothing to pay remotely via yaka. User reserve are enought?")

      } else if (ppsSize == 1) {
        val pp = payPrices.head
        if (pp.currency == MCurrencies.RUB) {
          // Цена в одной единственной валюте, которая поддерживается яндекс-кассой. Вернуть её наверх.
          pp

        } else {
          // Какая-то валюта, которая не поддерживается яндекс-кассой.
          throw new IllegalArgumentException(s"$logPrefix Yaka unsupported currency: ${pp.currency}. Price = $pp")
        }

      } else {
        // Сразу несколько валют. Не поддерживается яндекс.кассой.
        throw new UnsupportedOperationException(s"$logPrefix too many currencies need to pay, but yaka supports only RUB:\n$payPrices")
      }
  }

}
