package io.suggest.event

import akka.actor._
import scala.collection.mutable
import scala.concurrent.duration._
import io.suggest.util.LogsAbstract
import akka.actor.Terminated
import scala.concurrent.{Future, ExecutionContext}

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

object NewsQueue extends NewsQueueStaticT {

  // Сигнал, означающий для актора NewsQueue, что он остался в одиночестве и ему пора.
  val iAmLonelyMsg = 'iAmLonely
  val dropOldMsg = 'drop_old

  // По таймеру дропать последнюю новость и последующие новости, если они устаревают в течение следующих N мс.
  val checkNextNewsMs = 50
}

trait NewsQueueStaticT {

  import akka.pattern.ask
  import akka.util.Timeout

  // Какой тип данных является новостью
  type NewsEventT = AnyRef

  val pingMsg = 'ping

  def getTimestampMs = System.currentTimeMillis()

  // Далее идут функии для взаимодействия с актором:

  /**
   * Отправить новость в очередь новостей указанного актора NewsQueue.
   * @param nq актор
   * @param news новость любого типа.
   */
  def push(nq:ActorRef, news:NewsEventT) {
    nq ! PushNews(news)
  }

  /**
   * Запрос имеющихся новостей.
   * @param nq актор очереди
   * @param timestampMs таймштамп
   * @return Фьючерс, который вот-вот уже выполнится.
   */
  def shortPull(nq:ActorRef, timestampMs:Long) = {
    implicit val timeout = Timeout(5.seconds)
    (nq ? ShortPullNews(timestampMs)).asInstanceOf[Future[NewsReply]]
  }


  /**
   * "Длинный" запрос новостей. Фьючерс заполняется данными за время, не превышеающее указанный таймаут.
   * Если сервер очереди не отвечает, то будет соответствующий экзепшен.
   * @param nq актор очереди
   * @param timestampMs таймштамп для выборки свежих новостей
   * @param timeoutMs таймаут ожидания свежих новостей, если очередь пуста.
   * @return Фьючерс с новостями.
   */
  def longPoll(nq:ActorRef, timestampMs:Long, timeoutMs:FiniteDuration) = {
    implicit val timeout = Timeout(timeoutMs + 100.millis)
    (nq ? LongPollNews(timestampMs, timeoutMs)).asInstanceOf[Future[NewsReply]]
  }


  /**
   * Отправить ping актору.
   * @param nq
   * @return
   */
  def ping(nq:ActorRef) = {
    implicit val timeout = Timeout(200.millis)
    (nq ? pingMsg).asInstanceOf[Future[Boolean]]
  }

}


