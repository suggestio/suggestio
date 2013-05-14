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

  val ensureActorDuration = 2 second

  /**
   * Убедится, что актор для указанной очереди (указанного хоста и типа) запущен.
   * Если актор не существует, то он будет запущен супервизором.
   * @param dkey ключ домена
   * @param typ "тип", разграничивающий очереди в рамках домена
   * @return Future[ActorRef]
   */
  def ensureActorFor(dkey:String, typ:String) : Future[ActorRef] = {
    implicit val timeout = Timeout(ensureActorDuration)
    (supRef ? EnsureNQ(dkey, typ)).asInstanceOf[Future[ActorRef]]
  }

  /**
   * Блокирующий вызов для получения актора. Враппер над ensureActorFor. Тут присутствует для целей дебага.
   * @param dkey ключ домена
   * @param typ типота
   * @return ActorRef.
   */
  def ensureActorSyncFor(dkey:String, typ:String) : ActorRef = {
    val fut = ensureActorFor(dkey, typ)
    Await.result(fut, ensureActorDuration)
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


  /**
   * Асинхронный long-polling новостей. Если есть свежие новости, то они вернутся. Если нет, то фьючерс будет ожидать их
   * не более чем timeout времени. Обработка timeout реализована на стороне сервера очереди.
   * @param dkey ключ домена
   * @param typ идентификатор очереди в рамках домена
   * @param timestampMs таймштамп для отборки новостей
   * @param timeout таймаут ожидания ответа от сервера
   * @return Фьючерс NewsReply.
   */
  def longPollFrom(dkey:String, typ:String, timestampMs:Long, timeout:FiniteDuration) : Future[NewsReply] = {
    ensureActorFor(dkey, typ) flatMap {
      longPoll(_, timestampMs, timeout)
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
  def longPollSyncFrom(dkey:String, typ:String, timestampMs:Long, timeout:FiniteDuration) : NewsReply = {
    val fut1 = longPollFrom(dkey, typ, timestampMs, timeout)
    Await.result(fut1, timeout + 1.second)
  }


  /**
   * Асинхронное короткое обращение за текущими.
   * @param dkey ключ домена
   * @param typ id очереди в рамках домена
   * @param timestampMs таймштамп для выборки новостей
   * @return
   */
  def shortPullFrom(dkey:String, typ:String, timestampMs:Long) : Future[NewsReply] = {
    ensureActorFor(dkey, typ) flatMap {
      shortPull(_, timestampMs)
    }
  }


  val maxAwaitShortPull = 2.seconds

  def shortPullSyncFrom(dkey:String, typ:String, timestampMs:Long) = {
    val fut1 = shortPullFrom(dkey, typ, timestampMs)
    Await.result(fut1, maxAwaitShortPull)
  }

  def pingFor(dkey:String, typ:String) = {
    ensureActorFor(dkey, typ) flatMap(ping(_))
  }

}


// Супервизорд для множества акторов NewsQueue4Play.
class NewsQueue4PlaySup extends Actor {

  def receive = {

    // Запрос резолва имени актора и его запуска, если тот не существует.
    case EnsureNQ(dkey, typ) =>
      val name = dkey + "~" + typ
      val childRef = context.child(name) match {
        case None =>
          context.actorOf(Props[NewsQueue4PlayActor], name = name)

        case Some(actorRef) =>
          actorRef
      }
      sender ! childRef
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
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
// Должна использоваться как основа для других реализаций очередей в рамках sioweb21.
class NewsQueue4PlayActor extends NewsQueueAbstract with SioutilLogs
