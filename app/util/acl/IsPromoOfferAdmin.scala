package util.acl

import play.api.mvc.{SimpleResult, Request, ActionBuilder}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import models._
import util.acl.PersonWrapper.PwOpt_t
import MShop.ShopId_t

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
    val shopIdOptFut: Future[Option[ShopId_t]] = if (pwOpt.isDefined) {
      MShopPromoOffer.getShopIdFor(offerId) flatMap {
        case r @ Some(shopId) =>
          IsMartShopAdmin.isShopAdmin(shopId, pwOpt) map {
            case true  => r
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


/** Админство промо-оффера в магазине. */
abstract class AbstractRequestForPromoOfferAdm[A](request: Request[A]) extends AbstractRequestForShopAdm(request) {
  def offerId: String
}
case class RequestForPromoOfferAdm[A](shopId:ShopId_t, offerId:String, pwOpt:PwOpt_t, request: Request[A]) extends AbstractRequestForPromoOfferAdm(request)



/** Почти тоже самое, что и [[IsPromoOfferAdmin]], но внутри реквеста содержится полный оффер, к которому обращаются. */
case class IsPromoOfferAdminFull(offerId: String) extends ActionBuilder[RequestForPromoOfferAdmFull] {
  protected def invokeBlock[A](request: Request[A], block: (RequestForPromoOfferAdmFull[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    // Проверяем есть ли права на магазин. Если из глубин вернулся shopId, то да.
    val offerOptFut: Future[Option[MShopPromoOffer]] = if (pwOpt.isDefined) {
      MShopPromoOffer.getById(offerId) flatMap {
        case r @ Some(offer) =>
          IsMartShopAdmin.isShopAdmin(offer.shop_id, pwOpt) map {
            case true  => r
            case false => None
          }

        case None => Future successful None
      }
    } else Future successful None
    offerOptFut flatMap {
      case Some(offer) =>
        val req1 = RequestForPromoOfferAdmFull(offer=offer, pwOpt=pwOpt, request=request)
        block(req1)

      case None => IsAuth.onUnauth(request)
    }
  }
}

case class RequestForPromoOfferAdmFull[A](offer:MShopPromoOffer, pwOpt:PwOpt_t, request: Request[A]) extends AbstractRequestForPromoOfferAdm(request) {
  def offerId = offer.id.get
  def shopId = offer.shop_id
}

