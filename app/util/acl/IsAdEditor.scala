package util.acl

import play.api.mvc.{SimpleResult, Request, ActionBuilder}
import models._
import util.acl.PersonWrapper.PwOpt_t
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.03.14 16:39
 * Description: Управление рекламной карточкой.
 */

/** Редактировать карточку может только владелец магазина. */
case class IsAdEditor(adId: String) extends ActionBuilder[RequestWithAd] {
  protected def invokeBlock[A](request: Request[A], block: (RequestWithAd[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    MMartAd.getById(adId) flatMap {
      case Some(mad) =>
        // TODO Нужно проверять права доступа как-то: для ТЦ и для магазина
        if (PersonWrapper isSuperuser pwOpt) {
          val req1 = RequestWithAd(mad, request, pwOpt)
          block(req1)
        } else {
          IsAuth onUnauth request
        }

      case None => IsAuth onUnauth request
    }
  }
}


case class RequestWithAd[A](mad: MMartAd, request: Request[A], pwOpt: PwOpt_t)
  extends AbstractRequestWithPwOpt(request)

