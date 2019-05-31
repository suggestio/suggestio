package models.sms.smsRu

import io.suggest.bill.{MCurrencies, MPrice}
import models.sms.ISmsSendResult
import play.api.libs.json._
import play.api.libs.functional.syntax._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.05.19 23:28
  * Description: Результат запроса отправки смс.
  */
object MSmsRuResult {

  /** Поля ответа. */
  object R {
    def status      = "status"
    def statusCode  = "status_code"
    def sms         = "sms"
    def smsId       = "sms_id"
    def statusText  = "status_text"
    def balance     = "balance"
  }


  /** Допустимые значения resp.status. */
  object Status {
    def OK = "OK"
    def ERROR = "ERROR"
  }


  implicit def smsRuResultJson: Reads[MSmsRuResult] = (
    (__ \ R.status).read[String] and
    (__ \ R.statusCode).read[Int] and
    (__ \ R.statusText).readNullable[String] and
    (__ \ R.sms).read[Map[String, MSmsRuSendStatus]] and
    (__ \ R.balance).readNullable[Double]
  )(apply _)

}


final case class MSmsRuResult(
                               status                   : String,
                               statusCode               : Int,
                               override val statusText  : Option[String],
                               override val smsInfo     : Map[String, MSmsRuSendStatus],
                               amountRub                : Option[Double],
                             )
  extends ISmsSendResult
{

  override def isOk: Boolean =
    status ==* MSmsRuResult.Status.OK

  override def restBalance: Option[MPrice] =
    amountRub.map( MPrice.fromRealAmount(_, MCurrencies.RUB ))

  override def requestId = None

}
