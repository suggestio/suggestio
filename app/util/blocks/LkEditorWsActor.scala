package util.blocks

import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.json.{JsString, JsObject}
import util.{PlayLazyMacroLogsImpl, PlayMacroLogsImpl}
import util.img.MainColorDetector
import util.img.MainColorDetector.ImgBgColorUpdateAction
import util.ws._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.11.14 18:32
 * Description: Актор, обслуживающий WebSocket-интерфейс редактора блоков.
 */
object LkEditorWsActor {
  
  def props(out: ActorRef, wsId: String) = Props(LkEditorWsActor(out, wsId))

}


/**
 * Реализация актора для редактора в личном кабинете.
 * @param out ActorRef, который отражает собой исходящий канал ws-сокета.
 * @param wsId id ws-сокета, генерируемый в шаблоне при необходимости.
 */
case class LkEditorWsActor(out: ActorRef, wsId: String)
  extends WsActorDummy
  with SubscribeToWsDispatcher
  with ColorDetectedWsNotifyActor
  with PlayLazyMacroLogsImpl
