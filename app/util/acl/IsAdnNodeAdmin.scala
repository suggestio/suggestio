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


  def isAdnNodeAdminCheck(adnNode: MAdnNode, pwOpt: PwOpt_t): Boolean = {
    PersonWrapper.isSuperuser(pwOpt) || {
      pwOpt.exists { pw =>
        adnNode.personIds contains pw.personId
      }
    }
  }

  def checkAdnNodeCreds(adnNodeOptFut: Future[Option[MAdnNode]], pwOpt: PwOpt_t): Future[Either[Option[MAdnNode], MAdnNode]] = {
    adnNodeOptFut map { adnNodeOpt =>
      adnNodeOpt.fold [Either[Option[MAdnNode], MAdnNode]] (Left(None)) { adnNode =>
        val isAllowed = isAdnNodeAdminCheck(adnNode, pwOpt)
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
          val req1 = RequestForAdnNodeAdm(adnNode, isMyNode = true, request, pwOpt, srm)
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

case class RequestForAdnNodeAdm[A](adnNode: MAdnNode, isMyNode: Boolean, request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForAdnNode(request)


/**
 * Доступ к узлу, к которому НЕ обязательно есть права на админство.
 * @param adnId узел.
 */
case class AdnNodeAccess(adnId: String, povAdnIdOpt: Option[String]) extends ActionBuilder[RequestForAdnNode] {
  override protected def invokeBlock[A](request: Request[A], block: (RequestForAdnNode[A]) => Future[Result]): Future[Result] = {
    PersonWrapper.getFromRequest(request) match {
      case pwOpt @ Some(pw) =>
        val povAdnNodeOptFut = povAdnIdOpt.fold
          { Future successful Option.empty[MAdnNode] }
          { povAdnId => IsAdnNodeAdmin.isAdnNodeAdmin(povAdnId, pwOpt) }
        val adnNodeOptFut = MAdnNodeCache.getByIdCached(adnId)
        IsAdnNodeAdmin.checkAdnNodeCreds(adnNodeOptFut, pwOpt) flatMap {
          // Это админ текущего узла
          case Right(adnNode) =>
            SioReqMd.fromPwOptAdn(pwOpt, adnId) flatMap { srm =>
              povAdnNodeOptFut flatMap { povAdnNodeOpt =>
                val req1 = RequestForAdnNode(adnNode, povAdnNodeOpt, isMyNode = true, request, pwOpt, srm)
                block(req1)
              }
            }

          // Узел существует, но он не относится к текущему залогиненному юзеру. Это узел третьего лица, рекламодателя в частности.
          case Left(Some(adnNode)) =>
            povAdnNodeOptFut flatMap { povAdnNodeOpt =>
              val srmFut = povAdnNodeOpt match {
                // Это админ pov-узла подглядывает к рекламодателю или какому-то иному узлу.
                case Some(povAdnNode) =>
                  SioReqMd.fromPwOptAdn(pwOpt, povAdnNode.id.get)
                // Это кто-то иной косит под админа внешнего узела, скорее всего получил ссылку на ноду от другого человека.
                case None =>
                  SioReqMd.fromPwOpt(pwOpt)
              }
              srmFut flatMap { srm =>
                val req1 = RequestForAdnNode(adnNode, povAdnNodeOpt, isMyNode = false, request, pwOpt, srm)
                block(req1)
              }
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

case class RequestForAdnNode[A](adnNode: MAdnNode, povAdnNodeOpt: Option[MAdnNode], isMyNode: Boolean,
                                request: Request[A], pwOpt: PwOpt_t, sioReqMd: SioReqMd)
  extends AbstractRequestForAdnNode(request)
