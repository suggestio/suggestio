package util.pay.yaka

import models.mpay.yaka._
import play.api.data._
import Forms._
import javax.inject.{Inject, Singleton}

import io.suggest.bill._
import io.suggest.common.empty.EmptyUtil
import io.suggest.es.model.IEsModelDiVal
import io.suggest.pay.{IMPaySystem, MPaySystems}
import io.suggest.text.util.TextHashUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mpay.{MPayMode, MPayModes}
import org.apache.commons.codec.digest.DigestUtils
import play.api.Configuration
import util.{FormUtil, TplDataFormatUtil}
import play.api.http.HttpVerbs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.17 16:26
  * Description: Разная утиль для взаимодействия с яндекс-кассой, чтобы не перегружать контроллер лишним кодом.
  */
@Singleton
class YakaUtil @Inject() (mCommonDi: IEsModelDiVal)
  extends MacroLogsImpl
  with IMPaySystem
{

  import mCommonDi.configuration

  /** id магазина в системе яндекс-кассы.
    * По идее, всегда одинков, т.к. это номер единственного аккаунта по БД яндекса.
    */
  private def SHOP_ID = 84780L

  private def CONF_PREFIX = "sio.pay.yaka."

  override def paySystem = MPaySystems.YaKa


  /** Реализация IYakaConf. */
  private case class YakaProfile(
                                  override val scId         : Long,
                                  override val mode         : MPayMode,
                                  override val md5Password  : Option[String]
                                )
    extends IYakaProfile
  {

    /** ID магазина всегда стабилен и не зависит от режима работы. */
    override final def shopId = SHOP_ID

    override def eshopActionMethod = HttpVerbs.POST

    /** URL экшена оплаты. */
    override def eshopActionUrl: String = {
      val sb = new StringBuilder(40)
        .append( "https://" )
      // Для демо-кассы надо demo-домен юзать.
      if (isDemo)
        sb.append( "demo" )
      sb.append( "money.yandex.ru/eshop.xml" )
        .toString()
    }

    override final def toString = super.toString
  }


  /** Доступные для работы конфигурации яндекс-кассы. */
  val PROFILES: Map[MPayMode, IYakaProfile] = {
    val iter = for {
      confSeq <- configuration.getOptional[Seq[Configuration]](CONF_PREFIX + "profiles").iterator
      conf    <- confSeq
      scId    <- conf.getOptional[Long]("scid")
      modeId  <- conf.getOptional[String]("mode")
    } yield {
      val mode = MPayModes.withName(modeId)
      val yProf = YakaProfile(
        scId = scId,
        mode = mode,
        md5Password = conf.getOptional[String]("password")
      )
      LOGGER.debug(s"PROFILES: Found profile $yProf")
      mode -> yProf
    }
    iter.toMap
  }

  LOGGER.info(s"${PROFILES.size} profiles total: ${PROFILES.mkString("\n ", " \n ", "")}")


  /** Разрешён ли тестовый профиль для не-суперюзеров?
    * @return false почти всегда.
    *         true, если доступа к продакшену ещё пока не выдано яндекс-кассой или не описано в конфиге.
    */
  lazy val DEMO_ALLOWED_FOR_ALL: Boolean = {
    val r = !PROFILES.contains( MPayModes.Production )

    if (r)
      LOGGER.warn("!!!DEMO ALLOWED FOR ALL!!! Looks like, prod profile does not exists.")

    r
  }


  def PRODUCTION_OPT  = PROFILES.get( MPayModes.Production )
  def PRODUCTION      = PROFILES( MPayModes.Production )
  def DEMO            = PROFILES( MPayModes.Testing )


  object ErrorCodes {
    def NO_ERROR    = 0
    def MD5_ERROR   = 1
    def ORDER_ERROR = 100
    def BAD_REQUEST = 200
  }


  // ------------- Утиль для сборка маппингов форм. --------------
  /** Поле экшена яндекс-кассы. */
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


  def currencyM(profile: IYakaProfile): Mapping[MCurrency] = {
    val off = currencyIdOffset(profile)
    FormUtil.currency_iso4217(off)
  }


  /**
    * В демо-режиме только демо-рубли. И их код 10643.
    * В prod-режиме: рубли и код 643.
    * https://tech.yandex.ru/money/doc/payment-solution/deposition/test-data-docpage/
    * В обычном режиме -- используется стандартный код валюты.
    */
  def currencyIdOffset(profile: IYakaProfile): Int = {
    if (profile.isDemo) 10000 else 0
  }

  /** Маппинг для данных запроса check и aviso.
    *
    * Тело запроса содержит данные формы такого вида:
    * {{{
    *   orderNumber -> 617
    *   orderSumAmount -> 2379.07
    *   cdd_exp_date -> 1117
    *   shopArticleId -> 391660
    *   paymentPayerCode -> 4100322062290
    *   cdd_rrn -> 217175135289
    *   external_id -> deposit
    *   paymentType -> AC
    *   requestDatetime -> 2017-02-14T10:38:38.971+03:00
    *   depositNumber -> 97QmIfZ5P9JSF-mqD0uEbYeRXY0Z.001f.201702
    *   nst_eplPayment -> true
    *   cps_user_country_code -> PL
    *   cdd_response_code -> 00
    *   orderCreatedDatetime -> 2017-02-14T10:38:38.780+03:00
    *   sk -> yde0255436f276b59f1642648b119b0d0
    *   action -> checkOrder
    *   shopId -> 84780
    *   scid -> 548806
    *   shopSumBankPaycash -> 1003
    *   shopSumCurrencyPaycash -> 10643
    *   rebillingOn -> false
    *   orderSumBankPaycash -> 1003
    *   cps_region_id -> 2
    *   orderSumCurrencyPaycash -> 10643
    *   merchant_order_id -> 617_140217103753_00000_84780
    *   unilabel -> 2034c791-0009-5000-8000-00001cc939aa
    *   cdd_pan_mask -> 444444|4448
    *   customerNumber -> rosOKrUOT4Wu0Bj139F1WA
    *   yandexPaymentId -> 2570071018240
    *   invoiceId -> 2000001037346
    *   shopSumAmount -> 2295.80
    *   md5 -> AD3E905D6F489F1AD7F2F302D2982B1B
    * }}}
    */
  def md5Form(profile: IYakaProfile): Form[MYakaReq] = {
    import io.suggest.pay.yaka.YakaConst._
    val m = mapping(
      ACTION_FN                   -> yakaActionM,
      AMOUNT_FN                   -> amountM,
      CURRENCY_FN                 -> currencyM(profile),
      BANK_ID_FN                  -> number,
      SHOP_ID_FN                  -> shopIdM,
      INVOICE_ID_FN               -> invoiceIdM,
      PERSON_ID_FN                -> personIdM,
      MD5_FN                      -> md5M,
      ORDER_ID_FN                 -> longNumber,
      SIO_NODE_ID_FN              -> FormUtil.esIdM
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
  def getMd5(profile: IYakaProfile, yReq: IYakaReqSigned, price: IPrice): String = {
    profile.md5Password.fold[String] {
      throw new IllegalStateException(s"Yandex.kassa has no md5-password configured/enabled. mode=${profile.mode} scid=${profile.scId}")
    } { md5Password =>
      // MD5(action;суммазаказа;orderSumCurrencyPaycash;orderSumBankPaycash;shopId;invoiceId;customerNumber;shopPassword)
      val amount = TplDataFormatUtil.formatPriceAmountPlain(price)

      val d = ';'
      val str = new StringBuilder(128)    // ~106-112 примерная макс.длина этой строки в нашем случае.
        .append( yReq.action.strId ).append(d)
        .append( amount ).append(d)
        .append( price.currency.iso4217 + currencyIdOffset(profile) ).append(d)
        .append( yReq.bankId ).append(d)
        .append( SHOP_ID ).append(d)
        .append( yReq.invoiceId ).append(d)
        .append( yReq.personId ).append(d)
        .append( md5Password )
        .toString()

      val md5Str = DigestUtils.md5Hex(str).toUpperCase()
      // Закоменчено, чтобы не сорить паролями в логах.
      //LOGGER.trace(s"getMd5($yReq, $price): done:\n text for md5 = $str\n md5 = $md5Str")
      md5Str
    }
  }

  def getMd5(profile: IYakaProfile, yReq: IYakaReqSigned): String = {
    getMd5(profile, yReq, yReq)
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
