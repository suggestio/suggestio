package controllers

import java.io.{FileInputStream, File}
import java.net.JarURLConnection
import java.nio.file.{StandardCopyOption, Files}

import models.Context
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import play.api.cache.Cache
import play.api.mvc._
import play.twirl.api.{TxtFormat, HtmlFormat}
import util._
import util.acl.SioRequestHeader
import scala.concurrent.{Promise, Future}
import util.event.SiowebNotifier
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import play.api.Play.{current, configuration}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.Form
import io.suggest.img.SioImageUtilT
import play.api.libs.Files.TemporaryFile
import models._
import util.img.{SvgUtil, OutImgFmts}
import net.sf.jmimemagic.Magic
import play.api.libs.json.JsString
import play.api.mvc.Result
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.10.13 11:43
 * Description: Базис для контроллеров s.io.
 */

object SioControllerUtil extends PlayLazyMacroLogsImpl {

  import LOGGER._

  /** Дата последней модификации кода проекта. Берется на основе текущего кода. */
  val PROJECT_CODE_LAST_MODIFIED: DateTime = {
    Option(getClass.getProtectionDomain)
      .flatMap { pd => Option(pd.getCodeSource) }
      .flatMap[Long] { cs =>
        val csUrl = cs.getLocation
        csUrl.getProtocol match {
          case "file" =>
            try {
              val f = new File(csUrl.getFile)
              val lm = f.lastModified()
              Some(lm)
            } catch {
              case ex: Exception =>
                error("Cannot infer last-modifed from file " + csUrl, ex)
                None
            }
          case "jar" =>
            try {
              val connOpt = Option(csUrl.openConnection)
              try {
                connOpt map {
                  case jaUrlConn: JarURLConnection =>
                    jaUrlConn.getJarEntry.getTime
                }
              } finally {
                connOpt foreach {
                  _.getInputStream.close()
                }
              }
            } catch {
              case ex: Exception =>
                warn("Cannot get jar entry time last-modified for " + csUrl, ex)
                None
            }
          case other =>
            error("Cannot detect last-modified for class source " + csUrl + " :: Unsupported protocol: " + other)
            None
        }
      }
      .fold(DateTime.now) { new DateTime(_) }
  }

}


/** Базовый хелпер для контроллеров suggest.io. Используется почти всегда вместо обычного Controller. */
trait SioController extends Controller with ContextT {

  implicit protected def simpleResult2async(sr: Result): Future[Result] = {
    Future.successful(sr)
  }

  implicit def sn = SiowebNotifier

  implicit def html4email(html: HtmlFormat.Appendable): String = {
    HtmlCompressUtil.compressForEmail(html)
  }

  implicit def html2jsStr(html: HtmlFormat.Appendable) = JsString(
    HtmlCompressUtil.compressForJson(html)
  )

  implicit def txt2str(txt: TxtFormat.Appendable): String = txt.body.trim

  implicit def txt2jsStr(txt: TxtFormat.Appendable) = JsString(txt)

  /** Построчное красивое форматирование ошибок формы для вывода в логи/консоль. */
  def formatFormErrors(formWithErrors: Form[_]) = {
    formWithErrors.errors.map { e => "  " + e.key + " -> " + e.message }.mkString("\n")
  }
  
  /** Тело экшена, генерирующее страницу 404. Используется при минимальном окружении. */
  def http404AdHoc(implicit request: RequestHeader): Result = {
    http404ctx(ContextImpl())
  }

  def http404ctx(implicit ctx: Context): Result = {
    NotFound(views.html.static.http404Tpl())
  }

  // Обработка возвратов (?r=/../.../..) либо редиректов.
  /** Вернуть редирект через ?r=/... либо через указанный вызов. */
  def RdrBackOr(rdrPath: Option[String])(dflt: => Call): Result = {
    val rdrTo = rdrPath
      .filter(_ startsWith "/")
      .getOrElse(dflt.url)
    Results.Redirect(rdrTo)
  }

