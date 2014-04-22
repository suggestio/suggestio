package util.acl

import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import util.acl.PersonWrapper.PwOpt_t
import play.api.mvc._
import util.SiowebEsUtil.client
import controllers.routes
import play.api.mvc.Result
import scala.Some

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 18:35
 * Description: Проверка прав на управление абстрактным узлом рекламной сети.
 */
object IsAdnNodeAdmin {

  /** Что делать, когда юзер не авторизован, но долбится в ЛК? */
  def onUnauth(req: RequestHeader): Future[Result] = {
    Future.successful(
      Results.Redirect(routes.MarketLk.lkIndex())
    )
  }


  def checkAdnNodeCreds(adnNodeOptFut: Future[Option[MAdnNode]], pwOpt: PwOpt_t): Future[Option[MAdnNode]] = {
    adnNodeOptFut map { adnNodeOpt =>
      adnNodeOpt flatMap { adnNode =>
        val isAllowed = PersonWrapper.isSuperuser(pwOpt) || {
          pwOpt.isDefined && (adnNode.personIds contains pwOpt.get.personId)
        }
        if (isAllowed) {
          Some(adnNode)
        } else {
          None
        }
      }
    }
  }

  def isAdnNodeAdmin(adnId: String, pwOpt: PwOpt_t): Future[Option[MAdnNode]] = {
    val fut = MAdnNodeCache.getByIdCached(adnId)
    checkAdnNodeCreds(fut, pwOpt)
  }

}


import IsAdnNodeAdmin.onUnauth

/** В реквесте содержится магазин, если всё ок. */
case class IsAdnNodeAdmin(adnId: String) extends ActionBuilder[RequestForShopAdmFull] {
  protected def invokeBlock[A](request: Request[A], block: (RequestForShopAdmFull[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOptAdn(pwOpt, adnId)
    IsAdnNodeAdmin.isAdnNodeAdmin(adnId, pwOpt) flatMap {
      case Some(adnNode) =>
        srmFut flatMap { srm =>
          val req1 = RequestForShopAdmFull(adnNode, request, pwOpt, srm)
          block(req1)
        }

      case _ => onUnauth(request)
    }
  }
}



abstract class AbstractRequestForAdnNodeAdm[A](request: Request[A])
  extends AbstractRequestWithPwOpt(request) {
  def adnNode: MAdnNode
}

case class RequestForAdnNodeAdm[A](adnNode: MAdnNode, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForAdnNodeAdm(request)
