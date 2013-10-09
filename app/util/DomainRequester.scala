package util

import akka.actor.{Props, ActorRefFactory, ActorRef, Actor}
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
import play.api.Play.current

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.04.13 13:33
 * Description: Актор, используемый для задач последовательного соединения с доменами. Управляет фьючерсами на проверку
 * тех или иных задач. Использует dispatch 0.10+ для поддержания пула http-клиентов через стандартные фьючерсы.
 *
 * Суть: кто-то отправляет актору ссылку, и он смотрит нет ли уже выполняющегося фьючерса в карте. Если нет, то
 * добавить новый фьючерс, иначе скомбинировать через andThen и перезаписать в карте и вернуть его клиенту.
 *
 * Когда фьючерс завершится, актор получит уведомление и выкинет его из карты фьючерсов.
 * Если фьючерс был перезаписан, то удаления не произойдёт.
 */

// Клиент к актору DomainRequest. Содержит статическое барахло
object DomainRequester {

  // Тип, используемый для счетчика.
  type CounterT = Int

  /** Короткий враппер для получения int'а из конфига. */
  private def gi(key:String, default: => Int): Int = {
    current.configuration.getInt(key) getOrElse default
  }

  // Таймаут выполнения одного запроса.
  val httpRequestTimeoutMs = gi("domain.requester.http.request.timeout_ms", 15.seconds.toMillis.toInt)
  val idleConnectionTimeoutMs = gi("domain.requester.pool.idle.timeout_ms", 5.seconds.toMillis.toInt)
  val maxConnPerHost = gi("domain.requester.pool.max_per_host", 1)
  val maxPoolSize = gi("domain.requester.pool.max_size", 5)

  protected val timeoutSec = 5.seconds
  protected implicit val timeout = Timeout(timeoutSec)

  val ACTOR_NAME = "domainRequester"

  /**
   * Узнать actorRef для актора DomainRequester. Можно использовать actorFor(path), но тут по сути короче.
   * @return ActorRef
   */
  protected def actorRef : ActorRef = SiowebSup.getDomainRequesterRef

  /**
   * Отправить ссылку в псевдо-очередь.
   * @param dkey ключ домена, обычно нормализованный хостнейм.
   * @param url ссылка на страницу, которую опрашиваем.
   * @return Фьючерс DRResp200.
   */
  def queueUrl(dkey:String, url:String) : Future[DRResp200] = {
    (actorRef ? QueueUrl(dkey=dkey, url=url))
      // Можно заменить на asInstanceOf[Future[Future[...]] + flatten, но это затруднит читабельность кода.
      .flatMap { _.asInstanceOf[Future[DRResp200]] }
  }

  /**
   * Функция нужна для отладки: чтение копии текущей карты из работающего актора.
   * @return неизменяемая карта, копия текущей.
   */
  def getFMapCopy = call[Map[String, FutureReqTask]]('getFMapCopy)


  /**
   * Короткий враппер для вызова синхронного call. Это только для отладки.
   * @param msg сообщение актору.
   * @tparam T тип возвращаемого значения ответа, обязателен.
   * @return ответ актора типа T
   */
  protected def call[T](msg:Any) = Await.result(actorRef ? 'getFMapCopy, timeoutSec).asInstanceOf[T]


  def startLink(arf: ActorRefFactory): ActorRef = {
    arf.actorOf(Props[DomainRequester], name=ACTOR_NAME)
  }
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
      .setRequestTimeoutInMs(httpRequestTimeoutMs)
      .setFollowRedirects(false)
      .setMaximumConnectionsTotal(maxPoolSize)
      .setMaximumConnectionsPerHost(maxConnPerHost)
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
      val frt1: Future[DRResp200] = fmap.get(_dkey) match {
        // Какой-то запрос сейчас выполняется. Нужно скомбинировать этот фьючерс.
        case Some(frt) =>
          // Если фьючерс уже выполнен, то не комбинировать его, а выкинуть, создав новый.
          if (frt.future.isCompleted) {
            startReqAsync(_url)

          } else {
            // Нужно, чтобы независимо от результата выполняющегося фьючерса к нему прицепился следующий фьючерс.
            // Это можно сделать только через Promise + onComplete.
            val p = scala.concurrent.Promise[DRResp200]()
            frt.future onComplete { case _ =>
              p completeWith startReqAsync(_url)
            }
            p.future
          }

        // Никто не обращался к указанному домену последнее время. Нужно создать новый фьючерс.
        case None => startReqAsync(_url)
      }
      // Отправить будущий результат юзеру
      sender ! frt1
      // Когда фьючерс завершается, он должет уведомить о завершении в этот актор.
      frt1 onComplete {
        _ => self ! FutureCompleted(_dkey, counter)
      }
      // Обновить состояние актора
      fmap(_dkey) = FutureReqTask(counter=counter, future=frt1)
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


  val asCtAndStream = as.Response { r => DRResp200(r.getContentType, r.getResponseBodyAsStream) }


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
  future:  Future[DRResp200]
)
protected case class QueueUrl(dkey:String, url:String)
protected case class FutureCompleted(dkey:String, counter:DomainRequester.CounterT)
case class DRResp200(content_type:String, istream:InputStream)