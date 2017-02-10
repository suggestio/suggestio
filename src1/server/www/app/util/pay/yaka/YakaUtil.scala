package util.pay.yaka

import models.mpay.yaka.{IYakaReqSigned, MYakaAction, MYakaActions, MYakaReq}
import play.api.data._
import Forms._
import com.google.inject.Inject
import io.suggest.bill.MPrice
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

  /** id витрины. На продакшене задаётся в конфиге.
    * 548806 -- демо-витрина, выданная при первом подключении.
    */
  val SC_ID: Long = configuration.getLong("sio.pay.yaka.scid").getOrElse {
    val demoScId = 548806L
    LOGGER.info("DEMO pay mode, scid = " + demoScId)
    demoScId
  }

  /** Пароль для подписывания данных при MD5-режиме. Задаётся только в конфиге. */
  private val YAKA_MD5_PASSWORD: Option[String] = {
    val ck = "sio.pay.yaka.password"
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

  /** Маппинг для данных запроса check и aviso. */
  def md5Form: Form[MYakaReq] = {
    val m = mapping(
      "action"                    -> yakaActionM,
      "orderSumAmount"            -> amountM,
      "orderSumCurrencyPaycash"   -> FormUtil.currencyM,
      "orderSumBankPaycash"       -> number,
      "shopId"                    -> shopIdM,
      "invoiceId"                 -> invoiceIdM,
      "customerNumber"            -> personIdM,
      "md5"                       -> md5M
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


}
