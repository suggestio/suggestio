package io.suggest.sc.resp

import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.ScConstants.Resp._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 15:01
  * Description: Модель sc3-экшена с данными ответа сервера.
  * Наследница v2-модели из [www]:MScResp.
  */
object MSc3RespAction {

  implicit val MSC3_RESP_ACTION_FORMAT: OFormat[MSc3RespAction] = (
    (__ \ ACTION_FN).format[MScRespActionType] and
    (__ \ INDEX_RESP_ACTION).formatNullable[MSc3IndexResp]
  )(apply, unlift(unapply))

}

case class MSc3RespAction(
                           acType    : MScRespActionType,
                           index     : Option[MSc3IndexResp]      = None
                         )
