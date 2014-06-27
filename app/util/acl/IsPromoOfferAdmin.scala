package util.acl

import play.api.mvc.{Result, Request, ActionBuilder}
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import models._
import util.acl.PersonWrapper.PwOpt_t
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.14 18:06
 * Description: Проверка прав на управление оффером. Внутренне - это как бы враппер над IsMartShopAdmin.
 */
trait IsPromoOfferAdminBase extends ActionBuilder[AbstractRequestForPromoOfferAdm] {
  def offerId: String
  override def invokeBlock[A](request: Request[A], block: (AbstractRequestForPromoOfferAdm[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    // Проверяем есть ли права на магазин. Если из глубин вернулся shopId, то да.
    val shopIdOptFut: Future[Option[String]] = if (pwOpt.isDefined) {
      MShopPromoOffer.getShopIdFor(offerId) flatMap {
        case r @ Some(shopId) =>
          IsShopAdm.isShopAdminFull(shopId, pwOpt) map {
            case Some(_)  => r
            case None     => None
          }

        case None => Future successful None
      }
    } else Future successful None
    shopIdOptFut flatMap {
      case Some(shopId) =>
        srmFut flatMap { srm =>
          val req1 = RequestForPromoOfferAdm(shopId=shopId, offerId=offerId, pwOpt, request, srm)
          block(req1)
        }

      case None => IsAuth.onUnauth(request)
    }
  }
}
case class IsPromoOfferAdmin(offerId: String)
  extends IsPromoOfferAdminBase
  with ExpireSession[AbstractRequestForPromoOfferAdm]


/** Админство промо-оффера в магазине. */
abstract class AbstractRequestForPromoOfferAdm[A](request: Request[A]) extends AbstractRequestForShopAdm(request) {
  def offerId: String
}
case class RequestForPromoOfferAdm[A](shopId:String, offerId:String, pwOpt:PwOpt_t, request: Request[A], sioReqMd: SioReqMd)
  extends AbstractRequestForPromoOfferAdm(request)



/** Почти тоже самое, что и [[IsPromoOfferAdmin]], но внутри реквеста содержится полный оффер, к которому обращаются. */
trait IsPromoOfferAdminFullBase extends ActionBuilder[RequestForPromoOfferAdmFull] {
  def offerId: String
  override def invokeBlock[A](request: Request[A], block: (RequestForPromoOfferAdmFull[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    // Проверяем есть ли права на магазин. Если из глубин вернулся shopId, то да.
    val offerOptFut: Future[Option[MShopPromoOffer]] = if (pwOpt.isDefined) {
      MShopPromoOffer.getById(offerId) flatMap {
        case r @ Some(offer) =>
          IsShopAdm.isShopAdminFull(offer.shopId, pwOpt) map {
            case Some(_) => r
            case None    => None
          }

        case None => Future successful None
      }
    } else Future successful None
    offerOptFut flatMap {
      case Some(offer) =>
        srmFut flatMap { srm =>
          val req1 = RequestForPromoOfferAdmFull(offer=offer, pwOpt=pwOpt, request, srm)
          block(req1)
        }

      case None => IsAuth.onUnauth(request)
    }
  }
}
case class IsPromoOfferAdminFull(offerId: String)
  extends IsPromoOfferAdminFullBase
  with ExpireSession[RequestForPromoOfferAdmFull]


case class RequestForPromoOfferAdmFull[A](offer:MShopPromoOffer, pwOpt:PwOpt_t, request: Request[A], sioReqMd: SioReqMd)
  extends AbstractRequestForPromoOfferAdm(request) {
  def offerId = offer.id.get
  def shopId = offer.shopId
}

