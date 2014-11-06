package util.blocks

import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.json.{JsString, JsObject}
import util.PlayMacroLogsImpl
import util.img.MainColorDetector
import util.img.MainColorDetector.ImgBgColorUpdateAction
import util.ws.{WsActStopped, WsActStarted, WsDispatcherActor}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.11.14 18:32
 * Description: Актор, обслуживающий WebSocket-интерфейс редактора блоков.
 */
object LkEditorWsActor extends PlayMacroLogsImpl {
  
  val TYPE_FN     = "type"
  val PAYLOAD_FN  = "data"

  def props(out: ActorRef, wsId: String) = Props(new LkEditorWsActor(out, wsId))

}

// Ws-акторы появляются с адресами вида akka://application/system/websockets/554/handler
// TODO Нужен актор-диспатчер, который будет хранить карту wsId и соотв. им actorRef.

import LkEditorWsActor._


class LkEditorWsActor(out: ActorRef, wsId: String) extends Actor {

  import LOGGER._


  override def preStart(): Unit = {
    super.preStart()
    WsDispatcherActor.actorSelection ! WsActStarted(wsId)
    trace("Started websocket actor: " + self.path)
  }

  override def receive: Receive = {

    // Пришло сообщение, что успешно выявлен цвет картинки
    case MainColorDetector.Update(newColorHex) =>
      //trace(s"Sending detected $newColorHex color to client via ws...")
      val respJson = JsObject(Seq(
        TYPE_FN     -> JsString("imgColor"),
        PAYLOAD_FN  -> JsString(newColorHex)
      ))
      out ! respJson

    // Пришло сообщение о каких-то странностях при определении цвета:
    case detectedColorInfo: ImgBgColorUpdateAction =>
      trace(s"unexpected color-detecting msg received from $sender: $detectedColorInfo")
      // do nothing
  }

  override def postStop(): Unit = {
    super.postStop()
    WsDispatcherActor.actorSelection ! WsActStopped(wsId)
    trace("Stopped websocket actor: " + self.path)
  }

}