  def RdrBackOrFut(rdrPath: Option[String])(dflt: => Future[Call]): Future[Result] = {
    rdrPath
      .filter(_ startsWith "/")
      .fold { dflt.map(_.url) }  { Future successful }
      .map { r => Results.Redirect(r) }
  }

  /** Бывает нужно просто впендюрить кеш для результата, но только когда продакшен. */
  def cacheControlShort(r: Result): Result = {
    val v = if (play.api.Play.isProd) {
      "public, max-age=600"
    } else {
      "no-cache"
    }
    r.withHeaders(CACHE_CONTROL -> v)
  }
}


/** Функция для защиты от брутфорса. Повзоляет сделать асинхронную задержку выполнения экшена в контроллере.
  * Настраивается путём перезаписи констант. Если LAG = 333 ms, и DIVISOR = 3, то скорость ответов будет такова:
  * 0*333 = 0 ms (3 раза), затем 1*333 = 333 ms (3 раза), затем 2*333 = 666 ms (3 раза), и т.д.
  */
trait BruteForceProtect extends SioController with PlayMacroLogsI {

  /** имя модуля по конфигу. Нельзя, чтобы ключ конфига содержал знак $, который скала добавляет
    * ко всем объектам. Используется только при инициализации. */
  val myBfpConfName = getClass.getSimpleName.replace("$", "")

  /** Шаг задержки. Добавляемая задержка ответа будет кратна этому лагу. */
  val BRUTEFORCE_LAG_MS = configuration.getInt(s"bfp.$myBfpConfName.lag_ms") getOrElse BRUTEFORCE_ATTACK_LAG_MS_DFLT
  def BRUTEFORCE_LAG_MS_DFLT = 222

  /** Префикс в кеше для ip-адреса. */
  val BRUTEFORCE_CACHE_PREFIX = configuration.getInt(s"bfp.$myBfpConfName.cache.prefix") getOrElse BRUTEFORCE_CACHE_PREFIX_DFLT
  def BRUTEFORCE_CACHE_PREFIX_DFLT = "bfp:"

  /** Какой лаг уже считается лагом текущей атаки? (в миллисекундах) */
  val BRUTEFORCE_ATTACK_LAG_MS = configuration.getInt(s"bfp.$myBfpConfName.attack.lag.ms") getOrElse BRUTEFORCE_ATTACK_LAG_MS_DFLT
  def BRUTEFORCE_ATTACK_LAG_MS_DFLT = 2000

  /** Нормализация кол-ва попыток происходит по этому целому числу. */
  val BRUTEFORCE_TRY_COUNT_DIVISOR = configuration.getInt(s"bfp.$myBfpConfName.try.count.divisor") getOrElse BRUTEFORCE_TRY_COUNT_DEADLINE_DFLT
  def BRUTEFORCE_TRY_COUNT_DIVISOR_DFLT = 2

  /** Время хранения в кеше инфы о попытках для ip-адреса. */
  val BRUTEFORCE_CACHE_TTL = configuration.getInt(s"bfp.$myBfpConfName.cache.ttl").getOrElse(BRUTEFORCE_CACHE_TTL_SECONDS_DFLT).seconds
  def BRUTEFORCE_CACHE_TTL_SECONDS_DFLT = 30

  /** Макс кол-во попыток, после которого запросы будут отправляться в помойку. */
  val BRUTEFORCE_TRY_COUNT_DEADLINE = configuration.getInt(s"bfp.$myBfpConfName.cache.ttl") getOrElse BRUTEFORCE_TRY_COUNT_DEADLINE_DFLT
  def BRUTEFORCE_TRY_COUNT_DEADLINE_DFLT = 40

  def bruteForceLogPrefix(implicit request: RequestHeader): String = {
    s"bruteForceProtect($myBfpConfName/${request.remoteAddress}): ${request.method} ${request.path}?${request.rawQueryString} : "
  }

