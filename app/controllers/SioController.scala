package controllers

import play.api.mvc.{RequestHeader, SimpleResult, Controller}
import util.{HtmlCompressUtil, ContextT}
import scala.concurrent.Future
import play.api.i18n.Lang
import util.event.SiowebNotifier
import play.api.templates.{TxtFormat, HtmlFormat}
import play.api.libs.json.{JsString, JsValue}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.10.13 11:43
 * Description: Базис для контроллеров s.io.
 */

trait SioController extends Controller with ContextT {

  implicit protected def simpleResult2async(sr: SimpleResult): Future[SimpleResult] = {
    Future.successful(sr)
  }

  implicit def sn = SiowebNotifier

  implicit def html4email(html: HtmlFormat.Appendable) = HtmlCompressUtil.compressForEmail(html)

  implicit def html2jsStr(html: HtmlFormat.Appendable) = JsString(
    HtmlCompressUtil.compressForJson(html)
  )

  implicit def txt2str(txt: TxtFormat.Appendable): String = txt.body.trim

  implicit def txt2jsStr(txt: TxtFormat.Appendable) = JsString(txt)

}

