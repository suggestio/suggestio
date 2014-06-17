package util.acl

import util.acl.PersonWrapper.PwOpt_t
import util.PlayMacroLogsImpl
import scala.concurrent.Future
import play.api.mvc.{Result, Request, ActionBuilder}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import models._
import IsAdnNodeAdmin.onUnauth

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.14 13:58
 * Description: Проверка прав на управление ТЦ.
 */
object IsMartAdmin extends controllers.ShopMartCompat {

  def isMartAdmin(martId: String, pwOpt: PwOpt_t): Future[Option[MAdnNode]] = {
    val fut = getMartByIdCache(martId)
    IsAdnNodeAdmin.checkAdnNodeCredsOpt(fut, pwOpt)
  }

}

import IsMartAdmin._


/** Административная операция над торговым центром. */
trait IsMartAdminBase extends ActionBuilder[AbstractRequestForMartAdm] {
  def martId: String
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
        onUnauth(request)
    }
  }
}
case class IsMartAdmin(martId: String)
  extends IsMartAdminBase
  with ExpireSession[AbstractRequestForMartAdm]


/** Какая-то административная операция над магазином, подразумевающая права на ТЦ. */
trait IsMartAdminShopBase extends ActionBuilder[RequestForMartShopAdm] {
  def shopId: String
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
          onUnauth(request)
        }

      // Не возвращаем 404 для защиты от возможных (бессмысленных) сканов.
      // None означает что или магазина нет, или ТЦ у магазина не указан (удалённый магазин, интернет-магазин).
      case None => onUnauth(request)
    }
  }
}
case class IsMartAdminShop(shopId: String)
  extends IsMartAdminShopBase
  with ExpireSession[RequestForMartShopAdm]


// Реквесты
abstract class AbstractRequestForMartAdm[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def mmart: MAdnNode
  def martId: String = mmart.id.get
}
case class RequestForMartAdm[A](mmart: MAdnNode, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForMartAdm(request)

case class RequestForMartShopAdm[A](mshop: MAdnNode, mmart: MAdnNode, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForMartAdm(request)

