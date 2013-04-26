package util

import akka.actor.{ActorRef, Actor}
import scala.concurrent.Future
import dispatch._
import com.ning.http.client.{AsyncHttpClient, AsyncHttpClientConfig}
import scala.concurrent.duration._
import scala.collection.mutable
import concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import play.api.libs.concurrent.Execution.Implicits._
import java.io.InputStream

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.04.13 13:33
 * Description: Актор, используемый для задач последовательного соединения с доменами. Управляет фьючерсами на проверку
 * тех или иных задач. Использует dispatch 0.10+ для поддержания пула http-клиентов через стандартные фьючерсы.
 *
 * Суть: кто-то отправляет актору ссылку, и он смотрит нет ли уже выполняющегося фьючерса в карте. Если нет, то
 * добавить новый фьючерс, иначе скомбинировать через с andThen и перезаписать в карте и вернуть его клиенту.
 *
 * Когда фьючерс завершится, актор получит уведомление и выкинет его из карты фьючерсов.
 * Если фьючерс был перезаписан, то удаления не произойдёт.
 */

// Клиент к актору DomainRequest. Содержит статическое барахло
object DomainRequester {

  // Тип, используемый для счетчика.
  type CounterT = Int

  // Таймаут выполнения одного запроса.
  val httpFetchTimeoutMs = 15.seconds.toMillis.toInt
  val idleConnectionTimeoutMs = 5.seconds.toMillis.toInt

  protected val timeoutSec = 5.seconds
  protected implicit val timeout = Timeout(timeoutSec)

  /**
   * Узнать actorRef для актора DomainRequester. Можно использовать actorFor(path), но тут по сути короче.
   * @return ActorRef
   */
  protected def actorRef : ActorRef = SiowebSup.getDomainRequesterRef

  /**
   * Отправить ссылку в псевдо-очередь. Вернуть фьючерс запроса, который начнет выполнятся как только придёт время.
   * @param dkey
   * @param url
   */
  def queueUrlSync(dkey:String, url:String) = Await.result(queueUrl(dkey,url), timeoutSec)

  def queueUrl(dkey:String, url:String) = (actorRef ? QueueUrl(dkey=dkey, url=url)).asInstanceOf[Future[Future[DRResp]]]

  /**
   * Функция нужна для отладки: чтение копии текущей карты из работающего актора.
   * @return неизменяемая карта, копия текущей.
   */
  def getFMapCopy = call[Map[String, FutureReqTask]]('getFMapCopy)


  /**
   * Короткий враппер для вызова call.
   * @param msg сообщение актору.
   * @tparam T тип возвращаемого значения ответа, обязателен.
   * @return ответ актора типа T
   */
  protected def call[T](msg:Any) = Await.result(actorRef ? 'getFMapCopy, timeoutSec).asInstanceOf[T]

}


import DomainRequester._
class DomainRequester extends Actor {


  var counter : CounterT = Int.MinValue

  // Карта фьючерсов. Ключ - домен, значение - это фьючерс и счетчик. Счетчик нужен, чтобы комбинированный фьючерс
  // не удалил дочернюю комбинацию.
  protected val fmap : mutable.Map[String, FutureReqTask] = mutable.Map()

  // Пул http-рабочих, поддерживающих коннекшен до хоста некоторое время.
  protected val httpc = new Http {
    val builder = new AsyncHttpClientConfig.Builder()
      .setCompressionEnabled(false) // чтобы ускорить процесс и не напрягаться с декомпрессией
      .setAllowPoolingConnection(true)
      .setRequestTimeoutInMs(httpFetchTimeoutMs)
      .setFollowRedirects(false)
      .setMaximumConnectionsTotal(5)
      .setMaximumConnectionsPerHost(1)
      .setIdleConnectionInPoolTimeoutInMs(idleConnectionTimeoutMs)

    override val client = new AsyncHttpClient(builder.build())
  }


  /**
   * Обработка входящих сообщений.
   * @return
   */
  def receive = {

    // Запилить URL в очередь, вернув фьючерс клиенту и сохранив в память.
    case QueueUrl(_dkey, _url) =>
      val fut = fmap.get(_dkey) match {
        // Какой-то запрос сейчас выполняется. Нужно скомбинировать этот фьючерс.
        case Some(frt) =>
          frt.future andThen { case _ => startReqAsync(_url) }

        // Никто не обращался к указанному домену последнее время. Нужно создать новый фьючерс.
        case None => startReqAsync(_url)
      }
      // Скомбинировать фьючерс, чтоб он слал уведомление о завершении в этот актор.
      val fut1 = fut andThen {
        case _ => self ! FutureCompleted(_dkey, counter)
      }
      // Отправить результат юзеру
      sender ! fut1
      // Обновить состояние актора
      fmap(_dkey) = FutureReqTask(counter=counter, future=fut1)
      counter = counter + 1


    // Фьючерс завершился. Возможно, этот фьючерс уже был перезаписан в карте фьючерсов. Но если это не так, то удалить.
    case FutureCompleted(dkey, counterFut) =>
      fmap.get(dkey).foreach { frt =>
        if (frt.counter == counterFut)
          fmap.remove(dkey)
      }


    // В отладочных целях можно дергать этот метод для чтения текущей карты из актора
    case 'getFMapCopy =>
      sender ! fmap.toMap
  }


  override def postStop() {
    super.postStop()
    httpc.shutdown()
  }


  val asCtAndStream = as.Response { r => DRResp(r.getContentType, r.getResponseBodyAsStream) }


  /**
   * Запустить реквест
   * @param _url
   * @return
   */
  protected def startReqAsync(_url:String) = {
    val req = url(_url)
    httpc(req OK asCtAndStream)
  }

}



protected case class FutureReqTask(
  counter: CounterT,
  future:  Future[DRResp]
)
protected case class QueueUrl(dkey:String, url:String)
protected case class FutureCompleted(dkey:String, counter:DomainRequester.CounterT)
case class DRResp(content_type:String, istream:InputStream)