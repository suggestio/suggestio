package controllers

import play.api.mvc._
import util.{PlayMacroLogsImpl, ContextImpl, HtmlCompressUtil, ContextT}
import scala.concurrent.{Promise, Future}
import play.api.i18n.Lang
import util.event.SiowebNotifier
import play.api.templates.{TxtFormat, HtmlFormat}
import play.api.libs.json.JsString
import play.api.libs.concurrent.Akka
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.Form
import io.suggest.img.SioImageUtilT
import util.acl.AbstractRequestWithPwOpt
import play.api.libs.Files.TemporaryFile
import models.MPictureTmp
import util.img.OutImgFmts
import net.sf.jmimemagic.Magic
import util.ContextImpl
import play.api.libs.json.JsString
import scala.Some
import play.api.mvc.Result

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.10.13 11:43
 * Description: Базис для контроллеров s.io.
 */

trait SioController extends Controller with ContextT {

  implicit protected def simpleResult2async(sr: Result): Future[Result] = {
    Future.successful(sr)
  }

  implicit def sn = SiowebNotifier

  implicit def html4email(html: HtmlFormat.Appendable) = HtmlCompressUtil.compressForEmail(html)

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
    implicit val ctx = ContextImpl()
    NotFound(views.html.static.http404Tpl())
  }

}


trait BruteForceProtect {

  val INVITE_CHECK_LAG_DURATION = 333 millis

  /** Система асинхронного платформонезависимого противодействия брутфорс-атакам. */
  // TODO Надо вынести её код в util.
  def bruteForceProtect: Future[_] = {
    // Для противодействию брутфорсу добавляем асинхронную задержку выполнения проверки по методике https://stackoverflow.com/a/17284760
    // TODO Нужно лимитировать попытки по IP клиента. ip можно закидывать в cache с коротким ttl.
    val lagPromise = Promise[Unit]()
    Akka.system.scheduler.scheduleOnce(INVITE_CHECK_LAG_DURATION) {
      lagPromise.success()
    }
    lagPromise.future
  }

}



trait LogoSupport extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Обработчик полученного логотипа в контексте реквеста, содержащего необходимые данные. Считается, что ACL-проверка уже сделана. */
  protected def handleLogo(imageUtil: SioImageUtilT, marker: String)(implicit request: Request[MultipartFormData[TemporaryFile]]): Result = {
    request.body.file("picture") match {
      case Some(pictureFile) =>
        val fileRef = pictureFile.ref
        val srcFile = fileRef.file
        // Если на входе png/gif, то надо эти форматы выставить в outFmt. Иначе jpeg.
        val srcMagicMatch = Magic.getMagicMatch(srcFile, false)
        val outFmt = OutImgFmts.forImageMime(srcMagicMatch.getMimeType)
        val mptmp = MPictureTmp.getForTempFile(fileRef, outFmt, Some(marker))
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

