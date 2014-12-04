package util.img

import akka.actor.{Props, ActorRef}
import util.PlayLazyMacroLogsImpl
import util.ws._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 10:51
 * Description: Вебсокет-актор, используемый в качестве backend'а для связи с клиентом во время его работы в редакторе.
 * На момент создания точно совпадал с LkEditorWsActor, т.к. оба поддерживали только одну функцию: уведомления
 * о детектирование цвета.
 */
object SysGalEditWsActor {
  def props(out: ActorRef, wsId: String) = Props(SysGalEditWsActor(out, wsId))
}

case class SysGalEditWsActor(out: ActorRef, wsId: String)
  extends WsActorDummy
  with SubscribeToWsDispatcher
  with ColorDetectedWsNotifyActor
  with PlayLazyMacroLogsImpl
