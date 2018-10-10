package models.mpay.yaka

import io.suggest.bill.{IMCurrency, IMPrice, MCurrency, MPrice}
import io.suggest.mbill2.m.gid.Gid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.17 16:29
  * Description: Модель запроса с платежными данными, передаваемые от яндекс-кассы.
  */


/** Интерфейс с подписанными полями яндекс-кассы. */
trait IYakaReqSigned extends IMCurrency with IMPrice {

  /** Экшен: чек, aviso. */
  val action          : MYakaAction

  /** id банка в системе яндекс-кассы. */
  val bankId          : Int

  /** id магазина. */
  val shopId          : Long

  /** id инвойса по системе яндекс-деньги. */
  val invoiceId       : Long

  /** customerNumber: id клиента, присланный из suggest.io. */
  val personId        : String

  val realAmount: Double
  final lazy val price = MPrice.fromRealAmount( realAmount, currency )

  // MD5(action;суммазаказа;orderSumCurrencyPaycash;orderSumBankPaycash;shopId;invoiceId;customerNumber;shopPassword)
}


/** Обязательные параметры запроса яндекс-кассы. */
trait IYakaReq extends IYakaReqSigned {

  /** md5-сигнатура (Контрольная сумма данных оплаты). */
  val md5             : String

  //val requestDatetime : ZonedDateTime

}


/** Класс модели данных запроса платежа. */
case class MYakaReq(
                     override val action          : MYakaAction,
                     realAmount                   : Double,
                     currency                     : MCurrency,
                     override val bankId          : Int,
                     override val shopId          : Long,
                     override val invoiceId       : Long,
                     override val personId        : String,
                     override val md5             : String,
                     orderId                      : Gid_t,
                     onNodeId                     : String
                   )
  extends IYakaReq
