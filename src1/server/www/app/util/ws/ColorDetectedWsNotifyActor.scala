package util.ws

import akka.actor.{ActorRef, Actor}
import models.im.Histogram
import play.api.libs.json.{JsValue, JsArray, JsString, JsObject}
import util.PlayMacroLogsI
import io.suggest.ad.form.AdFormConstants.{WS_MSG_DATA_FN, WS_MSG_TYPE_FN, TYPE_COLOR_PALETTE}
import util.img.detect.main.{ImgBgColorUpdateAction, Update}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 10:20
 * Description: Уведомление об определении цвета картинки лежит здесь.
 */

trait ColorDetectedWsNotifyActor extends Actor with PlayMacroLogsI {

  def out: ActorRef

  abstract override def receive: Receive = super.receive orElse {

    // Пришла гистограмма с палитрой предлагаемых цветов.
    case h: Histogram =>
      //LOGGER.trace(s"Forwarding color histogram info to $out\n$h")
      val data = JsArray( h.sorted.map(c => JsString(c.colorHex)) )
      out ! _toWsMsgJson(TYPE_COLOR_PALETTE, data)

    // Пришло сообщение, что успешно выявлен цвет картинки
    case Update(newColorHex) =>
      //LOGGER.trace(s"Sending detected $newColorHex color to client via ws...")
      val data = JsString(newColorHex)
      out ! _toWsMsgJson("imgColor", data)

    // Пришло сообщение о каких-то странностях при определении цвета:
    case detectedColorInfo: ImgBgColorUpdateAction =>
      LOGGER.trace {
        val r = sender()
        s"unexpected color-detecting msg received from $r: $detectedColorInfo"
      }
      // do nothing

  }

  protected def _toWsMsgJson(typ: String, data: JsValue): JsValue = {
    JsObject(Seq(
      WS_MSG_TYPE_FN  -> JsString(typ),
      WS_MSG_DATA_FN  -> data
    ))
  }

}
