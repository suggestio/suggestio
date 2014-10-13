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
}


/** Функция для защиты от брутфорса. Повзоляет сделать асинхронную задержку выполнения экшена в контроллере.
  * Настраивается путём перезаписи констант. Если LAG = 333 ms, и DIVISOR = 3, то скорость ответов будет такова:
  * 0*333 = 0 ms (3 раза), затем 1*333 = 333 ms (3 раза), затем 2*333 = 666 ms (3 раза), и т.д.
  */
trait BruteForceProtect extends SioController with PlayMacroLogsI {

  /** имя модуля по конфигу. Нельзя, чтобы ключ конфига содержал знак $, который скала добавляет
    * ко всем объектам. Используется только при инициализации. */
  def myBfpConfName = getClass.getSimpleName.replace("$", "")

  /** Шаг задержки. Добавляемая задержка ответа будет кратна этому лагу. */
  val BRUTEFORCE_LAG_MS = configuration.getInt(s"bfp.$myBfpConfName.lag_ms") getOrElse 222

  /** Префикс в кеше для ip-адреса. */
  val BRUTEFORCE_CACHE_PREFIX = configuration.getInt(s"bfp.$myBfpConfName.cache.prefix") getOrElse "bfp:"

  /** Нормализация кол-ва попыток происходит по этому целому числу. */
  val BRUTEFORCE_TRY_COUNT_DIVISOR = configuration.getInt(s"bfp.$myBfpConfName.try.count.divisor") getOrElse 2

  /** Время хранения в кеше инфы о попытках для ip-адреса. */
  val BRUTEFORCE_CACHE_TTL = configuration.getInt(s"bfp.$myBfpConfName.cache.ttl").getOrElse(30).seconds

  /** Макс кол-во попыток, после которого запросы будут отправляться в помойку. */
  val BRUTEFORCE_TRY_COUNT_DEADLINE = configuration.getInt(s"bfp.$myBfpConfName.cache.ttl") getOrElse 40

  /** Система асинхронного платформонезависимого противодействия брутфорс-атакам. */
  def bruteForceProtected(f: => Future[Result])(implicit request: SioRequestHeader): Future[Result] = {
    // Для противодействию брутфорсу добавляем асинхронную задержку выполнения проверки по методике https://stackoverflow.com/a/17284760
    val raddr = request.remoteAddress
    val ck = BRUTEFORCE_CACHE_PREFIX + raddr
    val prevTryCount: Int = Cache.getAs[Int](ck) getOrElse 0
    if (prevTryCount > BRUTEFORCE_TRY_COUNT_DEADLINE) {
      TooManyRequest("Service overloaded. Please try again later.")
    } else {
      val lagLevel = prevTryCount / BRUTEFORCE_TRY_COUNT_DIVISOR
      val lagMs = lagLevel * lagLevel * BRUTEFORCE_LAG_MS
      val resultFut: Future[Result] = if (lagMs <= 0) {
        f
      } else {
        // Запускаем таймер задержки исполнения реквеста, пока в фоне.
        val lagPromise = Promise[Result]()
        Akka.system.scheduler.scheduleOnce(lagMs milliseconds) {
          lagPromise completeWith f
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

  /** Если нет возможности использовать implicit request, тут явная версия: */
  def bruteForceProtectedNoimpl(request: SioRequestHeader)(f: => Future[Result]): Future[Result] = {
    bruteForceProtected(f)(request)
  }

}



/** Функционал для поддержки работы с логотипами. Он является общим для ad, shop и mart-контроллеров. */
trait TempImgSupport extends SioController with PlayMacroLogsI {

  /** Обработчик полученной картинки в контексте реквеста, содержащего необходимые данные. Считается, что ACL-проверка уже сделана. */
  protected def _handleTempImg(imageUtil: SioImageUtilT, marker: Option[String], preserveFmt: Boolean = false)
                              (implicit request: Request[MultipartFormData[TemporaryFile]]): Result = {
    try {
      request.body.file("picture") match {
        case Some(pictureFile) =>
          val fileRef = pictureFile.ref
          val srcFile = fileRef.file
          val srcMagicMatch = Magic.getMagicMatch(srcFile, false)
          // Отрабатываем svg: не надо конвертить.
          val srcMime = srcMagicMatch.getMimeType
          if (SvgUtil maybeSvgMime srcMime) {
            // Это svg?
            if (SvgUtil isSvgFileValid srcFile) {
              // Это svg. Надо его сжать и переместить в tmp-хранилище.
              val newSvg = HtmlCompressUtil.compressSvgFromFile(srcFile)
              val mptmp = MPictureTmp.getForTempFile(srcFile, OutImgFmts.SVG, marker)
              FileUtils.writeStringToFile(mptmp.file, newSvg)
              Ok(Img.jsonTempOk(mptmp.filename))
            } else {
              val reply = Img.jsonImgError("SVG format invalid or not supported.")
              NotAcceptable(reply)
            }

          } else {
            // Это наверное растровая картинка.
            val outFmt = if (preserveFmt) {
              OutImgFmts.forImageMime(srcMime)
            } else {
              OutImgFmts.JPEG
            }
            try {
              val mptmp = MPictureTmp.getForTempFile(fileRef.file, outFmt, marker)
              imageUtil.convert(srcFile, mptmp.file)
              Ok(Img.jsonTempOk(mptmp.filename))
            } catch {
              case ex: Throwable =>
                LOGGER.debug(s"ImageMagick crashed on file $srcFile ; orig: ${pictureFile.filename} :: ${pictureFile.contentType} [${srcFile.length} bytes]", ex)
                val reply = Img.jsonImgError("Unsupported picture format.")
                NotAcceptable(reply)
            }
          }

        // В реквесте не найдена именованая часть, содержащая картинку.
        case None =>
          val reply = Img.jsonImgError("picture part not found in request.")
          NotAcceptable(reply)
      }

    } finally {
      // Удалить все файлы, которые были приняты в реквесте.
      request.body.files.foreach { f =>
        f.ref.file.delete()
      }
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
