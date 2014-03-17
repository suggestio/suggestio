package util.acl

import io.suggest.ym.model.MMart.MartId_t
import util.acl.PersonWrapper.PwOpt_t
import util.PlayMacroLogsImpl
import scala.concurrent.Future
import play.api.mvc.{RequestHeader, SimpleResult, Request, ActionBuilder}
import io.suggest.ym.model.MShop, MShop.ShopId_t
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

  def isMartAdmin(martId: MartId_t, pwOpt: PwOpt_t): Future[Option[MMart]] = {
    MMart.getById(martId) map { mmartOpt =>
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
  def invokeMartBlock[A](martId: MartId_t, request: Request[A], block: (AbstractRequestForMartAdm[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    isMartAdmin(martId, pwOpt) flatMap {
      case Some(mmart) =>
        val req1 = RequestForMartAdm(mmart, request, pwOpt)
        block(req1)

      case None =>
        IsAuth.onUnauth(request)
    }
  }

}

import IsMartAdmin._


/** Административная операция над торговым центром. */
case class IsMartAdmin(martId: MartId_t) extends ActionBuilder[AbstractRequestForMartAdm] {
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestForMartAdm[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    invokeMartBlock(martId, request, block)
  }
}

/** Какая-то административная операция над магазином, подразумевающая права на ТЦ. */
case class IsMartAdminShop(shopId: ShopId_t) extends ActionBuilder[AbstractRequestForMartAdm] {
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestForMartAdm[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    MShop.getMartIdFor(shopId) flatMap {
      case Some(martId) =>
        invokeMartBlock(martId, request, block)

      // Не возвращаем 404 для защиты от возможных (бессмысленных) сканов.
      // None означает что или магазина нет, или ТЦ у магазина не указан (удалённый магазин, интернет-магазин).
      case None => IsAuth.onUnauth(request)
    }
  }
}

abstract class AbstractRequestForMartAdm[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def martId: MartId_t
  def mmart: MMart
}
case class RequestForMartAdm[A](mmart: MMart, request: Request[A], pwOpt: PwOpt_t)
  extends AbstractRequestForMartAdm(request) {
  def martId: MartId_t = mmart.id.get
}


case class ShopMartAdRequest[A](ad: MMartAd, mmart:MMart, pwOpt: PwOpt_t, request : Request[A]) extends AbstractRequestWithPwOpt(request)

case class IsMartAdminShopAd(adId: String) extends ActionBuilder[ShopMartAdRequest] {
  protected def invokeBlock[A](request: Request[A], block: (ShopMartAdRequest[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    MMartAd.getById(adId) flatMap {
      case Some(ad) =>
        val pwOpt = PersonWrapper.getFromRequest(request)
        isMartAdmin(ad.martId, pwOpt) flatMap {
          case Some(mmart) =>
            val req1 = ShopMartAdRequest(ad, mmart, pwOpt, request)
            block(req1)

          case None => IsAuth.onUnauth(request)
        }

      case _ => IsAuth.onUnauth(request)
    }
  }
}

