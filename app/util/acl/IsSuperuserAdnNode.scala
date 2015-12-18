package util.acl

import controllers.SioController
import models.req.{SioReq, MNodeReq}
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
  with IsSuperuserUtilCtl
  with Csrf
{

  import mCommonDi._

  /** Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin. */
  sealed trait IsSuperuserAdnNodeBase
    extends ActionBuilder[MNodeReq]
    with IsSuperuserUtil
  {

    /** id запрашиваемого узла. */
    def adnId: String

    override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)
      if (user.isSuperUser) {
        val mnodeOptFut = mNodeCache.getById(adnId)
        mnodeOptFut flatMap {
          case Some(mnode) =>
            val req1 = MNodeReq(mnode, request, user)
            block(req1)

          case None =>
            nodeNotFound(request)
        }

      } else {
        val req1 = SioReq(request, user)
        supOnUnauthFut(req1)
      }
    }

    def nodeNotFound(implicit request: RequestHeader): Future[Result] = {
      errorHandler.http404Fut
    }

  }

  sealed abstract class IsSuperuserAdnNodeBase2
    extends IsSuperuserAdnNodeBase
    with ExpireSession[MNodeReq]

  /**
   * Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin.
   * @param adnId id рекламного узла, которым интересуется суперпользователь.
   */
  case class IsSuperuserAdnNode(override val adnId: String)
    extends IsSuperuserAdnNodeBase2

  /** Аналог [[IsSuperuserAdnNode]] с выставлением CSRF-токена. */
  case class IsSuperuserAdnNodeGet(override val adnId: String)
    extends IsSuperuserAdnNodeBase2
    with CsrfGet[MNodeReq]

  /** Аналог [[IsSuperuserAdnNode]] с проверкой CSRF-токена при сабмитах. */
  case class IsSuperuserAdnNodePost(override val adnId: String)
    extends IsSuperuserAdnNodeBase2
    with CsrfPost[MNodeReq]

}
