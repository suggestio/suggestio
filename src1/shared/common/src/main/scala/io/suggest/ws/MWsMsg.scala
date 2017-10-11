package io.suggest.ws

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 11:27
  * Description: JSON-модель контейнера websocket-сообщения.
  * Тип задаётся полем типа, а JSON-сериализация идёт в 2 эпапа: внешний контейнер, а затем внутренний на основе типа.
  */
object MWsMsg {

  object Fields {
    val TYPE_FN     = "t"
    val PAYLOAD_FN  = "p"
  }

  /** Поддержка play-json. */
  implicit val MWS_MSG_FORMAT: OFormat[MWsMsg] = {
    val F = Fields
    (
      (__ \ F.TYPE_FN).format[MWsMsgType] and
      (__ \ F.PAYLOAD_FN).format[JsValue]
    )(apply, unlift(unapply))
  }

  implicit def univEq: UnivEq[MWsMsg] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}

case class MWsMsg(
                   typ    : MWsMsgType,
                   payload: JsValue
                 )
