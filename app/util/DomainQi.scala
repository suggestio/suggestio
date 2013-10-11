package util

import play.api.mvc.Session
import play.api.libs.concurrent.Execution.Implicits._
import org.apache.tika.metadata.{HttpHeaders, TikaMetadataKeys, Metadata}
import java.io.InputStream
import java.util.concurrent.{FutureTask, Callable, ExecutionException, TimeoutException, TimeUnit}
import io.suggest.sax._
import org.apache.tika.parser.AutoDetectParser
import org.xml.sax.Attributes
import io.suggest.util.StringUtil.randomId
import util.event._
import java.net.{MalformedURLException, URL}
import io.suggest.util.UrlUtil
import scala.concurrent.{Await, Future}
import models.{MDomainQiAuthzTmp, MPersonDomainAuthz}
import util.acl.PersonWrapper.PwOpt_t
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.LocalDate
import play.api.Logger
import play.api.Play.current
import scala.concurrent.duration._

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

  val parseTimeoutMs = current.configuration.getInt("domain.qi.parse.timeout_ms") getOrElse 5000
  protected val timeout_msg = "timeout"
  val qiIdLen = current.configuration.getInt("domain.qi.id.len") getOrElse 8
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


  def dtQiNow = qiDateFormatter.print(LocalDate.now())

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
  def installFromSession(person_id:String)(implicit session:Session): Future[Session] = {
    lazy val logPrefix = "installFromSession(%s): " format person_id
    // Разделить карту сессии на относящихся к qi и остальные.
    val sesMap = session.data.groupBy { case (k,_) => isSkey(k) }
    // Относящиеся к qi данные параллельно проанализировать на профпригодность.
    val skeySession = sesMap.getOrElse(true, Map.empty)
    Future.traverse(skeySession) { case kv @ (k, v) =>
      val dkey = skey2dkey(k)
      // Пора распарсить и проанализировать значение по ключу.
      val Array(qi_id, dtQi) = v split qiDtSepCh
      // Надо ли оставить этот элемент сессии (true)? Или стереть?
      val keepFut = MPersonDomainAuthz.getForPersonDkey(dkey, person_id) flatMap {
        case Some(da) =>
          // Зареганный юзер проходил qi-проверку. Если прошел, то значит предикат должен вернуть false.
          Future.successful(!da.is_verified)

        case None =>
          MDomainQiAuthzTmp.getForDkeyId(dkey, qi_id) flatMap {
            // Анонимус добавлял сайт и успешно прошел qi-проверку. Нужно перенести сайт к зареганному анонимусу
            case Some(dqia) =>
              logger.debug(logPrefix + s" approving ($dkey $qi_id) to ex-anon")
              // side-effects:
              MPersonDomainAuthz.newQi(id=qi_id, dkey=dkey, person_id=person_id, is_verified=true).save flatMap {_ =>
                // Удалить анонимный qi из базы и из сессии, ибо больше не нужен.
                dqia.delete map { _ => false }
              }

            // Юзер не проходил проверок, или прошел но неудачно. Нужно проверить, не истекло ли время хранения qi в сессии.
            case None =>
              val result = dtQi.toInt >= dtQiNow.toInt - qiInSessionDaysMax
              Future successful result
          }
      }
      keepFut map {
        case true  => Some(kv)
        case false => None
      }
    }.map { skeySession2 =>
      val otherSession = sesMap.getOrElse(false, Map.empty).toList
      val s2 = skeySession2.foldLeft(otherSession) {
        case (accSes, Some(kv)) => kv :: accSes
        case (accSes, None)     => accSes
      }
      session.copy(s2.toMap)
    }
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
  def skey2dkeyOpt(skey:String) : Option[String] = {
    if (isSkey(skey))
      Some(skey2dkey(skey))
    else
      None
  }
  def skey2dkey(skey: String): String = skey.substring(skeyPrefixStr.length)
  def isSkey(skey: String): Boolean = skey startsWith skeyPrefixStr


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
  def maybeCheckQiAsync(dkey:String, maybeUrl:String, qi_id:String, sendEvents:Boolean, pwOpt:PwOpt_t): Option[Future[SioJsV2]] = {
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
        Some(checkQiAsync(dkey, url.toExternalForm, Some(qi_id), sendEvents=sendEvents, pwOpt=pwOpt))

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
  def checkQiAsync(dkey:String, url:String, qiIdOpt:Option[String], sendEvents:Boolean, pwOpt:PwOpt_t): Future[SioJsV2] = {
    // Запросить постановку в очередь указанной ссылки для указанного домена
    DomainRequester.queueUrl(dkey, url) flatMap { case DRResp200(ct, istream) =>
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
            // Найден js нужной версии. Нужно проверить его компоненты.
            case SioJsV2(_dkey, _qi_id) =>
              dkey == _dkey && {
                // Сообщить системе, что есть событие установки скрипта на сайт.
                SiowebNotifier publish ValidSioJsFoundOnSite(dkey)
                // Что вернуть юзеру? Это зависит от qi_id (его наличия и совпадения с текущим в сессии).
                qiIdOpt.isDefined  &&  qiIdOpt.get == _qi_id
              }

            // Что-то иное. Вероятно SioJs старой версии, который для qi не подходит.
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

        } catch {   // Произошла какая-то ошибка во внутреннем try, надо бы уведомить юзера об этом
          // Таймаут работы парсера. Возможно юзер подсунул бесконечный документ. Надо прервать его.
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
            // Отправить юзеру соответствующее уведомление.
            if (sendEvents) {
              val qi_id1 = jsInfo.qi_id
              approveQi(dkey, qi_id1, pwOpt)
              val qiNews = QiSuccess(dkey=dkey, qi_id=qi_id1, url=url)
              SiowebNotifier publish qiNews
            }
            Future.successful(jsInfo)

          // Ниасилил. Надо чиркануть в логи и сделать фьючерс неудачным.
          case Left((errMsg, listJsInstalled)) =>
            // TODO надо отреагировать на список найденных скриптов. Если там что-то есть с верным доменом, то значит надо установить.
            if (sendEvents) {
              logger.warn("qi check failed on %s: %s" format (url, errMsg))
              val qiNews = QiError(dkey=dkey, qiIdOpt=qiIdOpt, url=url, msg=errMsg)
              SiowebNotifier publish qiNews
            }
            val ex = QiCheckException(message=errMsg, jsFound=listJsInstalled)
            Future.failed(ex)
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
  def approveQi(dkey:String, qi_id:String, pwOpt:PwOpt_t) {
    pwOpt match {
      case Some(pw) =>
        MPersonDomainAuthz.newQi(id=qi_id, dkey=dkey, person_id=pw.id, is_verified=true).save

      // Анонимус. Нужно поместить данные о профите во временное хранилище. Когда юзер зарегается, будет вызван installFromSession, который оттуда всё извлечет.
      case None =>
        MDomainQiAuthzTmp(dkey=dkey, id=qi_id).save
    }
  }

}

sealed case class QiCheckException(message:String, jsFound:List[SioJsInfoT]) extends Exception


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

