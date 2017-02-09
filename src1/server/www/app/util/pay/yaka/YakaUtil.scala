package util.pay.yaka

import models.mpay.yaka.{IYakaReqSigned, MYakaAction, MYakaActions, MYakaReq}
import play.api.data._
import Forms._
import com.google.inject.Inject
import io.suggest.bill.MPrice
import io.suggest.common.empty.EmptyUtil
import io.suggest.es.model.IEsModelDiVal
import io.suggest.text.util.TextHashUtil
import org.apache.commons.codec.digest.DigestUtils
import util.{FormUtil, TplDataFormatUtil}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.17 16:26
  * Description: Разная утиль для взаимодействия с яндекс-кассой, чтобы не перегружать контроллер лишним кодом.
  */
class YakaUtil @Inject() (mCommonDi: IEsModelDiVal) {

  import mCommonDi.configuration

  /** Пароль для подписывания данных при MD5-режиме. */
  private val YAKA_MD5_PASSWORD = configuration.getString("sio.pay.yaka.password").get


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
    // MD5(action;суммазаказа;orderSumCurrencyPaycash;orderSumBankPaycash;shopId;invoiceId;customerNumber;shopPassword)
    val amount = TplDataFormatUtil.formatPriceAmountPlain(price)
    val str = s"${yReq.action};$amount;${price.currency.iso4217};${yReq.bankId};${yReq.shopId};${yReq.invoiceId};${yReq.personId};$YAKA_MD5_PASSWORD"
    DigestUtils.md5Hex(str)
  }


}
