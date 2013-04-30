package util

import play.api.mvc.Session
import play.api.libs.concurrent.Execution.Implicits._
import org.apache.tika.metadata.{HttpHeaders, TikaMetadataKeys, Metadata}
import java.io.InputStream
import java.util.concurrent._
import scala.concurrent.{Future, future}
import io.suggest.sax._
import org.apache.tika.parser.{AutoDetectParser, ParseContext}
import scala.concurrent.duration._
import play.api.libs.concurrent.Promise.timeout
import java.util.concurrent.TimeUnit

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
   * Домен появляется в сессии юзера только, когда qi-проверялка реквестует страницу со скриптом и проверит все данные.
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
  def asyncCheckQi(dkey:String, url:String, qi_id:Option[String]) {
    // Запросить постановку в очередь указанной ссылки для указанного домена
    DomainRequester.queueUrl(dkey, url)
      // DomainRequest скоро запишет объект в выданный фьючерс. Внутри будет другой фьючерс - ответа хттп-клиента
      .onSuccess { case respFut =>
        // 200 OK: запустить тику с единственным SAX-handler и определить наличие скрипта на странице.
        respFut.onSuccess { case DRResp200(ct, istream) =>
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
            try {
              val l = task.get(parseTimeout.toMillis, TimeUnit.MILLISECONDS)
              // Есть список найденных скриптов suggest_io.js на странице. Определить, есть ли среди них подходящий.
              l.find {
                case SioJsV2(_dkey, _qi_id) if dkey == _dkey && qi_id.isDefined && qi_id.get == _qi_id => true
                case other => false
              } match {
                case Some(info) =>
                  val info2 = info.asInstanceOf[SioJsV2]
                  // TODO нужно заапрувить учетку юзера
                  logger.info("qi success!! " + info2)

                case None =>
                  // TODO нужно послать уведомление о неудачной проверке. Если в списке есть элементы, то они "чужие", а если нет то что-то не так установлено.
                  logger.error("qi install failed: found " + l)
              }

            // Произошла какая-то ошибка во внутреннем try, надо бы уведомить юзера об этом
            } catch {
              case te:TimeoutException =>
                task.cancel(true)
                t.interrupt()
                // TODO послать сообщение о проблеме
                logger.error("Cannot parse page " + url + " for qi: parsing timeout")

              // Внутри callable вылетел экзепшен. Он обернут в ExecutionException
              case ex:ExecutionException =>
                // TODO Нужно сообщить юзеру об ошибке
                logger.error("Cannot parse " + url, ex.getCause)
            }

          } finally {
            istream.close()
          }
      }
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