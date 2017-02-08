package util.acl

import com.google.inject.{Inject, Singleton}
import models.mproj.ICommonDi
import models.req.{IReqHdr, MNodeReq, MReq}

import scala.concurrent.Future
import play.api.mvc.{ActionBuilder, Request, Result}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:48
 * Description: Аддон для контроллеров с гибридом IsSuperuser + MAdnNode.getById().
 *
 * 2015.dec.23: Изначально назывался IsSuperuserAdnNode и обслуживал только MAdnNode.
 */
@Singleton
class IsSuNode @Inject() (
                           val csrf   : Csrf,
                           isSu       : IsSu,
                           mCommonDi  : ICommonDi
                         ) {

  import mCommonDi._

  /** Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin. */
  sealed trait IsSuNodeBase
    extends ActionBuilder[MNodeReq]
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
        isSu.supOnUnauthFut(req1)
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
  def apply(nodeId: String) = IsSuNode(nodeId)

  /** Аналог [[IsSuNode]] с выставлением CSRF-токена. */
  case class Get(override val nodeId: String)
    extends IsSuNodeBase2
    with csrf.Get[MNodeReq]

  /** Аналог [[IsSuNode]] с проверкой CSRF-токена при сабмитах. */
  case class Post(override val nodeId: String)
    extends IsSuNodeBase2
    with csrf.Post[MNodeReq]

}

trait IIsSuNodeDi {
  val isSuNode: IsSuNode
}
