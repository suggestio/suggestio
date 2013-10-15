package util

import scala.concurrent.duration._
import akka.actor._
import io.suggest.event._
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
object NewsQueue4Play extends NewsQueueStaticT {

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

  private val ensureActorDuration = 2 second
  private implicit val supRefAskTimeout = Timeout(ensureActorDuration)

  /**
   * Убедится, что актор для указанной очереди (указанного хоста и типа) запущен.
   * Если актор не существует, то он будет запущен супервизором.
   * @param dkey ключ домена
   * @param typ "тип", разграничивающий очереди в рамках домена
   * @return Future[ActorRef]
   */
  def ensureActorFor(dkey:String, typ:String) : Future[ActorRef] = {
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
   * Узнать ref актора очереди, если тот запущен.
   * @param dkey ключ домена
   * @param typ "тип" очереди, ключ для разграничения очередей в рамках домена.
   * @return Фьючерс с опшином, содержащим ActorRef очереди.
   */
  def getActorFor(dkey:String, typ:String) : Future[Option[ActorRef]] = {
    (supRef ? GetNQ(dkey, typ)).asInstanceOf[Future[Option[ActorRef]]]
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
    ensureActorFor(dkey, typ) flatMap ping
  }

  def stop(nqActorRef: ActorRef) {
    supRef ! StopActor(nqActorRef)
  }

  /**
   * Остановить очередь, относящуюся к указанному домену и "типу".
   * @param dkey ключ домена
   * @param typ "Тип" для разграничения очередей в рамках домена.
   */
  def stopFor(dkey:String, typ:String) {
    supRef ! StopNQ(dkey, typ)
  }


  /**
   * Сообщение, говорящее супервизору убедится в том, что необходимый менеджер очереди новостей работает и готов.
   * @param dkey ключ домена
   * @param typ некий "тип". Нужно для разграничения очередей новостей внутри домена.
   */
  sealed case class EnsureNQ(dkey:String, typ:String)

  /**
   * Сообщение, запрашивающее у супервизора данные по актору очереди, который может быть не запущен.
   * @param dkey ключ домена
   * @param typ "тип". Нужно для разграничения очередей новостей внутри домена.
   */
  sealed case class GetNQ(dkey:String, typ:String)

  sealed case class StopNQ(dkey:String, typ:String)
  sealed case class StopActor(actorRef: ActorRef)

}


// Супервизорд для множества акторов NewsQueue4Play.
class NewsQueue4PlaySup extends Actor with Logs {
  import LOGGER._
  import NewsQueue4Play._

  def receive = {
    // Запрос запуска актора очереди, если тот не существует.
    case EnsureNQ(dkey, typ) =>
      val name = nqActorName(dkey, typ)
      lazy val logPrefix = s"EnsureNQ($dkey, $typ): childName => $name: "
      val childRef: ActorRef = context.child(name) match {
        case None =>
          val ref = context.actorOf(Props[NewsQueue4PlayActor], name = name)
          trace(logPrefix + "New child started at " + ref)
          ref

        case Some(ref) =>
          trace(logPrefix + "Child already exists: " + ref)
          ref
      }
      sender ! childRef

    // Запрос наличия очереди.
    case GetNQ(dkey, typ) =>
      val name = nqActorName(dkey, typ)
      val result = context.child(name)
      trace(s"GetNQ($dkey, $typ): child with name=$name is $result")
      sender ! result

    // Остановить указанного актора.
    case StopActor(actorRef) =>
      trace(s"StopActor($actorRef)")
      context.stop(actorRef)

    // Остановить актора очереди.
    case StopNQ(dkey, typ) =>
      val name = nqActorName(dkey, typ)
      lazy val logPrefix = s"StopNQ($dkey, $typ): name => $name: "
      context.child(name) match {
        case Some(ref) =>
          debug(logPrefix + "stopping child" + ref)
          context.stop(ref)

        case None =>
          debug(logPrefix + "No child exist for name=" + name)
      }
  }


  override def supervisorStrategy: SupervisorStrategy = {
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _:Throwable => Restart
      case _ => Stop
    }
  }

  private def nqActorName(dkey:String, typ:String) = dkey + "~" + typ
}


// Самая простая реализация актора NewsQueue с логгированием через play.
// Должна использоваться как основа для других реализаций очередей в рамках sioweb21.
class NewsQueue4PlayActor extends NewsQueueAbstract with SioutilLogs

