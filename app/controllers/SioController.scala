package controllers

import play.api.mvc.{RequestHeader, SimpleResult, Controller}
import util.ContextT
import scala.concurrent.Future
import play.api.i18n.Lang
import util.event.SiowebNotifier

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.10.13 11:43
 * Description:
 */

trait SioController extends Controller with ContextT {
  implicit protected def simpleResult2async(sr: SimpleResult): Future[SimpleResult] = {
    Future.successful(sr)
  }

  implicit def sn = SiowebNotifier
}

