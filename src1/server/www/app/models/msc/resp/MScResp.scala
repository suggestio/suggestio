package models.msc.resp

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.sc.ScConstants.Resp.RESP_ACTIONS_FN

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.16 18:53
  * Description: JSON-модель-контейнер абстрактного ответа выдачи.
  * Ответ содержит более конкретные экшены.
  * Модель ориентирована на дальнейшее наращивание поддержки работы через websocket.
  */
object MScResp {

  /** Поддержка сериализации в JSON. */
  implicit val WRITES: OWrites[MScResp] = {
    (__ \ RESP_ACTIONS_FN).write[Seq[MScRespAction]]
      .contramap[MScResp](_.scActions)
  }

}

case class MScResp(
  scActions: Seq[MScRespAction]
)
