package io.suggest.sc.sc3

import io.suggest.sc.ScConstants.Resp.RESP_ACTIONS_FN
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 14:56
  * Description: Модель-контейнер данных с ответом сервера по поводу выдачи.
  *
  * Сервер возвращает различные данные для разных экшенов, но всегда в одном формате.
  * Это неявно-пустая модель.
  */
object MSc3Resp {

  /** Поддержка play-json. */
  implicit def MSC3_RESP: OFormat[MSc3Resp] = {
    (__ \ RESP_ACTIONS_FN).format[Seq[MSc3RespAction]]
      .inmap[MSc3Resp]( apply, _.respActions )
  }

}

case class MSc3Resp(
                     respActions: Seq[MSc3RespAction]
                   )
