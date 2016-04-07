package util.blocks

import akka.actor.{ActorRef, Props}
import com.google.inject.Inject
import com.google.inject.assistedinject.Assisted
import util.PlayLazyMacroLogsImpl
import util.ws._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.11.14 18:32
 * Description: Актор, обслуживающий WebSocket-интерфейс редактора блоков.
 */
class LkEditorWsActors @Inject() (
  factory: ILkEditorWsActorFactory
) {

  def props(out: ActorRef, wsId: String) = {
    Props( factory.create(out, wsId) )
  }

}


/** Интерфейс Guice-сборщика инстансов [[LkEditorWsActor]]. */
trait ILkEditorWsActorFactory {
  def create(out: ActorRef, wsId: String): LkEditorWsActor
}


/**
 * Реализация актора для редактора в личном кабинете.
 * @param out ActorRef, который отражает собой исходящий канал ws-сокета.
 * @param wsId id ws-сокета, генерируемый в шаблоне при необходимости.
 */
case class LkEditorWsActor @Inject() (
  @Assisted out                   : ActorRef,
  @Assisted wsId                  : String,
  override val wsDispatcherActors : WsDispatcherActors
)
  extends WsActorDummy
  with SubscribeToWsDispatcher
  with ColorDetectedWsNotifyActor
  with PlayLazyMacroLogsImpl
{

  import LOGGER._

  override def postStop(): Unit = {
    super.postStop()
    trace(s"Stopping actor for wsId=$wsId and out=$out")
  }

}
