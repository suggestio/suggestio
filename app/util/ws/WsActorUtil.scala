package util.ws

import akka.actor.Actor

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 10:24
 * Description: Базовая нарезка из трейтов для быстрой сборки акторов.
 * play-2.3.x: WS-акторы появляются с адресами вида akka://application/system/websockets/554/handler
 */


/** Для stackable-акторов нужна базовая реализация метода receive(). */
trait WsActorDummy extends Actor {
  override def receive: Receive = PartialFunction.empty
}


/** WS акторы обычно подписываются на диспатчер, чтобы получить связь с внешним миром через wsId. */
trait SubscribeToWsDispatcher extends Actor {

  def wsId: String

  override def preStart(): Unit = {
    super.preStart()
    WsDispatcherActor.actorSelection ! WsActStarted(wsId)
  }

  override def postStop(): Unit = {
    super.postStop()
    WsDispatcherActor.actorSelection ! WsActStopped(wsId)
  }

}

