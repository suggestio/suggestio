package util

import akka.actor.{Cancellable, ActorRef, Actor}
import scala.collection.mutable
import scala.concurrent.duration._
import io.suggest.util.LogsAbstract

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.05.13 19:21
 * Description: Процессы очереди новостей и супервизор для них. Необходимы для поддержки новостных очередей
 * для различных задач. Основная суть - подписать процесс очереди на разные события (в т.ч. на других
 * нодах) и мониторить его содержимое асинхронно по отношению к браузеру юзера, который связывается с контроллером
 * через рвущийся comet или постоянный websocket. Процесс NewsQueue ориентирован на добавление событий через
 * наследование. Т.е. если надо выполнять какие-то действия при наступлении событий, необходимо сделать
 * override соответствующих модулей.
 *
 * Процесс очереди новостей должен автоматически очищать собственную очередь новостей и самозавершаться
 * после некоторого периода неиспользования.
 */


object NewsQueue {
  // Какой тип данных является новостью
  type NewsEventT = AnyRef

  // Сигнал, означающий для актора NewsQueue, что он остался в одиночестве и ему пора.
  val iAmLonelyMsg = 'iAmLonely
}


abstract class NewsQueueAbstract extends Actor with LogsAbstract {

  import NewsQueue._

  protected val noNews = List[NewsEventT]()

  // Очередь. Scala использует mutable list, поэтому функция определения длины имеет сложность O(1)
  protected [this] val news_queue = mutable.Queue[QueuedNews]()
  protected [this] val waiting = mutable.MutableList[WaitingActor]()

  // Через сколько времени одиночества, актор должен совершать самоуничтожение. По умолчанию - жить вечно.
  protected val stopLonelyDuration : Option[FiniteDuration] = None

  // Тут храним ссылку на таймер самовыпиливания. Если наступает какое-то событие, то таймер будет отменен.
  protected [this] var stopLonelyTimer : Option[Cancellable] = stopLonelyDuration map {
    scheduler.scheduleOnce(_, self, iAmLonelyMsg)
  }

  /**
   * Обработка входящих сообщений, в частности входящих событий и запросов на получение свежих новостей.
   * @return
   */
  def receive = {
    // Пришли новости. Закинуть их в очередь под текущим таймштампом.
    case AddNews(news) =>
      news_queue.enqueue(QueuedNews(getTimestampMs, news))

    // Клиент запрашивает все свежие новости на текущий момент. Вызов какбы неблокирующий
    // и всегда сразу возвращает результат
    case ShortPullNews(timestampMs) =>
      val freshNews = getNewsFresherThan(timestampMs)
      sender ! (getTimestampMs, freshNews)
      // TODO

    case _ => ???
  }


  protected def scheduler = context.system.scheduler
  protected def getTimestampMs = System.currentTimeMillis()

  /**
   * Получить список новостей, которые новее некоторого timestamp.
   * @param timestampMs
   */
  protected def getNewsFresherThan(timestampMs:Long) = {
    if (news_queue.isEmpty)
      noNews
    else
      getNewsFresherThanNoCheck(timestampMs)
  }

  protected def getNewsFresherThanNoCheck(timestampMs:Long) = {
    news_queue
      .dropWhile(_.timestampMs < timestampMs)
      .toList
  }


  // Внутренний класс, описывающий элемент очереди новостей.
  protected case class QueuedNews(timestampMs:Long, news:NewsEventT)
  // Элемент списка процессов, ждущих новостей
  protected case class WaitingActor(actorRef:ActorRef)
}

protected case class AddNews(news:NewsQueue.NewsEventT)
protected case class ShortPullNews(timestampMs:Long)
protected case class LongPollNews(timestampMs:Long, timeoutMs:Long)
