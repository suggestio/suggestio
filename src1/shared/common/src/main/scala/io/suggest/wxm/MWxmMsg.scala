package io.suggest.wxm

import boopickle.Default._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 22:01
  * Description: Модель бинарного WebSocket message с запросом или ответом.
  *
  * Сообщение не имеет направленности между клиентом и сервером, являясь универсальным протоколом общения,
  * не зависящим от нижележащего протокола связи (XHR, WebSocket, etc).
  */

/** Модель одного куска сообщения WXM. */
object MWxmMsg {

  /** pickler для boopickle */
  implicit def mWxmMsgPartPickler[T: Pickler]: Pickler[MWxmMsg[T]] = {
    generatePickler[MWxmMsg[T]]
  }

  /** pickler для play-json. */
  implicit def mWxmMsgPartPjFormat[T: Format]: OFormat[MWxmMsg[T]] = {
    (
      (__ \ "i").formatNullable[WxmMsgId_t] and
      (__ \ "d").format[T]
    )(apply, unlift(unapply))
  }

}

/** Класс модели одной части одного сообщения WXM.
  *
  * @param id id сообщения, присвоенный wxm.
  *           id может отсутствовать, если отслеживать сообщение не требуется: отправил и забыл.
  * @param payload Суть сообщений типа T.
  * @tparam T Тип сообщения.
  */
case class MWxmMsg[T](
                       id           : Option[WxmMsgId_t],
                       payload      : T
                     )