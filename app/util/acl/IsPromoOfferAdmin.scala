package util.acl

import play.api.mvc.{SimpleResult, Request, ActionBuilder}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import models.MShopPromoOffer

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.14 18:06
 * Description: Проверка прав на управление оффером. Внутренне - это как бы враппер над IsMartShopAdmin.
 */

case class IsPromoOfferAdmin(offerId: String) extends ActionBuilder[AbstractRequestForPromoOfferAdm] {
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestForPromoOfferAdm[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    // Проверяем есть ли права на магазин. Если из глубин вернулся shopId, то да.
    val shopIdOptFut: Future[Option[Int]] = if (pwOpt.isDefined) {
      MShopPromoOffer.getShopIdFor(offerId) flatMap {
        case Some(shopId) =>
          IsMartShopAdmin.isShopAdmin(shopId, pwOpt) map {
            case true  => Some(shopId)
            case false => None
          }

        case None => Future successful None
      }
    } else Future successful None
    shopIdOptFut flatMap {
      case Some(shopId) =>
        val req1 = RequestForPromoOfferAdm(shopId=shopId, offerId=offerId, pwOpt=pwOpt, request=request)
        block(req1)

      case None => IsAuth.onUnauth(request)
    }
  }
}