  /** Система асинхронного платформонезависимого противодействия брутфорс-атакам. */
  def bruteForceProtected(f: => Future[Result])(implicit request: SioRequestHeader): Future[Result] = {
    // Для противодействию брутфорсу добавляем асинхронную задержку выполнения проверки по методике https://stackoverflow.com/a/17284760
    val ck = BRUTEFORCE_CACHE_PREFIX + request.remoteAddress
    val prevTryCount: Int = Cache.getAs[Int](ck) getOrElse 0
    if (prevTryCount > BRUTEFORCE_TRY_COUNT_DEADLINE) {
      // Наступил предел толерантности к атаке.
      onBruteForceDeadline(f, prevTryCount)
    } else {
      val lagMs = getLagMs(prevTryCount)
      val resultFut: Future[Result] = if (lagMs <= 0) {
        f
      } else {
        // Нужно решить, что делать с запросом.
        if (lagMs > BRUTEFORCE_ATTACK_LAG_MS) {
          // Кажется, идёт брутфорс-атака.
          onBruteForceAttackDetected(f, lagMs, prevTryCount = prevTryCount)
        } else {
          // Есть подозрение на брутфорс.
          onBruteForceAttackSuspicion(f, lagMs, prevTryCount = prevTryCount)
        }
      }
      // Закинуть в кеш инфу о попытке
      Cache.set(ck, prevTryCount + 1, BRUTEFORCE_CACHE_TTL)
      resultFut
    }
  }

  /** Формула рассчёта лага брутфорса. */
  def getLagMs(prevTryCount: Int): Int = {
    val lagLevel = prevTryCount / BRUTEFORCE_TRY_COUNT_DIVISOR
    lagLevel * lagLevel * BRUTEFORCE_LAG_MS
  }

  def onBruteForceReplyLagged(f: => Future[Result], lagMs: Int): Future[Result] = {
    val lagPromise = Promise[Result]()
    Akka.system.scheduler.scheduleOnce(lagMs milliseconds) {
      lagPromise completeWith f
    }
    lagPromise.future
  }

  /** Подозрение на брутфорс. В нормале - увеличивается лаг. */
  def onBruteForceAttackSuspicion(f: => Future[Result], lagMs: Int, prevTryCount: Int)(implicit request: SioRequestHeader): Future[Result] = {
    LOGGER.debug(s"${bruteForceLogPrefix}Inserting lag $lagMs ms, try = $prevTryCount")
    onBruteForceReplyLagged(f, lagMs)
  }

  /** Замечен брутфорс. */
  def onBruteForceAttackDetected(f: => Future[Result], lagMs: Int, prevTryCount: Int)(implicit request: SioRequestHeader): Future[Result] = {
    LOGGER.warn(s"${bruteForceLogPrefix}Attack is going on! Inserting fat lag $lagMs ms, prev.try count = $prevTryCount.")
    onBruteForceReplyLagged(f, lagMs)
  }

  /** Наступил дедлайн, т.е. атака точно подтверждена, и пора дропать запросы. */
  def onBruteForceDeadline(f: => Future[Result], prevTryCount: Int)(implicit request: SioRequestHeader): Future[Result] = {
    LOGGER.warn(bruteForceLogPrefix + "Too many bruteforce retries. Dropping request...")
    TooManyRequest("Service overloaded. Please try again later.")
  }

  /** Если нет возможности использовать implicit request, тут явная версия: */
  def bruteForceProtectedNoimpl(request: SioRequestHeader)(f: => Future[Result]): Future[Result] = {
    bruteForceProtected(f)(request)
  }

}




/** compat-прослойка для контроллеров, которые заточены под ТЦ и магазины.
  * После унификации в web21 этот контроллер наверное уже не будет нужен. */
trait ShopMartCompat {
  def getShopById(shopId: String) = MAdnNode.getByIdType(shopId, AdNetMemberTypes.SHOP)
  def getShopByIdCache(shopId: String) = MAdnNodeCache.getByIdType(shopId, AdNetMemberTypes.SHOP)

  def getMartById(martId: String) = MAdnNode.getByIdType(martId, AdNetMemberTypes.MART)
  def getMartByIdCache(martId: String) = MAdnNodeCache.getByIdType(martId, AdNetMemberTypes.MART)
}
