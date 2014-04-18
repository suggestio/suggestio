package util.acl

import util.acl.PersonWrapper.PwOpt_t
import util.PlayMacroLogsImpl
import scala.concurrent.Future
import play.api.mvc.{Result, Request, ActionBuilder}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.14 13:58
 * Description: Проверка прав на управление ТЦ.
 */
object IsMartAdmin extends controllers.ShopMartCompat {


  def isMartAdmin(martId: String, pwOpt: PwOpt_t): Future[Option[MAdnNode]] = {
    val fut = getMartByIdCache(martId)
    IsAdnNodeAdmin.checkAdnNodeCreds(fut, pwOpt)
  }

}

import IsMartAdmin._


/** Административная операция над торговым центром. */
case class IsMartAdmin(martId: String) extends ActionBuilder[AbstractRequestForMartAdm] {
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestForMartAdm[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOptAdn(pwOpt, martId)
    isMartAdmin(martId, pwOpt) flatMap {
      case Some(mmart) =>
        srmFut flatMap { srm =>
          val req1 = RequestForMartAdm(mmart, request, pwOpt, srm)
          block(req1)
        }

      case _ =>
        IsAuth.onUnauth(request)
    }
  }
}


/** Какая-то административная операция над магазином, подразумевающая права на ТЦ. */
case class IsMartAdminShop(shopId: String) extends ActionBuilder[RequestForMartShopAdm] {
  protected def invokeBlock[A](request: Request[A], block: (RequestForMartShopAdm[A]) => Future[Result]): Future[Result] = {
    IsMartAdmin.getShopByIdCache(shopId) flatMap {
      case Some(mshop) if mshop.adn.supId.isDefined =>
        val pwOpt = PersonWrapper.getFromRequest(request)
        val martId = mshop.adn.supId.get
        val srmFut = SioReqMd.fromPwOptAdn(pwOpt, martId)
        isMartAdmin(martId, pwOpt) flatMap {
          case Some(mmart) =>
            srmFut flatMap { srm =>
              val req1 = RequestForMartShopAdm(mshop, mmart=mmart, request, pwOpt, srm)
              block(req1)
            }

          case None =>
          IsAuth.onUnauth(request)
        }

      // Не возвращаем 404 для защиты от возможных (бессмысленных) сканов.
      // None означает что или магазина нет, или ТЦ у магазина не указан (удалённый магазин, интернет-магазин).
      case None => IsAuth.onUnauth(request)
    }
  }
}


// Реквесты
abstract class AbstractRequestForMartAdm[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def mmart: MAdnNode
  def martId: String = mmart.id.get
}
case class RequestForMartAdm[A](mmart: MAdnNode, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForMartAdm(request)

case class RequestForMartShopAdm[A](mshop: MAdnNode, mmart: MAdnNode, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForMartAdm(request)



// Модерирование чужих рекламных карточек

case class MartShopAdRequest[A](ad: MAd, mmart: MAdnNode, pwOpt: PwOpt_t, request : Request[A], sioReqMd: SioReqMd)
  extends AbstractRequestWithPwOpt(request)

case class IsMartAdminShopAd(adId: String) extends ActionBuilder[MartShopAdRequest] {
  protected def invokeBlock[A](request: Request[A], block: (MartShopAdRequest[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    // Для экшенов модерации обычно (пока что) не требуется bill-контекста, поэтому делаем srm по-простому.
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    MAd.getById(adId) flatMap {
      case Some(ad) =>
        Future.traverse(ad.receivers.valuesIterator) { adRcvr =>
          isMartAdmin(adRcvr.receiverId, pwOpt)
        } flatMap { results =>
          results.find(_.isDefined).flatten match {
            case Some(mmart) =>
              srmFut flatMap { srm =>
                val req1 = MartShopAdRequest(ad, mmart, pwOpt, request, srm)
                block(req1)
              }
            case None => IsAuth.onUnauth(request)
          }
        }

      case _ => IsAuth.onUnauth(request)
    }
  }
}

