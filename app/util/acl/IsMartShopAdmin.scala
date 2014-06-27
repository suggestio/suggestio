package util.acl

import play.api.mvc.{Result, Request, ActionBuilder}
import scala.concurrent.Future
import util.acl.PersonWrapper.PwOpt_t
import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client
import models._
import IsAdnNodeAdmin.onUnauth

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.14 18:38
 * Description: Проверка прав на управление магазином, находящимся внутри ТЦ.
 * Следует различать случаи, когда на магазин влияет владелец ТЦ, а когда - владелец самого магазина.
 */
object IsShopAdm extends controllers.ShopMartCompat {

  /**
   * Вернуть магазин, если с правами всё ок. Иначе None.
   * @param shopId id магазина.
   * @param pwOpt Текущий юзер.
   * @return Option с принадлежащим пользователю магазином или None.
   */
  def isShopAdminFull(shopId: String, pwOpt: PwOpt_t): Future[Option[MAdnNode]] = {
    val fut = getShopByIdCache(shopId)
    IsAdnNodeAdmin.checkAdnNodeCredsOpt(fut, pwOpt)
  }

}

import IsShopAdm._

/** В реквесте содержится магазин, если всё ок. */
trait IsShopAdmBase extends ActionBuilder[RequestForShopAdmFull] {
  def shopId: String
  override def invokeBlock[A](request: Request[A], block: (RequestForShopAdmFull[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOptAdn(pwOpt, shopId)
    isShopAdminFull(shopId, pwOpt) flatMap {
      case Some(mshop) =>
        srmFut flatMap { srm =>
          val req1 = RequestForShopAdmFull(mshop, request, pwOpt, srm)
          block(req1)
        }

      case _ => onUnauth(request)
    }
  }
}
case class IsShopAdm(shopId: String)
  extends IsShopAdmBase
  with ExpireSession[RequestForShopAdmFull]

case class RequestForShopAdmFull[A](mshop: MAdnNode, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForShopAdm(request) {
  def shopId: String = mshop.id.get
}
