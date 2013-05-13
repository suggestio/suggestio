package util

import scala.concurrent.duration._
import akka.actor._
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor.SupervisorStrategy._
import scala.concurrent.{Future, Await}
import scala.Some
import akka.actor.OneForOneStrategy
import akka.pattern.ask
import akka.util.Timeout

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.05.13 16:56
 * Description: Реализация систем NewsQueue для веб-морды на базе play. Тут супервизор, реализация класса
 * и статический frontend.
 */

// Статическое управлением всем добром для NewsQueue. Прозрачно резолвит идентификаторы акторов, управляет ими, обращается к ним.
object NewsQueue4Play {

  import NewsQueue._

  val sup_name = "nq4playSup"

  protected var supRef : ActorRef = null

  /**
   * Запуск актора супервизора. Функция сохраняет actorRef в состояние этого объекта-клиента.
   * @param arf ActorRefFactory
   * @return ActorRef
   */
  def startLinkSup(arf:ActorRefFactory) = {
    supRef = arf.actorOf(Props[NewsQueue4PlaySup], name=sup_name)
    supRef
  }

  /**
   * Убедится, что актор для указанной очереди (указанного хоста и типа) запущен.
   * @param dkey ключ домена
   * @param typ "тип", разграничивающий очереди в рамках домена
   * @return Future[ActorRef]
   */
  def ensureActorFor(dkey:String, typ:String) = {
    implicit val timeout = Timeout(1 second)
    (supRef ? EnsureNQ(dkey, typ)).asInstanceOf[Future[ActorRef]]
  }

  /**
   * Отправить новость в очередь новостей.
   * @param dkey ключ домена
   * @param typ "тип"
   * @param news новостЬ
   */
  def pushTo(dkey:String, typ:String, news:NewsEventT) {
    ensureActorFor(dkey, typ) onSuccess {
      case actorRef => push(actorRef, news)
    }
  }


  def longPollAsyncFrom(dkey:String, typ:String, timestampMs:Long, timeout:FiniteDuration) = {
    ensureActorFor(dkey, typ) map { actorRef =>
      longPoll(actorRef, timestampMs, timeout)
    }
  }


  /**
   * Блокирующий вызов к очереди новостей.
   * @param dkey ключ домена
   * @param typ имя очереди в рамках домена
   * @param timestampMs минимальное время появления новости
   * @param timeout таймаут ожидания свежих новостей, если таких нет.
   * @return NewsReply
   */
  def longPollSyncFrom(dkey:String, typ:String, timestampMs:Long, timeout:FiniteDuration) = {
    val fut1 = longPollAsyncFrom(dkey, typ, timestampMs, timeout)
    val fut  = Await.result(fut1, timeout)
    Await.result(fut, timeout + 1.second)
  }


  /**
   * Асинхронное обращение за фьючерсом
   * @param dkey
   * @param typ
   * @param timestampMs
   * @return
   */
  def shortPullAsyncFrom(dkey:String, typ:String, timestampMs:Long) = {
    ensureActorFor(dkey, typ) map { actorRef =>
      shortPull(actorRef, timestampMs)
    }
  }


  val maxAwaitShortPull = 2.seconds

  def shortPullSyncFrom(dkey:String, typ:String, timestampMs:Long) = {
    val fut1 = shortPullAsyncFrom(dkey, typ, timestampMs)
    val fut  = Await.result(fut1, maxAwaitShortPull)
    Await.result(fut, maxAwaitShortPull)
  }

}


// Супервизорд для множества акторов NewsQueue4Play.
class NewsQueue4PlaySup extends Actor {

  def receive = {

    // Запрос резолва имени актора.
    case EnsureNQ(dkey, typ) =>
      val name = dkey + "/" + typ
      val childRef = context.child(name) match {
        case None => context.actorOf(Props[NewsQueue4PlayActor], name = name)
        case Some(actorRef) => actorRef
      }
      sender ! childRef
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _:Exception => Restart
  }

}


/**
 * Сообщение, говорящее супервизору убедится в том, что необходимый менеджер очереди новостей работает и готов.
 * @param dkey ключ домена
 * @param typ некий "тип". Нужно для разграничения очередей новостей внутри домена.
 */
protected case class EnsureNQ(dkey:String, typ:String)


// Самая простая реализация актора NewsQueue с логгированием через play.
// Должна использоваться как основа для других реализаций в рамках sioweb.
class NewsQueue4PlayActor extends NewsQueueAbstract with SioutilLogs
