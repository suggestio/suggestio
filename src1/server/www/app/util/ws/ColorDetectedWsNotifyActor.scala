package util.ws

import akka.actor.{Actor, ActorRef}
import io.suggest.color.MHistogram
import io.suggest.util.logs.IMacroLogs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 10:20
 * Description: Уведомление об определении цвета картинки лежит здесь.
 */
trait ColorDetectedWsNotifyActor extends Actor with IMacroLogs {

  // TODO Удалить вслед за старым редактором карточек.

  import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}
  import io.suggest.ad.form.AdFormConstants.{TYPE_COLOR_PALETTE, WS_MSG_DATA_FN, WS_MSG_TYPE_FN}

  def out: ActorRef

  abstract override def receive: Receive = super.receive.orElse {

    // Пришла гистограмма с палитрой предлагаемых цветов.
    case h: MHistogram =>
      //LOGGER.trace(s"Forwarding color histogram info to $out\n$h")
      val data = JsArray( h.sorted.map(c => JsString(c.code)) )
      out ! _toWsMsgJson(TYPE_COLOR_PALETTE, data)

  }

  private def _toWsMsgJson(typ: String, data: JsValue): JsValue = {
    JsObject(Seq(
      WS_MSG_TYPE_FN  -> JsString(typ),
      WS_MSG_DATA_FN  -> data
    ))
  }

}