// Абстрактная реализация сервера очереди. Использует LogsAbstract для логгирования: на стороне play будет костыль для
// play.Logger, а в кравлере - slf4j.Logger, т.е. sio.util.Logs.
abstract class NewsQueueAbstract(
  stopLonelyDuration : Option[FiniteDuration] = None,      // Через сколько времени "одиночества", актор должен совершать самоуничтожение. По умолчанию - жить вечно.
  newsTtl : FiniteDuration = 30.seconds                    // Новость автоматически удаляется из очереди после этого временного промежутка.
)(implicit ex:ExecutionContext) extends Actor with LogsAbstract {

  import NewsQueue._

  // Очередь. Scala использует mutable list, поэтому функция определения длины имеет сложность O(1)
  protected [this] val news_queue = mutable.Queue[QueuedNews]()
  protected [this] var waiting = mutable.MutableList[WaitingActor]()

  // Тут храним ссылку на таймер самовыпиливания. Если наступает какое-то событие, то таймер будет отменен.
  protected [this] var lonelyTrefOpt : Option[Cancellable] = None

  // Таймер удаления старых новостей из очереди. Пуст когда пуста очередь.
  protected [this] var dropOldNewsTrefOpt : Option[Cancellable] = None


  /**
   * Запуск актора. Нужно запустить таймер самоуничтожения, ибо актор всегда запускается одиноким.
   */
  override def preStart() {
    super.preStart()
    ensureLonelyTimer()
  }


  /**
   * Если пришли новости, то тут реакция. Вынесено за пределы receive, ибо новости по историческим причинам могут приходить по разным каналам.
   * @param news новости.
   */
  protected def handleNews(news : NewsEventT) {
    val queueWasEmpty = news_queue.isEmpty
    news_queue.enqueue(QueuedNews(getTimestampMs, news))
    // Уведомить всех ожидающих о появлении свежей новости
    if (!waiting.isEmpty) {
      val newsReply = NewsReply(timestampMs = getTimestampMs, news = List(news))
      waiting.foreach {
        case WaitingActor(waitingActorRef, timeoutTref) =>
          timeoutTref.cancel()
          waitingActorRef ! newsReply
          context.unwatch(waitingActorRef)
      }
      waiting.clear()
    }
    // Т.к. ожидающих сейчас точно нет, надо убедится, что таймер самовыпиливания запущен
    ensureLonelyTimer()
    // Если очередь новостей БЫЛА пуста, то надо запустить таймер выпиливания старых новостей.
    if (queueWasEmpty)
      startDropOldNewsTimer(newsTtl)
  }


  /**
   * Обработка входящих сообщений, в частности входящих событий и запросов на получение свежих новостей.
   * Подклассы могут расширять функцию через super.receive orElse otherPartialFunction
   * @return
   */
  def receive : PartialFunction[Any, Unit] = {

    // Пинг используется чтобы сообщить, что актор ещё кому-то интересен.
    case atom if atom == pingMsg =>
      sender ! true
      maybeRestartLonelyTimer()


    // Пришли новости. Закинуть их в очередь под текущим таймштампом.
    case PushNews(news)  => handleNews(news)


    // Очередь присоединена напрямую к SioNotifier, и он пробрасывает новости.
    case news: SioNotifier.Event => handleNews(news)


    // Cработал таймер очистки очереди новостей. Пора удалять старые новости. Обычно удаляется один самый старый элемент.
    case atom if atom == dropOldMsg =>
      news_queue.dequeue()

      // Чтобы не запускать короткоживущие таймеры, дропаем последующие новости, если им тоже уже пора
      val currentTstampMs = getTimestampMs
      def dropNearOld() {
        news_queue.headOption.foreach { qn =>
          if (qn.timestampMs - checkNextNewsMs < currentTstampMs) {
            news_queue.dequeue()
            dropNearOld()
          }
        }
      }
      dropNearOld()
      // И запустить новый таймер чистки новостей, если очередь новостей не пуста.
      if (news_queue.isEmpty) {
        dropOldNewsTrefOpt = None

      } else {
        // Ещё есть новости в очереди. Нужно запустить таймер для следующей новости
        val tstampDiffMs = news_queue.head.timestampMs - currentTstampMs
        startDropOldNewsTimer(tstampDiffMs.milliseconds)
      }


    // Клиент запрашивает все свежие новости на текущий момент. Вызов какбы неблокирующий и всегда сразу возвращает результат.
    case ShortPullNews(timestampMs) =>
      stopLonelyTimer()

      val freshNews = getNewsFresherThan(timestampMs)
      sender ! NewsReply(timestampMs = getTimestampMs, news = freshNews)
      // Сразу запустить таймер одиночества
      ensureLonelyTimerIfNoWaiting()


    // Клиент запрашивает новости, и если их нет, то не отвечать до их появления или до прихода unwait-сообщения.
    case LongPollNews(timestampMs, timeout) =>
      stopLonelyTimer()

      val fresherNews = getNewsFresherThan(timestampMs)
      if (fresherNews == Nil) {
        // Нет [свежих] новостей. Отправить клиента в список waiting
        context.watch(sender)
        val timeoutTref = scheduler.scheduleOnce(timeout, self, WaitingTimeout(sender))
        waiting += WaitingActor(sender, timeoutTref)

      } else {
        // Есть новости - вернуть их ожидающему
        sender ! NewsReply(timestampMs = getTimestampMs, news = fresherNews)
        ensureLonelyTimerIfNoWaiting()
      }


    // Наступил таймаут ожидания актором ответа этого сервера новостей. Нужно выкинуть актора из waiting и вернуть пустой
    // ответ ожидающему, если он присутствовал в waiting.
    case WaitingTimeout(waitingActorRef) =>
      val len0 = waiting.size
      rmWaiting(waitingActorRef)
      // Если из waiting был отфильтрован хоть один элемент, надо отправить ответ актору, у которого произошел таймаут.
      if (waiting.size < len0)
        waitingActorRef ! NewsReply(getTimestampMs, noNews)


    // Terminated -- это такой аварийный unwait, если процесс сдох, не дождавшись результата.
    // Не должен наступать часто, а лишь в исключительных случаях.
    case Terminated(actorRef) =>
      error("News-client terminated during wait: " + actorRef)
      rmWaiting(actorRef)
      ensureLonelyTimerIfNoWaiting()

    // Таймер автозавершения сработал, т.е. наступило состояние полного одиночества.
    case atom if atom == iAmLonelyMsg =>
      context.stop(self)
  }



  /**
   * Остановка актора. Нужно остановить таймер самовыпиливания и разослать всем ожидающим сообщение о том, чтоб не ждали.
   */
  override def postStop() {
    super.postStop()
    stopLonelyTimer()
    if (!waiting.isEmpty) {
      val noNewsMsg = NewsReply(getTimestampMs, noNews)
      waiting.foreach(_.actorRef ! noNewsMsg)
    }
  }


  /**
   * Строка, характеризующая инстанс.
   * @return String
   */
  override def toString: String = {
    getClass.getSimpleName +
      "(stopLonely=" + stopLonelyDuration +
      ", newsTtl=" + newsTtl +
      ", queueLen=" + news_queue.size +
      ", waitingCount=" + waiting.size +
      ")"
  }


  /* ********************************************************************************
   * Internal functions
   * ********************************************************************************/

  protected def scheduler = context.system.scheduler

  /**
   * Получить список новостей, которые новее некоторого timestamp.
   * @param timestampMs таймштам, который разделяет новости на старые и свежие.
   */
  protected def getNewsFresherThan(timestampMs:Long) : List[NewsEventT] = {
    if (news_queue.isEmpty)
      noNews
    else
      getNewsFresherThanNoCheck(timestampMs)
  }

  protected def getNewsFresherThanNoCheck(timestampMs:Long) = {
    news_queue
      .dropWhile(_.timestampMs < timestampMs)
      .toList
      .map(_.news)
  }


  /* **********************************************************************
   * Управления таймером самозавершения в случае наступления одиночества.
   * **********************************************************************/
  /**
   * Убедится, что таймер самовыпиливания запущен.
   */
  protected def ensureLonelyTimer() {
    if (lonelyTrefOpt.isEmpty)
      startLonelyTimer()
  }

  /**
   * Запустить таймер самовыпиливания.
   * Функция НЕ проверяет запущенность предыдущего таймер и просто перезаписывает значение stopLonelyTimer.
   */
  protected def startLonelyTimer() {
    lonelyTrefOpt = getLonelyTimerRefOpt
  }

  /**
   * Запустить таймер автозавершения и вернуть указатель (опционально).
   */
  protected def getLonelyTimerRefOpt = {
    stopLonelyDuration map {
      scheduler.scheduleOnce(_, self, iAmLonelyMsg)
    }
  }

  /**
   * Остановить таймер одиночества.
   */
  protected def stopLonelyTimer() {
    lonelyTrefOpt.foreach { _tref =>
      _tref.cancel()
      lonelyTrefOpt = None
    }
  }

  protected def ensureLonelyTimerIfNoWaiting() {
    if (waiting.isEmpty)
      ensureLonelyTimer()
  }


  /**
   * Запустить/перезапустить таймер. Аналог ensure, но если таймер уже запущен, то он будет остановлен и снова запущен.
   */
  protected def restartLonelyTimer() {
    stopLonelyTimer()
    startLonelyTimer()
  }

  /**
   * Перезапустить таймер, если он запущен. Если таймера нет, то не запускать новый.
   */
  protected def maybeRestartLonelyTimer() {
    if (lonelyTrefOpt.isDefined)
      restartLonelyTimer()
  }


  def rmWaiting(actorRef:ActorRef) {
    waiting = waiting.filterNot(_.actorRef != actorRef)
  }


  // Чтобы не порождать одинаковые элементы, тут готовый пустой список новостей. По сути, хранит в себе указатель на объект Nil.
  protected def noNews : List[NewsEventT] = Nil


  /* ******************************************************************************
   * Таймер очистки очереди от старых новостей. Первый раз запускается на push при пустой очереди,
   * затем последовательно дозапускается для последующих новостей.
   * ******************************************************************************/

  protected def startDropOldNewsTimer(dropAfter:FiniteDuration) {
    val tref = scheduler.scheduleOnce(dropAfter, self, dropOldMsg)
    dropOldNewsTrefOpt = Some(tref)
  }


  /* *************************************************************************
   * Внутренние классы для внутреннего обмена сообщениями
   * *************************************************************************/

  // Внутренний класс, описывающий элемент очереди новостей.
  protected case class QueuedNews(timestampMs:Long, news:NewsEventT)
  // Элемент списка процессов, ждущих новостей
  protected case class WaitingActor(actorRef:ActorRef, timeoutTref:Cancellable)
  protected case class WaitingTimeout(waitingActorRef:ActorRef)
}


sealed case class PushNews(news:NewsQueue.NewsEventT)
sealed case class ShortPullNews(timestampMs:Long)
sealed case class LongPollNews(timestampMs:Long, timeout:FiniteDuration)
sealed case class NewsReply(timestampMs:Long, news:List[NewsQueue.NewsEventT])

