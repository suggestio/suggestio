package util.acl

import play.api.mvc.{SimpleResult, Request, ActionBuilder}
import scala.concurrent.Future
import util.acl.PersonWrapper.PwOpt_t
import models.MShop, MShop.ShopId_t
import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits._
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.02.14 18:38
 * Description: Проверка прав на управление магазином, находящимся внутри ТЦ.
 */
object IsMartShopAdmin extends PlayMacroLogsImpl {
  import LOGGER._

  /**
   * Асинхронная проверка прав на управление магазином.
   * @param shopId Магазин.
   * @param pwOpt Юзер.
   * @return Фьючерс, т.к. модели магазинов будут перегнаны на асинхронных ES.
   */
  def isShopAdmin(shopId: ShopId_t, pwOpt: PwOpt_t): Future[Boolean] = {
    if (PersonWrapper isSuperuser pwOpt) {
      Future successful true
    } else {
      if (pwOpt.isDefined) {
        // Нужно узнать, существует ли магазин и TODO есть ли права у юзера на магазин
        warn("Check ACL for shop: NOT YET IMPLEMENTED. Allowing " + shopId + " for " + pwOpt)
        MShop.isExist(shopId)
      } else {
        Future successful false
      }
    }
  }

  /**
   * Вернуть магазин, если с правами всё ок. Иначе None.
   * @param shopId id магазина.
   * @param pwOpt Текущий юзер.
   * @return Option[MShop].
   */
  def isShopAdminFull(shopId: ShopId_t, pwOpt: PwOpt_t): Future[Option[MShop]] = {
    if (PersonWrapper isSuperuser pwOpt) {
      MShop getById shopId
    } else {
      if (pwOpt.isDefined) {
        // Нужно узнать, существует ли магазин и TODO есть ли права у юзера на магазин
        warn("Check ACL for shop: NOT YET IMPLEMENTED. Allowing " + shopId + " for " + pwOpt)
        MShop getById shopId
      } else {
        Future successful None
      }
    }
  }

}

import IsMartShopAdmin._

case class IsMartShopAdmin(shopId: ShopId_t) extends ActionBuilder[AbstractRequestForShopAdm] {
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestForShopAdm[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    isShopAdmin(shopId, pwOpt) flatMap {
      case true =>
        val req1 = RequestForShopAdm(shopId, pwOpt, request)
        block(req1)

      case false => IsAuth.onUnauth(request)
    }
  }
}


/** В реквесте содержится магазин, если всё ок. */
case class IsShopAdmFull(shopId: ShopId_t) extends ActionBuilder[RequestForShopAdmFull] {
  protected def invokeBlock[A](request: Request[A], block: (RequestForShopAdmFull[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    isShopAdminFull(shopId, pwOpt) flatMap {
      case Some(mshop) =>
        val req1 = RequestForShopAdmFull(mshop, request, pwOpt)
        block(req1)

      case None => IsAuth onUnauth request
    }
  }
}

case class RequestForShopAdmFull[A](mshop: MShop, request: Request[A], pwOpt: PwOpt_t) extends AbstractRequestForShopAdm(request) {
  def shopId: ShopId_t = mshop.id.get
}