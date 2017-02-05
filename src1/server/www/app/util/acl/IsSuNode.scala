package util.acl

import controllers.SioController
import models.req.{IReqHdr, MReq, MNodeReq}
import scala.concurrent.Future
import play.api.mvc.{Request, ActionBuilder, Result}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:48
 * Description: Аддон для контроллеров с гибридом IsSuperuser + MAdnNode.getById().
 *
 * 2015.dec.23: Изначально назывался IsSuperuserAdnNode и обслуживал только MAdnNode.
 */
trait IsSuNode
  extends SioController
  with Csrf
{

  import mCommonDi._

  /** Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin. */
  sealed trait IsSuNodeBase
    extends ActionBuilder[MNodeReq]
    with IsSuperuserUtil
  {

    /** id запрашиваемого узла. */
    def nodeId: String

    override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)
      if (user.isSuper) {
        val mnodeOptFut = mNodesCache.getById(nodeId)
        mnodeOptFut.flatMap {
          case Some(mnode) =>
            val req1 = MNodeReq(mnode, request, user)
            block(req1)

          case None =>
            val req1 = MReq(request, user)
            nodeNotFound(req1)
        }

      } else {
        val req1 = MReq(request, user)
        supOnUnauthFut(req1)
      }
    }

    def nodeNotFound(req: IReqHdr): Future[Result] = {
      errorHandler.http404Fut(req)
    }

  }

  sealed abstract class IsSuNodeBase2
    extends IsSuNodeBase
    with ExpireSession[MNodeReq]

  /**
   * Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin.
   * @param nodeId id рекламного узла, которым интересуется суперпользователь.
   */
  case class IsSuNode(override val nodeId: String)
    extends IsSuNodeBase2

  /** Аналог [[IsSuNode]] с выставлением CSRF-токена. */
  case class IsSuNodeGet(override val nodeId: String)
    extends IsSuNodeBase2
    with CsrfGet[MNodeReq]

  /** Аналог [[IsSuNode]] с проверкой CSRF-токена при сабмитах. */
  case class IsSuNodePost(override val nodeId: String)
    extends IsSuNodeBase2
    with CsrfPost[MNodeReq]

}
