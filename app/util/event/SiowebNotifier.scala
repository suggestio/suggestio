package util.event

import io.suggest.event._
import util.{SioutilLogs, DomainManager, SiowebSup}
import akka.actor.{Props, ActorRef, ActorRefFactory}
import akka.util.Timeout
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import scala.concurrent.future
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.10.13 14:50
 * Description: Поддержка SioNotifier с уклоном на текущий проект.
 * Полностью статический клиент + реализация, подписывающая всех вокруг на события.
 */
object SiowebNotifier extends SioNotifierStaticActorSelection with SNStaticSubscriptionManager {

  implicit val SN_ASK_TIMEOUT: Timeout = {
    val ms = current.configuration.getInt("sn.ask.timeout_ms") getOrElse 5000
    Timeout(ms milliseconds)
  }

  def supPath = SiowebSup.actorPath

  protected def getSystem = Akka.system

  /** Набор модулей, которые необходимо статически подписать на события. */
  // TODO Вынести это отсюда?
  protected def getStaticSubscribers: Seq[SNStaticSubscriber] = List(
    DomainManager
  )

  /** SiowebSup собирается запустить сие. */
  def startLink(arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props[SiowebNotifier], name=actorName)
  }

  /** Сабжевый актор стартанул. Надо выполнить асинхронно какие-то действия.
   * Вызывается из preStart() актора.
   */
  private def snAfterStartAsync = future {
    staticSubscribeAllSync()
  }
}


class SiowebNotifier extends SioNotifier with SioutilLogs {

  // После запуска надо подписаться на статические события проекта.
  override def preStart(): Unit = {
    trace(s"preStart(): my actor path = " + self.path)
    super.preStart()
    SiowebNotifier.snAfterStartAsync
  }

}
