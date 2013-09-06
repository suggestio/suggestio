package util

import play.api.mvc.Session
import play.api.libs.concurrent.Execution.Implicits._
import org.apache.tika.metadata.{HttpHeaders, TikaMetadataKeys, Metadata}
import java.io.InputStream
import java.util.concurrent.{FutureTask, Callable, ExecutionException, TimeoutException, TimeUnit}
import io.suggest.sax._
import org.apache.tika.parser.AutoDetectParser
import scala.concurrent.duration._
import org.xml.sax.Attributes
import io.suggest.util.StringUtil.randomId
import io.suggest.util.event.subscriber.SioEventTJSable
import play.api.libs.json.{JsBoolean, JsString, JsValue}
import io.suggest.event.SioNotifier
import java.net.{MalformedURLException, URL}
import io.suggest.util.UrlUtil
import scala.concurrent.{Future, future}
import models.{MDomainQiAuthzTmp, MPersonDomainAuthz}
import util.Acl.PwOptT
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.LocalDate
import play.api.Logger

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
  private val parseTimeoutMs = parseTimeout.toMillis
  protected val timeout_msg = "timeout"
  val qiIdLen = 8
  val skeyPrefixStr = "~"
  val qiDtSepCh = ','

  // Нужно удалять из сессии юзера домены, которые он не осилил провалидировать. Тут - таймаут хранения qi-вхождений в сессии в днях.
  val qiInSessionDaysMax = 3
  // Даты, сохраняемые с qi в сессию должны иметь краткий формат. Форматтим дату в виде YYMMDD.
  private val qiDateFormatter = {
    new DateTimeFormatterBuilder()
      .appendYear(2, 2)
      .appendMonthOfYear(2)
      .appendDayOfMonth(2)
      .toFormatter
  }


  private def dtQiNow = qiDateFormatter.print(LocalDate.now())

  /**
   * Отправить в сессию данные по добавлению сайта.
   * @param dkey ключ домена.
   * @return qi_id для генерации скрипта и обновлённый объект session либо без него если session не изменялся.
   */
  def addDomainQiIntoSession(dkey:String)(implicit session:Session) : (String, Option[Session]) = {
    // Храним домен в сессии, используя его в качестве ключа внутри криптоконтейнера.
    val skey = dkey2skey(dkey)
    session.get(skey) match {
      // Этот домен уже упоминается в сессии. Опционально обновить дату и вернуть лежащий там qi_id.
      case Some(v) =>
        val Array(qi_id, dtQi) = v.split(qiDtSepCh)
        val _dtQiNow = dtQiNow
        val newSessionOpt: Option[Session] = if (dtQi == _dtQiNow) {
          None
        } else {
          val v = qi_id + "," + _dtQiNow
          Some(session + (skey -> v))
        }
        qi_id -> newSessionOpt

      // Нет такого домена в базе. Сгенерить новый qi_id и отправить его в сессию.
      case None =>
        val qi_id = randomId(qiIdLen)
        val v = qi_id + "," + qiDateFormatter.print(LocalDate.now())
        val session1 = session + (skey -> v)
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
      case Some(v) => v.startsWith(qi_id)
      case None => false
    }
  }


  /**
   * Прочитать текущий qi_id, записанный у юзера в сессии для указанного домена.
   * @param dkey Ключ домена.
   * @param session Сессия запроса.
   * @return Значение qi_id, если такое есть в сессии.
   */
  def getQiFromSession(dkey:String)(implicit session:Session): Option[String] = {
    session.get(dkey2skey(dkey)) map { v =>
      val sepPos = v.indexOf(qiDtSepCh)
      v.substring(0, sepPos)
    }
  }


  /**
   * Прочитать из сессии список быстро добавленных в систему доменов и выполнить те или иные действия.
   * Бывают следующие случаи:
   * - d+qi остались в сессии, но так же имеются в модели с is_verified=true. Это значит ЗАЛОГИНЕННЫЙ юзер добавил сайт
   *   и прошел процедуру qi. Нужно просто удалить домен с ключом из сессии.
   * - d+qi в сессии и в MDomainQiAuthzTmp. Это происходит, когда анонимус проходит qi, и затем логинится.
   *   Нужно удалить врЕменное разрешение, и создать нормальный MPersonDomainAuthz.
   * - d+qi в сессии не подходят под предыдущие условия. Значит не прошли валидацию. Смотреть дату, и удалить если истекло время хранения.
   * @param person_id id Юзер. Обычно это его e-mail.
   * @param session Исходные данные сессии.
   * @return Обновлённые данные сессии.
   */
  def installFromSession(person_id:String)(implicit session:Session): Session = {
    lazy val logPrefix = "installFromSession(%s): " format person_id
    // Фильтр ключей сессии имеет сайд-эффекты.
    val sessData1 = session.data.filter { case (k, v)  =>
      skey2dkey(k) match {
        // Это сериализованый ключ домена. Нужно продолжить анализ.
        case Some(dkey) =>
          // Пора распарсить и проанализировать значение по ключу.
          val Array(qi_id, dtQi) = v.split(qiDtSepCh)
          MPersonDomainAuthz.getForPersonDkey(dkey, person_id).map { da =>
          // Зареганный юзер проходил qi-проверку. Если прошел, то значит предикат должен вернуть false.
            !da.is_verified

          } getOrElse {
            MDomainQiAuthzTmp.get(dkey, qi_id) map { dqia =>
            // Анонимус добавлял сайт и успешно прошел qi-проверку. Нужно перенести сайт к зареганному анонимусу
              logger.debug(logPrefix + " approving (%s %s) to ex-anon".format(dkey, qi_id))
              MPersonDomainAuthz.newQi(id=qi_id, dkey=dkey, person_id=person_id, is_verified=true).save
              dqia.delete
              false

            } getOrElse {
              // Юзер не проходил проверок, или прошел но неудачно. Нужно проверить, не истекло ли время хранения qi в сессии.
              dtQi.toInt >= dtQiNow.toInt - qiInSessionDaysMax
            }
          }

        // Другие элементы сессии здесь не интересны. Не трогаем их.
        case None => true
      }
    }
    session.copy(sessData1)
  }


  /**
   * Обернуть ключ домена в ключ сессии. Такое оборачивание используется, чтобы отличать обычные ключи в сессии от
   * остальных возможных ключей без использования какой-либо группировки.
   * @param dkey Ключ домена, строка. Считается заведомо валидной строкой.
   * @return Строка, помеченная как dkey+qi.
   */
  def dkey2skey(dkey:String) : String = skeyPrefixStr + dkey

  /**
   * Попытаться извлечь (какбы десериализовать) dkey из ключа сессии.
   * @param skey Строка ключа сессии.
   * @return Some(dkey) если ключ является dkey с пометкой qi. Иначе None.
   */
  def skey2dkey(skey:String) : Option[String] = {
    if (skey.startsWith(skeyPrefixStr))
      Some(skey.substring(1))
    else
      None
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
  def maybeCheckQiAsync(dkey:String, maybeUrl:String, qi_id:String, sendEvents:Boolean)(implicit pw_opt:PwOptT): Option[Future[SioJsV2]] = {
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
        
        Logger.logger.debug("pathNorm : " + urlPathBadRe.pattern.matcher(pathNorm).find())
        
        urlPathBadRe.pattern.matcher(pathNorm).find()
      }
      
      if (isCheck) {
        Some(checkQiAsync(dkey, url.toExternalForm, Some(qi_id), sendEvents=sendEvents))

      } else None

    } catch {
      // Ссылка не верна. Ничего не делать, просто погасить исключение.
      case ex:MalformedURLException =>
      	Logger.logger.debug("malformed url")
      	None
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
  def checkQiAsync(dkey:String, url:String, qiIdOpt:Option[String], sendEvents:Boolean)(implicit pw_opt:PwOptT): Future[SioJsV2] = {
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
        val result: Either[(String, List[SioJsInfoT]), SioJsV2] = try {
          val l = task.get(parseTimeoutMs, TimeUnit.MILLISECONDS)
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
            val msg = "Cannot parse page %s for qi: parsing timeout" format url
            Left(msg -> Nil)

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
        // Отрезультировать фьючерс с возможными side-эффектами. Если приказано слать уведомления о ходе работы в шину событий, то сразу же сделать это, проанализировав результат анализа.
        result match {
          // Всё верно. Можно заапрувить учетку юзера по отношению к этому домену.
          case Right(jsInfo) =>
            if (sendEvents) {
              val qi_id1 = jsInfo.qi_id
              approve_qi(dkey, qi_id1)
              val qiNews = QiSuccess(dkey=dkey, qi_id=qi_id1, url=url)
              SioNotifier.publish(qiNews)
            }
            jsInfo

          // Ниасилил. Надо чиркануть в логи и сделать фьючерс неудачным.
          case Left((errMsg, listJsInstalled)) =>
            // TODO надо отреагировать на список найденных скриптов. Если там что-то есть с верным доменом, то значит надо установить.
            if (sendEvents) {
              logger.warn("qi check failed on %s: %s".format(url, errMsg))
              val qiNews = QiError(dkey=dkey, qiIdOpt=qiIdOpt, url=url, msg=errMsg)
              SioNotifier.publish(qiNews)
            }
            throw QiCheckException(message=errMsg, jsFound=listJsInstalled)
        }

      } finally {
        istream.close()
      }
    }
  }


  /**
   * Зааппрувить указанный qi id для указанного юзера. Нужно выдать юзеру с указанным qi права на указанный домен.
   * Сессия юзера почистится из контроллера, когда тот обраружит у зареганного юзера, что в сессии лежат dkey+qi уже заапрувленного домена.
   * @param dkey ключ домена, который должен быть добавлен в базу кравлера.
   * @param qi_id qi id, относящиеся к неопределенному юзеру.
   */
  def approve_qi(dkey:String, qi_id:String)(implicit pw_opt:PwOptT) {
    pw_opt match {
      case Some(pw) =>
        MPersonDomainAuthz.newQi(id=qi_id, dkey=dkey, person_id=pw.id, is_verified=true).save

      // Анонимус. Нужно поместить данные о профите во временное хранилище. Когда юзер зарегается, будет вызван installFromSession, который оттуда всё извлечет.
      case None =>
        MDomainQiAuthzTmp(dkey=dkey, id=qi_id).save
    }
  }

}

case class QiCheckException(message:String, jsFound:List[SioJsInfoT]) extends Exception


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
  val headSneToken = Some("qi")

  def getClassifier(
    dkeyOpt:Option[String] = None,
    qiIdOpt:Option[String] = None,
    isSuccessOpt: Option[Boolean] = None): SioNotifier.Classifier = List(headSneToken, dkeyOpt, qiIdOpt, isSuccessOpt)
}


trait QiEventT extends SioEventTJSable with DkeyContainerT {
  import play.api.libs.json._

  val dkey: String
  val url: String
  val qiIdOpt: Option[String]
  def isSuccess : Boolean


  def getClassifier: SioNotifier.Classifier = QiEventUtil.getClassifier(dkeyOpt = Some(dkey), qiIdOpt=qiIdOpt, isSuccessOpt = Some(isSuccess))

  def toJson: JsValue = {
    val jsonFields = {
      "type" -> JsString(jsonEventType) ::
      "url"  -> JsString(url) ::
      jsonMapTail ++ dkeyJsProps
    }
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