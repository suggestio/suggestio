package util

import play.api.mvc.Session
import play.api.libs.concurrent.Execution.Implicits._
import org.apache.tika.metadata.{HttpHeaders, TikaMetadataKeys, Metadata}
import java.io.InputStream
import java.util.concurrent._
import io.suggest.sax._
import org.apache.tika.parser.AutoDetectParser
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import models.MDomainQi
import org.xml.sax.Attributes

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 16:45
 * Description: Domain Quick Install - система быстрого подключения доменов к suggest.io. Юзер добавляет домен,
 * ему выдается js-код с ключиком, этот же ключик прописывается в сессии. Затем, при запросе скрипта от имени этого
 * домена происходит проверка наличия на сайте нужного скрипта.
 *
 * Суть этого модуля - дергать DomainRequester и прикручивать асинхронный анализатор результата к фьючерсу.
 * Анализатор должен парсить htmk и искать на странице тег скрипта suggest.io и определять из него домен и qi_id,
 * затем проверять по базе и порождать какое-то событие или предпринимать иные действия.
 */


// Статический клиент к DomainRequester, который выполняет запросы к доменам
object DomainQi extends Logs {

  val parseTimeout = 5.seconds
  protected val timeout_msg = "timeout"

  /**
   * Прочитать из сессии список быстро добавленных в систему доменов и прилинковать их к текущему юзеру.
   * Домен появляется в сессии юзера, когда qi-проверялка реквестует страницу со скриптом и проверит все данные.
   * @param email email юзера, т.е. его id
   * @param session Неизменяемые данные сессии. Все данные сессии будут безвозвратно утрачены после завершения этого метода.
   */
  def installFromSession(email:String, session:Session) {
    println("installFromSession(): Not yet implemented")
  }


  /**
   * Быстро создать фьючерс и скомбинировать его с парсером html и анализатором всея добра.
   * Для комбинирования используются callback'и, ибо новые комбинированные фьючерсы тут никому не нужны.
   * @param dkey ключ домена
   * @param url ссылка. Обычно на гланге
   * @param qi_id заявленный юзером qi_id, если есть. Может и не быть, если в момент установки на сайт зашел кто-то
   *              другой без qi_id в сессии.
   */
  def checkQiAsync(dkey:String, url:String, qi_id:Option[String]) {
    // Запросить постановку в очередь указанной ссылки для указанного домена
    DomainRequester.queueUrl(dkey, url) onSuccess { case DRResp200(ct, istream) =>
      // 200 OK: запустить тику с единственным SAX-handler и определить наличие скрипта на странице.
      try {
        // Формируем набор метаданных
        val md = new Metadata
        md.add(TikaMetadataKeys.RESOURCE_NAME_KEY, url)
        md.add(HttpHeaders.CONTENT_TYPE, ct)
        // Запускаем через жабовскую FutureTask, ибо в scala нормального прерывания фьючерса по таймауту нет.
        val c = new DomainQiTikaCallable(md, istream)
        val task = new FutureTask(c)
        val t = new Thread(task)
        t.start()
        val result : Either[String, SioJsV2] = try {
          val l = task.get(parseTimeout.toMillis, TimeUnit.MILLISECONDS)
          // Есть список найденных скриптов suggest_io.js на странице. Определить, есть ли среди них подходящий.
          l.find {
            case SioJsV2(_dkey, _qi_id) if dkey == _dkey && qi_id.isDefined && qi_id.get == _qi_id => true
            case other => false
          } match {
            case Some(info) =>
              val info2 = info.asInstanceOf[SioJsV2]
              logger.info("qi success for " + dkey + "! " + info2)
              Right(info2)

            case None =>
              // Послать уведомление о неудачной проверке. Если в списке есть элементы, то они "чужие", а если нет то что-то не так установлено.
              val errMsg = if (l.isEmpty)
                "No suggest.io JS found on this page. Not installed?"
              else
                "Expected suggest.io.js key NOT found, but found other suggest.io scripts on the page. Not yours?"
              Left(errMsg)
          }

          // Произошла какая-то ошибка во внутреннем try, надо бы уведомить юзера об этом
        } catch {
          case te:TimeoutException =>
            task.cancel(true)
            //t.interrupt()   // cancel(true) сам вызывает t.interrupt()
            Left("Cannot parse page " + url + " for qi: parsing timeout")

          // Внутри callable вылетел экзепшен. Он обернут в ExecutionException
          case ex:ExecutionException =>
            val errMsg = "Cannot parse page: internal parse error."
            logger.error("parse exception on %s".format(url) + ex.getCause)
            Left(errMsg)
        }
        val qiNews : QiEventT = result match {
          // Всё верно. Можно заапрувить учетку юзера по отношению к этому домену.
          case Right(_) if qi_id.isDefined =>
            approve_qi(dkey, qi_id.get)
            QiSuccess(url)

          // Ниасилил. Надо чиркануть в логи и вернуть в новости ошибку qi.
          case Left(errMsg) =>
            logger.warn("qi check failed on %s: %s".format(url, errMsg))
            QiError(url, errMsg)

          // Скорее всего, тот недостижимый код, но всё же перестраховываемся.
          case other =>
            logger.error("Unexpected results from qi checker: %s while qi_id opt = %s".format(other, qi_id))
            QiError(url, "Internal suggest.io error. So sorry...")
        }
        NewsQueue4Play.pushTo(dkey, "qi", qiNews)

      } finally {
        istream.close()
      }
    }
  }


  /**
   * Зааппрувить указанный qi id.
   * @param dkey
   * @param qi_id
   */
  def approve_qi(dkey:String, qi_id:String) {
    MDomainQi.getForDkeyId(dkey, qi_id) match {
      // Есть в базе запись об открытом qi. Нужно удалить этот qi и создать соответствующий PersonDomainAuthz
      case Some(qi) =>
        ???

      case None =>
        logger.error("approve_qi(): QI '%s' unexpectedly not found for domain '%s'".format(qi_id, dkey))
    }
  }

}


// Анализ вынесен в отдельный поток для возможности слежения за его выполнением и принудительной остановкой по таймауту.
class DomainQiTikaCallable(md:Metadata, input:InputStream) extends Callable[List[SioJsInfoT]] {

  /**
   * Запуск tika для парсинга запроса
   * @return
   */
  def call(): List[SioJsInfoT] = {
    val jsInstalledHandler = new SioJsDetectorSAX
    val parser = new AutoDetectParser()
    parser.parse(input, jsInstalledHandler, md)
    jsInstalledHandler.getSioJsInfo
  }

}


trait QiEventT {
  val url: String
  val msg: String
}
case class QiError(url:String, msg:String) extends QiEventT
case class QiSuccess(url:String) extends QiEventT {
  val msg : String = "OK"
}


// Т.к. джава не умеет останавливать потоки, а только выставлять отметки interrupted, тут надо проверять сие.
// ХЗ, нужен ли этот код вообще -- наверное лучше проверять длину ответа от сервера.
class SioJsDetectorInterruptableSAX extends SioJsDetectorSAX {

  // Счетчики тегов. Каждые N тегов проверять флаг текущего потока на предмет наличия прерывания.
  protected var ie = 0
  val checkIntEvery = 10

  override def startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
    ie = ie + 1
    if (ie > checkIntEvery) {
      if (Thread.interrupted())
        throw new InterruptedException(getClass.getSimpleName + ": thread interrupted")

      ie = 0
    }
    super.startElement(uri, localName, qName, attributes)
  }

}