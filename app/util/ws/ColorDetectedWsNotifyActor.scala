package util.ws

import akka.actor.{ActorRef, Actor}
import models.im.Histogram
import play.api.libs.json.{JsArray, JsString, JsObject}
import util.PlayMacroLogsI
import util.img.MainColorDetector
import util.img.MainColorDetector.ImgBgColorUpdateAction

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
      LOGGER.trace(s"Forwarding color histogram info to $out\n$h")
      val respJson = JsObject(Seq(
        "type" -> JsString("colorPalette"),
        "data" -> JsArray( h.sorted.map(c => JsString(c.colorHex)) )
      ))
      out ! respJson

    // Пришло сообщение, что успешно выявлен цвет картинки
    case MainColorDetector.Update(newColorHex) =>
      LOGGER.trace(s"Sending detected $newColorHex color to client via ws...")
      val respJson = JsObject(Seq(
        "type"  -> JsString("imgColor"),
        "data"  -> JsString(newColorHex)
      ))
      out ! respJson

    // Пришло сообщение о каких-то странностях при определении цвета:
    case detectedColorInfo: ImgBgColorUpdateAction =>
      LOGGER.trace {
        val r = sender()
        s"unexpected color-detecting msg received from $r: $detectedColorInfo"
      }
      // do nothing

  }

}
