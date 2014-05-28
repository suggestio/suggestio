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

  def checkAdnNodeCreds(adnNodeOptFut: Future[Option[MAdnNode]], pwOpt: PwOpt_t): Future[Either[Option[MAdnNode], MAdnNode]] = {
    adnNodeOptFut map { adnNodeOpt =>
      adnNodeOpt.fold [Either[Option[MAdnNode], MAdnNode]] (Left(None)) { adnNode =>
        val isAllowed = PersonWrapper.isSuperuser(pwOpt) || {
          pwOpt.isDefined && (adnNode.personIds contains pwOpt.get.personId)
        }
        if (isAllowed) {
          Right(adnNode)
        } else {
          Left(adnNodeOpt)
        }
      }
    }
  }

  def checkAdnNodeCredsOpt(adnNodeOptFut: Future[Option[MAdnNode]], pwOpt: PwOpt_t): Future[Option[MAdnNode]] = {
    checkAdnNodeCreds(adnNodeOptFut, pwOpt) map {
      case Right(adnNode) => Some(adnNode)
      case _ => None
    }
  }


  def isAdnNodeAdmin(adnId: String, pwOpt: PwOpt_t): Future[Option[MAdnNode]] = {
    val fut = MAdnNodeCache.getByIdCached(adnId)
    checkAdnNodeCredsOpt(fut, pwOpt)
  }

}


import IsAdnNodeAdmin.onUnauth

/** В реквесте содержится магазин, если всё ок. */
case class IsAdnNodeAdmin(adnId: String) extends ActionBuilder[AbstractRequestForAdnNode] {
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestForAdnNode[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val srmFut = SioReqMd.fromPwOptAdn(pwOpt, adnId)
    IsAdnNodeAdmin.isAdnNodeAdmin(adnId, pwOpt) flatMap {
      case Some(adnNode) =>
        srmFut flatMap { srm =>
          val req1 = RequestForAdnNode(adnNode, isMyNode = true, request, pwOpt, srm)
          block(req1)
        }

      case _ => onUnauth(request)
    }
  }
}



abstract class AbstractRequestForAdnNode[A](request: Request[A])
  extends AbstractRequestWithPwOpt(request) {
  def adnNode: MAdnNode
  def isMyNode: Boolean
}

case class RequestForAdnNode[A](adnNode: MAdnNode, isMyNode: Boolean, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForAdnNode(request)


/**
 * Доступ к узлу, к которому НЕ обязательно есть права на админство.
 * @param adnId узел.
 */
case class AdnNodeAccess(adnId: String) extends ActionBuilder[AbstractRequestForAdnNode] {
  override protected def invokeBlock[A](request: Request[A], block: (AbstractRequestForAdnNode[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        val adnNodeOptFut = MAdnNodeCache.getByIdCached(adnId)
        IsAdnNodeAdmin.checkAdnNodeCreds(adnNodeOptFut, pwOpt) flatMap {
          // Это админ узла
          case Right(adnNode) =>
            SioReqMd.fromPwOptAdn(pwOpt, adnId) flatMap { srm =>
              val req1 = RequestForAdnNode(adnNode, isMyNode = true, request, pwOpt, srm)
              block(req1)
            }

          // Узел существует, но он не относится к текущему залогиненному юзеру.
          case Left(Some(adnNode)) =>
            SioReqMd.fromPwOpt(pwOpt) flatMap { srm =>
              val req1 = RequestForAdnNode(adnNode, isMyNode = false, request, pwOpt, srm)
              block(req1)
            }

          // Узел не существует.
          case Left(None) =>
            Future successful Results.NotFound
        }

      // Отправить анонима на страницу логина.
      case None => IsAuth.onUnauth(request)
    }

  }
}
