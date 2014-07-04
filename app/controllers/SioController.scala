package controllers

import models.Context
import play.api.cache.Cache
import play.api.mvc._
import play.twirl.api.{TxtFormat, HtmlFormat}
import util._
import scala.concurrent.{Promise, Future}
import util.event.SiowebNotifier
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.Form
import io.suggest.img.SioImageUtilT
import play.api.libs.Files.TemporaryFile
import models._
import util.img.OutImgFmts
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
}


/** Функция для защиты от брутфорса. Повзоляет сделать асинхронную задержку выполнения экшена в контроллере.
  * Настраивается путём перезаписи констант. Если LAG = 333 ms, и DIVISOR = 3, то скорость ответов будет такова:
  * 0*333 = 0 ms (3 раза), затем 1*333 = 333 ms (3 раза), затем 2*333 = 666 ms (3 раза), и т.д.
  */
trait BruteForceProtect extends PlayMacroLogsI {

  /** Шаг задержки. Добавляемая задержка ответа будет кратна этому лагу. */
  val BRUTEFORCE_LAG_MS = 222

  /** Префикс в кеше для ip-адреса. */
  val BRUTEFORCE_CACHE_PREFIX = "bfp:"

  /** Нормализация кол-ва попыток происходит по этому целому числу. */
  val BRUTEFORCE_TRY_COUNT_DIVISOR = 2

  /** Время хранения в кеше инфы о попытках для ip-адреса. */
  val BRUTEFORCE_CACHE_TTL = 30 seconds

  /** Система асинхронного платформонезависимого противодействия брутфорс-атакам. */
  def bruteForceProtect(implicit request: RequestHeader): Future[_] = {
    // Для противодействию брутфорсу добавляем асинхронную задержку выполнения проверки по методике https://stackoverflow.com/a/17284760
    val raddr = request.remoteAddress
    val ck = BRUTEFORCE_CACHE_PREFIX + raddr
    val prevTryCount: Int = Cache.getAs[Int](ck) getOrElse 0
    val lagLevel = prevTryCount / BRUTEFORCE_TRY_COUNT_DIVISOR
    val lagMs = lagLevel * lagLevel * BRUTEFORCE_LAG_MS
    val resultFut = if (lagMs <= 0) {
      Future successful None
    } else {
      // Запускаем таймер задержки исполнения реквеста, пока в фоне.
      val lagPromise = Promise[Unit]()
      Akka.system.scheduler.scheduleOnce(lagMs milliseconds) {
        lagPromise.success()
      }
      lazy val logPrefix = s"bruteForceProtect($raddr): ${request.method} ${request.path} :: "
      if (lagMs > 2000) {
        LOGGER.warn(s"${logPrefix}Attack is going on! Inserting fat lag $lagMs ms, prev.try count = $prevTryCount.")
      } else {
        LOGGER.debug(s"${logPrefix}Inserting lag $lagMs ms, try = $prevTryCount")
      }
      lagPromise.future
    }
    // Закинуть в кеш инфу о попытке
    Cache.set(ck, prevTryCount + 1, BRUTEFORCE_CACHE_TTL)
    resultFut
  }

}



/** Функционал для поддержки работы с логотипами. Он является общим для ad, shop и mart-контроллеров. */
trait TempImgSupport extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Обработчик полученного логотипа в контексте реквеста, содержащего необходимые данные. Считается, что ACL-проверка уже сделана. */
  protected def _handleTempImg(imageUtil: SioImageUtilT, marker: Option[String])(implicit request: Request[MultipartFormData[TemporaryFile]]): Result = {
    request.body.file("picture") match {
      case Some(pictureFile) =>
        val fileRef = pictureFile.ref
        val srcFile = fileRef.file
        // Если на входе png/gif, то надо эти форматы выставить в outFmt. Иначе jpeg.
        val srcMagicMatch = Magic.getMagicMatch(srcFile, false)
        val outFmt = OutImgFmts.forImageMime(srcMagicMatch.getMimeType)
        val mptmp = MPictureTmp.getForTempFile(fileRef.file, outFmt, marker)
        try {
          imageUtil.convert(srcFile, mptmp.file)
          Ok(Img.jsonTempOk(mptmp.filename))
        } catch {
          case ex: Throwable =>
            debug(s"ImageMagick crashed on file $srcFile ; orig: ${pictureFile.filename} :: ${pictureFile.contentType} [${srcFile.length} bytes]", ex)
            val reply = Img.jsonImgError("Unsupported picture format.")
            BadRequest(reply)
        } finally {
          srcFile.delete()
        }

      case None =>
        val reply = Img.jsonImgError("Picture not found in request.")
        NotAcceptable(reply)
    }
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
