package util.event

import play.api.libs.iteratee._
import play.api.libs.json.JsValue
import util.{Logs, NewsQueue4Play}
import io.suggest.event.SioNotifier.Classifier
import io.suggest.util.event.subscriber.SioEventTJSable
import scala.concurrent.Future
import util.acl.PersonWrapper.PwOpt_t
import java.util.UUID
import io.suggest.event.subscriber.SnActorRefSubscriber
import io.suggest.util.event.subscriber.SnWebsocketSubscriber
import play.api.libs.concurrent.Execution.Implicits._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.06.13 10:36
 * Description: Набор утили для событий: например, подписка на всякие события.
 */

object EventUtil extends Logs {

  type EventIO_t = (Iteratee[JsValue, Unit], Enumerator[JsValue])

  // Статическая раздача пустых каналов ввода-вывода.
  // TODO Возможно, это потом придется перенести в val, убедившись, что это безопасно.
  def dummyIn  = Iteratee.ignore[JsValue]
  def dummyOut = Enumerator[JsValue]()
  def dummyIO : EventIO_t = (dummyIn, dummyOut)

  /**
   * Раздача базовых каналов для вебсокетов юзера.
   * @param pw_opt acl-данные о юзере, обычно приходят неявно из экшенов.
   * @return In и Out каналы, пригодные для раздачи по websocket и комбинированию в контроллерах под конкретные задачи.
   */
  def globalUserEventIO(implicit pw_opt: PwOpt_t) : EventIO_t = {
    // TODO нужно для владельцев сайтов подцеплять события и важные уведомления какие-то для обратной связи.
    dummyIO
  }

  /**
   * Кто-то долбится на веб-сокет в обход логики работы системы и упирается в права доступа.
   * Надо написать в веб-сокет, что это слишком круто для нашей системы.
   * @param msg Сообщение, отсылаемое клиенту или дефолтовое.
   * @return (in, out)
   */
  def wsAccessImpossbleIO(msg: String = "Forbidden"): EventIO_t = {
    val in = Done[JsValue, Unit]((), Input.EOF)
    val jsMsg = AccessErrorEvent(msg).toJson
    val out = Enumerator[JsValue](jsMsg)
      .andThen(Enumerator.enumInput(Input.EOF))
    (in, out)
  }

  /**
   * Функция быстрой замены очереди на канал вебсокета с перекачкой новостей.
   * Комбинация из очереди + канала ws используется для сбора и передачи событий, который поступаю асинхронно по отношению
   * к запросам юзерам и которые могут начать поступать ещё до реального открытия канала ws.
   * @param classifier Классификатор событий, на которые подписана очередь и будет подписан WS-канал.
   * @param uuid идентификатор ws-события. Потом событие с таким же идентификатором подцепляется на in.EOF для unsubscribe.
   * @param nqDkey Ключ очереди новостей. Обычно это dkey или иной id сущности.
   * @param nqTyp Подтип очереди новостей. Используется для разделения очередей в рамках id сущности.
   * @param channel Выходной канал вебсокета.
   * @param nqIsMandatory Считать ли наличие очереди обязательным? Пока это влияет только на логи.
   * @param timestampMs Перекачивать из очереди новостей только новости новее указанного времени.
   * @param logPrefix Использовать указанный префикс для log-записей. Обычно функция работает без лог-записей, поэтому этот параметр "ленивый".
   */
  def replaceNqWithWsChannel(classifier:Classifier, uuid:UUID, nqDkey:String, nqTyp:String, channel:Concurrent.Channel[JsValue], nqIsMandatory:Boolean = false, timestampMs:Long = -1L, logPrefix: => String = "???")(implicit pw_opt:PwOpt_t = None) {
    // TODO вероятно, эту функцию нужно разбить на две.
    NewsQueue4Play.getActorFor(nqDkey, nqTyp).foreach { nqActorRefOpt =>
      logger.debug(logPrefix + "NQ supervisor answered: queue(%s %s) => %s" format(nqDkey, nqTyp, nqActorRefOpt))
      val subscriberWs = new SnWebsocketSubscriber(uuid=uuid, channel=channel)
      val snActionFuture: Future[Boolean] = nqActorRefOpt match {
        // Как и ожидалось, у супервизора уже есть очередь с новостями. Нужно заменить её в SioNotifier на прямой канал SN -> WS.
        case Some(nqActorRef) =>
          logger.debug(logPrefix + "SN atomic replace: NewsQueue %s -> %s; user=%s" format(nqActorRef, subscriberWs, pw_opt))
          SiowebNotifier.replaceSubscriberSync(
            subscriberOld = SnActorRefSubscriber(nqActorRef),
            classifier    = classifier,
            subscriberNew = subscriberWs
          ) andThen { case _ =>
            // Затем нужно перекачать накопленные новости в открытый канал.
            NewsQueue4Play.shortPull(nqActorRef, timestampMs).foreach { newsReply =>
              val news = newsReply.news
              val newsFailed = news.foldLeft(List[NewsQueue4Play.NewsEventT]()) { (accWrong, n) =>
                n match {
                  case n:SioEventTJSable =>
                    channel.push(n.toJson)
                    accWrong

                  case other => other :: accWrong
                }
              }
              // Если были недопустимые новости, то нужен варнинг в логах.
              if(!newsFailed.isEmpty)
                logger.warn(logPrefix + "forwarded only %s news of total %s. Failed to forward: %s" format(news.size - newsFailed.size, news.size, newsFailed))
              else
                logger.debug(logPrefix + "All %s news forwarded from NewsQueue@%s into ws channel".format(news.size, nqActorRef))
            }
            logger.debug(logPrefix + "Async.stopping NewsQueue %s ..." format nqActorRef)
            NewsQueue4Play.stop(nqActorRef)
          }

        // Внезапно очереди нет. Это плохо, и это скорее всего приведет к ошибке, если валидация сайта уже прошла до текущего момента,
        // и значит уведомление об успехе было отправлено в /dev/null. Нужно предложить юзеру обновить страницу.
        // TODO Может нужно обновить страницу сразу? Или отобразить кнопку релоада юзеру?
        case None =>
          if(nqIsMandatory) {
            logger.error(logPrefix + "NewsQueue doesn't exist, but it should. Possible incorrect behaviour for user %s." format pw_opt)
            channel.push(MaybeErrorEvent("It looks like, something went wrong during installation procedure. If errors or problems occurs, please reload the page.").toJson)
          }
          SiowebNotifier.subscribeSync(
            subscriber = subscriberWs,
            classifier = classifier
          )
      }
      // перехват возможных внутренних ошибок
      snActionFuture onFailure { case ex:Throwable =>
        logger.error(logPrefix + "Internal error during NQ -> WS swithing.", ex)
        channel.push(InternalServerErrorEvent("Suggest.io has detected internal error.").toJson)
      }
    }
  }


  /**
   * Функция для отписывания от канала ws от новостей по закрытия.
   * @param in Входной Iteratee.
   * @param uuid Идентификатор WS-подписуемого
   * @param classifier Классификатор события.
   * @return Новый Iteratee на базе исходного. Новый автоматически вызовет unsubscribe, когда канал начнет закрываться.
   */
  def inIterateeSnUnsubscribeWsOnEOF[A, B](in:Iteratee[A, B], uuid:UUID, classifier:Classifier): Iteratee[A, B] = {
    // Раньше было .mapDone(), теперь просто .map().
    in.map { x =>
      SiowebNotifier.unsubscribe(
        subscriber = new SnWebsocketSubscriber(uuid=uuid, channel = null),
        classifier = classifier
      )
      x
    }
  }

}
