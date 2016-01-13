package util.event

import io.suggest.event._
import models.adv.DeleteAllAdvsOnAdDeleted
import models.mbill.DelContractsWhenNodeDeleted
import util._
import akka.actor.{Props, ActorRef, ActorRefFactory}
import akka.util.Timeout
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models._

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.10.13 14:50
 * Description: Поддержка SioNotifier с уклоном на текущий проект.
 * Полностью статический клиент + реализация, подписывающая всех вокруг на события.
 */
object SiowebNotifier
  extends SioNotifierStaticActorSelection
  with SNStaticSubscriptionManager
{

  object Implicts {
    implicit def sn = SiowebNotifier
  }

  implicit val SN_ASK_TIMEOUT: Timeout = {
    val ms = current.configuration.getInt("sn.ask.timeout_ms") getOrElse 5000
    Timeout( ms.milliseconds )
  }

  // TODO Сделать нормальную инжекцию.
  private lazy val siowebSup = current.injector.instanceOf[SiowebSup]
  def supPath = siowebSup.actorPath

  protected def getSystem = Akka.system

  private def _inj[X <: SNStaticSubscriber : ClassTag]: X = {
    current.injector.instanceOf[X]
  }

  /** Набор модулей, которые необходимо статически подписать на события. */
  // TODO Вынести это отсюда?
  protected def getStaticSubscribers: Seq[SNStaticSubscriber] = {
    List(
      // TODO inject
      _inj[MNodeCache],
      _inj[DeleteAdsOnAdnNodeDeleteSubscriber],
      _inj[DeleteAllAdvsOnAdDeleted],
      _inj[DelContractsWhenNodeDeleted],
      _inj[AdnNodeEvents]
    )
  }

  /** SiowebSup собирается запустить сие. */
  def startLink(arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props[SiowebNotifier], name = actorName)
  }

  /** Сабжевый актор стартанул. Надо выполнить асинхронно какие-то действия.
   * Вызывается из preStart() актора.
   */
  private def snAfterStartAsync = {
    Future {
      staticSubscribeAllSync()
    }
  }

}


class SiowebNotifier extends SioNotifier with SioutilLogs {

  override def ec = defaultContext

  // После запуска надо подписаться на статические события проекта.
  override def preStart(): Unit = {
    trace(s"preStart(): my actor path = " + self.path)
    super.preStart()
    SiowebNotifier.snAfterStartAsync
  }

}
