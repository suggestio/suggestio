package models.sms.smsRu

import japgolly.univeq.UnivEq
import models.sms.ISmsSendStatus
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.05.19 23:12
  * Description: Статус отправки одной смс.
  */
object MSmsRuSendStatus {

  import MSmsRuResult.R

  implicit def smsRuSendStatusJson: Reads[MSmsRuSendStatus] = (
    (__ \ R.status).read[String] and
    (__ \ R.statusCode).read[Int] and
    (__ \ R.statusText).readNullable[String] and
    (__ \ R.smsId).readNullable[String]
  )(apply _)

  implicit def univEq: UnivEq[MSmsRuSendStatus] = UnivEq.derive

}


/** Статус отправки смс на один номер через смс.ру.
  *
  * @param status Строка OK или ERROR.
  * @param statusCode Код статуса.
  * @param statusText Сообщение статуса.
  * @param smsId id отправленного смс.
  */
case class MSmsRuSendStatus(
                             status                   : String,
                             statusCode               : Int,
                             override val statusText  : Option[String],
                             override val smsId       : Option[String],
                           )
  extends ISmsSendStatus
{

  override def isOk: Boolean =
    status equalsIgnoreCase MSmsRuResult.Status.OK

}
