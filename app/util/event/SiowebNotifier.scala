package util.event

import io.suggest.event._
import util._
import akka.actor.{ActorRef, ActorRefFactory, ActorSystem, Props}
import akka.util.Timeout
import com.google.inject.{Inject, Singleton}
import models.MNodeCache

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import play.api.Configuration
import play.api.inject.Injector

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.10.13 14:50
 * Description: Поддержка SioNotifier с уклоном на текущий проект.
 * Полностью статический клиент + реализация, подписывающая всех вокруг на события.
 */
@Singleton
class SiowebNotifier @Inject() (
  factory       : ISiowebNotifierActorsFactory,
  injector      : Injector,
  siowebSup     : SiowebSup,
  configuration : Configuration,
  actorSystem   : ActorSystem
)
  extends SioNotifierStaticActorSelection
  with SNStaticSubscriptionManager
{

  implicit val SN_ASK_TIMEOUT: Timeout = {
    val ms = configuration.getInt("sn.ask.timeout_ms").getOrElse(5000)
    Timeout( ms.milliseconds )
  }

  def supPath = siowebSup.actorPath

  protected def getSystem = actorSystem


  /** Набор модулей, которые необходимо статически подписать на события. */
  // TODO Вынести это отсюда?
  protected def getStaticSubscribers: Seq[SNStaticSubscriber] = {
    List(
      injector.instanceOf[MNodeCache],
      injector.instanceOf[AdnNodeEvents]
    )
  }

  /** SiowebSup собирается запустить сие. */
  def startLink(arf: ActorRefFactory): ActorRef = {
    val ref = arf.actorOf(Props(factory.create()), name = actorName)
    staticSubscribeAllSync()
    ref
  }

}


trait ISiowebNotifierActorsFactory {
  def create(): SiowebNotifierActor
}

class SiowebNotifierActor @Inject() (
  override val ec: ExecutionContext
)
  extends SioNotifier
    with SioutilLogs
