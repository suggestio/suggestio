package util

import play.api.mvc.Session
import play.api.libs.concurrent.Execution.Implicits._
import org.apache.tika.metadata.{HttpHeaders, TikaMetadataKeys, Metadata}
import java.io.InputStream
import java.util.concurrent.{FutureTask, Callable, ExecutionException, TimeoutException, TimeUnit}
import io.suggest.sax._
import org.apache.tika.parser.AutoDetectParser
import scala.concurrent.duration._
import models.MDomainQi
import org.xml.sax.Attributes
import io.suggest.util.StringUtil.randomId
import io.suggest.util.event.subscriber.SioEventTJSable
import play.api.libs.json.{JsBoolean, JsString, JsValue}
import io.suggest.event.SioNotifier
import java.net.{MalformedURLException, URL}
import io.suggest.util.UrlUtil
import scala.concurrent.Future

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
  val qiIdLen = 8

  /**
   * Отправить в сессию данные по добавлению сайта.
   * @param dkey ключ домена.
   * @return qi_id для генерации скрипта и обновлённый объект session либо без него если session не изменялся.
   */
  def addDomainQiIntoSession(dkey:String)(implicit session:Session) : (String, Option[Session]) = {
    // Храним домен в сессии, используя его в качестве ключа внутри криптоконтейнера.
    val skey = dkey2skey(dkey)
    session.get(skey) match {
      case Some(qi_id) =>
        qi_id -> None

      case None =>
        val qi_id = randomId(qiIdLen)
        val session1 = session + (skey -> qi_id)
        qi_id -> Some(session1)
    }
  }


  /**
   * Проверить qi по сессии и вынести вердикт по текущему ходу qi и домену, к которому оно относится.
   * Короче, функция отвечает на вопрос, происходит ли сейчас процедура qi и с какими точными данными или же нет.
   * Если юзер попытался что-то подменить, то функция не сработает.
   * @param dkey запрошенный юзером ключ домена
   * @param qi_id id qi, лежащий в сессии
   * @return Если всё совпадает, то true.
   */
  def isQi(dkey:String, qi_id:String)(implicit session:Session) : Boolean = {
    session.get(dkey2skey(dkey)) match {
      case Some(qi_id_s) => qi_id_s == qi_id
      case None => false
    }
  }


  /**
   * Прочитать из сессии список быстро добавленных в систему доменов и прилинковать их к текущему юзеру.
   * Домен появляется в сессии юзера, когда qi-проверялка реквестует страницу со скриптом и проверит все данные.
   * Следует найти в них qi и подходящие для установки, выпилив затем из сессии.
   * @param email E-mail юзера, т.е. его id.
   * @param session Изменяемые данные сессии.
   */
  def installFromSession(email:String, session:Session) = {
    println("installFromSession(): Not yet implemented")
    session
  }

  def dkey2skey(dkey:String) : String = "~" + dkey

  /**
   * Распарсить строку сессии qi_str в карту dkey -> qi_id.
   * @param qi_str строка сессии qi.
   * @return Карта dkey -> qi_id
   */
  def qiStr2Map(qi_str:String) : Map[String, String] = {
    val splits = qi_str.split(",") // -> ["a.ru", "asd3aef", "b.com", "sg53fsf", ... , ...]
    // теперь надо превратить список токенов в карту
    val acc0 : (List[(String, String)], Option[String])  =  List() -> None
    splits.foldLeft (acc0) {
      case ((acc, None), domain)         => (acc, Some(domain))
      case ((acc, Some(domain)), qi_id)  => (domain -> qi_id :: acc, None)
    }._1.toMap
  }


  private val urlProtoAllowedRe = "(?i)https?".r
  private val urlPathBadRe = "^/(.*(sear?ch|find).*)?$".r

  /**
   * Метод используется для проверки реферерров и присланных клиентом ссылок.
   * НЕ надо проверять ссылки, если они не относятся к указанному домену, ведут на главную или просто не корректны.
   * @param dkey Ключ домена, по которому гуляем
   * @param maybeUrl Возможно, ссылка. Возможно, относящаяся к домену. Возможно ведущая не на главную, а на другую страницу.
   * @param qi_id qi_id. Просто перенаправляется в нижележащую функцию.
   * @return true, если ссылка была выверена и отправлена в очередь на обход. Иначе false.
   */
  def maybeCheckQiAsync(dkey:String, maybeUrl:String, qi_id:String, sendEvents:Boolean): Boolean = {
    try {
      val url = new URL(maybeUrl)
      // Протокол - это http/https?
      val isCheck: Boolean = urlProtoAllowedRe.pattern.matcher(url.getProtocol).matches() && {
        // хост верен?
        val urlDkey = UrlUtil.normalizeHostname(url.getHost)
        urlDkey == dkey
      } && {
        // ссылка ведет НЕ на главную и НЕ на страницу встроенного поиска на сайте?
        val pathNorm = UrlUtil.normalizePath(url.getPath)
        !urlPathBadRe.pattern.matcher(pathNorm).find()
      }
      if (isCheck) {
        checkQiAsync(dkey, url.toExternalForm, Some(qi_id), sendEvents=sendEvents)
        true
      } else false

    } catch {
      case ex:MalformedURLException => false // Ссылка не верна. Ничего не делать, просто погасить исключение.
    }
  }


  /**
   * Быстро создать фьючерс и скомбинировать его с парсером html и анализатором всея добра.
   * Для комбинирования используются callback'и, ибо новые комбинированные фьючерсы тут никому не нужны.
   * @param dkey ключ домена
   * @param url ссылка. Обычно на гланге
   * @param qiIdOpt заявленный юзером qi_id, если есть. Может и не быть, если в момент установки на сайт зашел кто-то
   *              другой без qi_id в сессии.
   * @param sendEvents Слать ли события QiSuccess/QiError в шину sio_notifier?
   *                   Это бывает полезно для простой обратной связи с клиетом через websocket/comet/NewsQueue.
   *                   Таким образом, если sendEvents = true, то функция имеет явные сайд-эффекты.
   */
  def checkQiAsync(dkey:String, url:String, qiIdOpt:Option[String], sendEvents:Boolean): Future[Either[(String, List[SioJsInfoT]), SioJsV2]] = {
    // Запросить постановку в очередь указанной ссылки для указанного домена
    DomainRequester.queueUrl(dkey, url) map { case DRResp200(ct, istream) =>
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
        val result : Either[(String, List[SioJsInfoT]), SioJsV2] = try {
          val l = task.get(parseTimeout.toMillis, TimeUnit.MILLISECONDS)
          // Есть список найденных скриптов suggest_io.js на странице. Определить, есть ли среди них подходящий.
          l.find {
            case SioJsV2(_dkey, _qi_id) =>
              dkey == _dkey && qiIdOpt.isDefined && qiIdOpt.get == _qi_id

            case other => false

          } match {
            // find нашел подходящий скрипт
            case Some(info) =>
              val info2 = info.asInstanceOf[SioJsV2]
              logger.info("qi success for " + dkey + "! " + info2)
              Right(info2)

            // find не нашел ничего интересного на странице.
            case None =>
              // Послать уведомление о неудачной проверке. Если в списке есть элементы, то они "чужие", а если нет то что-то не так установлено.
              val errMsg = if (l.isEmpty)
                "No suggest.io JS found on this page. Not installed?"
              else
                "Expected suggest.io.js key NOT found, but found other suggest.io scripts on the page. Not yours?"
              Left(errMsg -> l)
          }

          // Произошла какая-то ошибка во внутреннем try, надо бы уведомить юзера об этом
        } catch {
          case te:TimeoutException =>
            task.cancel(true)
            //t.interrupt()   // cancel(true) сам вызывает t.interrupt()
            Left("Cannot parse page %s for qi: parsing timeout".format(url) -> Nil)

          // Внутри callable вылетел экзепшен. Он обернут в ExecutionException
          case ex:ExecutionException =>
            val errMsg = "Cannot parse page: internal parse error."
            logger.error("parse exception on %s" format url, ex)
            Left(errMsg -> Nil)

          // Какое-то неведомое исключение возникло.
          case ex:Throwable =>
            val errMsg = "Unknown error during qi."
            logger.error(errMsg, ex)
            Left(errMsg -> Nil)
        }
        // side-effects: Если приказано слать уведомления о ходе работы в шину событий, то сразу же сделать это, проанализировав результат анализа.
        if (sendEvents) {
          val qiNews: QiEventT = result match {
            // Всё верно. Можно заапрувить учетку юзера по отношению к этому домену.
            case Right(jsInfo) =>
              val qi_id1 = jsInfo.qi_id
              approve_qi(dkey, qi_id1)
              QiSuccess(dkey=dkey, qi_id=qi_id1, url=url)

            // Ниасилил. Надо чиркануть в логи и вернуть в новости ошибку qi.
            case Left((errMsg, listJsInstalled)) =>
              // TODO надо отреагировать на список найденных скриптов. Если там что-то есть с верным доменом, то значит надо установить.
              logger.warn("qi check failed on %s: %s".format(url, errMsg))
              QiError(dkey=dkey, qiIdOpt=qiIdOpt, url=url, msg=errMsg)
          }
          SioNotifier.publish(qiNews)
        }
        result

      } finally {
        istream.close()
      }
    }
  }


  /**
   * Зааппрувить указанный qi id. Нужно выдать юзеру с указанным qi права на указанный домен.
   * @param dkey ключ домена, который должен быть добавлен в базу кравлера.
   * @param qi_id qi id, относящиеся к неопределенному юзеру.
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


object QiEventUtil {
  private val headSneToken = Some("qi")

  def getClassifier(
    dkeyOpt:Option[String] = None,
    qiIdOpt:Option[String] = None,
    isSuccessOpt: Option[Boolean] = None): SioNotifier.Classifier = List(headSneToken, dkeyOpt, qiIdOpt, isSuccessOpt)
}


trait QiEventT extends SioEventTJSable {
  import play.api.libs.json._

  val dkey: String
  val url: String
  val qiIdOpt: Option[String]
  def isSuccess : Boolean


  def getClassifier: SioNotifier.Classifier = QiEventUtil.getClassifier(dkeyOpt = Some(dkey), qiIdOpt=qiIdOpt, isSuccessOpt = Some(isSuccess))

  def toJson: JsValue = {
    val jsonFields = "type" -> JsString(jsonEventType) ::
      "url" -> JsString(url) ::
      jsonMapTail
    JsObject(jsonFields)
  }

  def jsonEventType : String
  def jsonMapTail : List[(String, JsValue)]
}

/**
 * Уведомление об ошибке qi.
 * @param dkey Ключ домена
 * @param url Ссылка, которая проверялась на предмет qi
 * @param msg Сообщение о проблеме.
 */
case class QiError(dkey:String, qiIdOpt:Option[String], url:String, msg:String) extends QiEventT {
  val isSuccess = false

  def jsonEventType: String = "qi.error"
  def jsonMapTail: List[(String, JsValue)] = List("error" -> JsString(msg))
}

/**
 * Уведомление об успешном прохождении qi.
 * @param dkey Ключ домена
 * @param url Ссылка, для информации.
 */
case class QiSuccess(dkey:String, qi_id:String, url:String) extends QiEventT {
  val isSuccess = true
  val qiIdOpt = Some(qi_id)

  def jsonEventType: String = "qi.success"
  def jsonMapTail: List[(String, JsValue)] = List("is_js_installed" -> JsBoolean(isSuccess))
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