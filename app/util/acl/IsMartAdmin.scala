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
object IsMartAdmin extends PlayMacroLogsImpl {
  import LOGGER._

  def isMartAdmin(martId: String, pwOpt: PwOpt_t): Future[Option[MAdnNode]] = {
    MAdnNodeCache.getByIdCached(martId) map { mmartOpt =>
      mmartOpt flatMap { mmart =>
        val isAllowed = PersonWrapper.isSuperuser(pwOpt) || {
          pwOpt.isDefined && (mmart.personIds contains pwOpt.get.personId)
        }
        if (isAllowed) {
          Some(mmart)
        } else {
          None
        }
      }
    }
  }

  /** Код вызова проверки martID и выполнения того или иного экшена. */
  def invokeMartBlock[A](martId: String, request: Request[A], block: (AbstractRequestForMartAdm[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    isMartAdmin(martId, pwOpt) flatMap {
      case Some(mmart) =>
        srmFut flatMap { srm =>
          val req1 = RequestForMartAdm(mmart, request, pwOpt, srm)
          block(req1)
        }

      case None =>
        IsAuth.onUnauth(request)
    }
  }

}

import IsMartAdmin._


/** Административная операция над торговым центром. */
case class IsMartAdmin(martId: String) extends ActionBuilder[AbstractRequestForMartAdm] {
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestForMartAdm[A]) => Future[Result]): Future[Result] = {
    invokeMartBlock(martId, request, block)
  }
}

/** Какая-то административная операция над магазином, подразумевающая права на ТЦ. */
case class IsMartAdminShop(shopId: String) extends ActionBuilder[AbstractRequestForMartAdm] {
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestForMartAdm[A]) => Future[Result]): Future[Result] = {
    MAdnNodeCache.getByIdCached(shopId) flatMap {
      case Some(mshop) if mshop.adnMemberInfo.supId.isDefined =>
        invokeMartBlock(mshop.adnMemberInfo.supId.get, request, block)

      // Не возвращаем 404 для защиты от возможных (бессмысленных) сканов.
      // None означает что или магазина нет, или ТЦ у магазина не указан (удалённый магазин, интернет-магазин).
      case None => IsAuth.onUnauth(request)
    }
  }
}

abstract class AbstractRequestForMartAdm[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def martId: String
  def mmart: MAdnNode
}
case class RequestForMartAdm[A](mmart: MAdnNode, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForMartAdm(request) {
  def martId: String = mmart.id.get
}


case class ShopMartAdRequest[A](ad: MAd, mmart: MAdnNode, pwOpt: PwOpt_t, request : Request[A], sioReqMd: SioReqMd)
  extends AbstractRequestWithPwOpt(request)

case class IsMartAdminShopAd(adId: String) extends ActionBuilder[ShopMartAdRequest] {
  protected def invokeBlock[A](request: Request[A], block: (ShopMartAdRequest[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOpt(pwOpt)
    MAd.getById(adId) flatMap {
      case Some(ad) =>
        Future.traverse(ad.receivers) { adRcvr =>
          isMartAdmin(adRcvr.receiverId, pwOpt)
        } flatMap { results =>
          results.find(_.isDefined).flatten match {
            case Some(mmart) =>
            srmFut flatMap { srm =>
              val req1 = ShopMartAdRequest(ad, mmart, pwOpt, request, srm)
              block(req1)
            }
            case None => IsAuth.onUnauth(request)
          }
        }

      case _ => IsAuth.onUnauth(request)
    }
  }
}

