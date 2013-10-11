package controllers

import play.api.mvc.{SimpleResult, Controller}
import util.ContextT
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.10.13 11:43
 * Description:
 */
trait SioController extends Controller with ContextT {

  implicit protected def simpleResult2async(sr: SimpleResult): Future[SimpleResult] = Future.successful(sr)

}
