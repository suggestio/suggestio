package util.blocks

import akka.actor.{ActorRef, Props}
import util.PlayLazyMacroLogsImpl
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
  with PlayLazyMacroLogsImpl {

  import LOGGER._

  override def postStop(): Unit = {
    super.postStop()
    trace(s"Stopping actor for wsId=$wsId and out=$out")
  }

}
