package util.acl

import controllers.SioController
import io.suggest.di.{IExecutionContext, IEsClient}
import models.req.SioReqMd
import util.di.{IErrorHandler, INodeCache}
import scala.concurrent.Future
import play.api.mvc.{RequestHeader, Request, ActionBuilder, Result}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:48
 * Description: Аддон для контроллеров с гибридом IsSuperuser + MAdnNode.getById().
 */
trait IsSuperuserAdnNode
  extends SioController
  with IEsClient
  with IExecutionContext
  with IsSuperuserUtilCtl
  with INodeCache
  with IErrorHandler
{

  /** Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin. */
  sealed trait IsSuperuserAdnNodeBase
    extends ActionBuilder[RequestForAdnNodeAdm]
    with IsSuperuserUtil
  {

    /** id запрашиваемого узла. */
    def adnId: String

    override def invokeBlock[A](request: Request[A], block: (RequestForAdnNodeAdm[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      if (PersonWrapper.isSuperuser(pwOpt)) {
        val mnodeOptFut = mNodeCache.getById(adnId)
        val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
        mnodeOptFut flatMap {
          case Some(mnode) =>
            sioReqMdFut flatMap { srm =>
              block(RequestForAdnNodeAdm(mnode, isMyNode = true, request, pwOpt, srm))
            }
          case None =>
            nodeNotFound(request)
        }

      } else {
        supOnUnauthFut(request, pwOpt)
      }
    }

    def nodeNotFound(implicit request: RequestHeader): Future[Result] = {
      errorHandler.http404Fut
    }

  }

  sealed abstract class IsSuperuserAdnNodeBase2
    extends IsSuperuserAdnNodeBase
    with ExpireSession[RequestForAdnNodeAdm]

  /**
   * Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin.
   * @param adnId id рекламного узла, которым интересуется суперпользователь.
   */
  case class IsSuperuserAdnNode(override val adnId: String)
    extends IsSuperuserAdnNodeBase2

  /** Аналог [[IsSuperuserAdnNode]] с выставлением CSRF-токена. */
  case class IsSuperuserAdnNodeGet(override val adnId: String)
    extends IsSuperuserAdnNodeBase2
    with CsrfGet[RequestForAdnNodeAdm]

  /** Аналог [[IsSuperuserAdnNode]] с проверкой CSRF-токена при сабмитах. */
  case class IsSuperuserAdnNodePost(override val adnId: String)
    extends IsSuperuserAdnNodeBase2
    with CsrfPost[RequestForAdnNodeAdm]

}
