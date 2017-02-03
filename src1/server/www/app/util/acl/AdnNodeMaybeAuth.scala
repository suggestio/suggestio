package util.acl

import models.MNode
import models.mproj.IMCommonDi
import models.req.{IReqHdr, INodeReq, MReq, MNodeReq}
import play.api.mvc.{ActionBuilder, Request, Result}
import util.{PlayMacroLogsDyn, PlayMacroLogsI}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 18:49
 * Description: Смесь принципов MaybeAuth и считывания указанного узла.
 */
trait AdnNodeMaybeAuth
  extends IMCommonDi
{

  import mCommonDi._

  /** Общий код проверок типа AdnNodeMaybeAuth. */
  sealed trait AdnNodeMaybeAuthBase
    extends ActionBuilder[MNodeReq]
    with PlayMacroLogsI
  {

    /** id запрашиваемого узла. */
    def nodeId: String

    override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
      val mnodeOptFut = mNodesCache.getById(nodeId)

      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      mnodeOptFut.flatMap {
        case Some(mnode) =>
          val req1 = MNodeReq(mnode, request, user)
          if (isNodeValid(mnode)) {
            block(req1)
          } else {
            accessProhibited(req1)
          }

        case None =>
          val req1 = MReq(request, user)
          nodeNotFound(req1)
      }
    }

    def isNodeValid(adnNode: MNode): Boolean

    def accessProhibited[A](req: INodeReq[_]): Future[Result] = {
      LOGGER.warn(s"Failed access to acl-prohibited node: ${req.mnode.id.get} (${req.mnode.meta.basic.name}) :: Returning 404 to ${req.remoteAddress}")
      nodeNotFound(req)
    }

    def nodeNotFound(req: IReqHdr): Future[Result] = {
      LOGGER.warn(s"Node $nodeId not found, requested by ${req.remoteAddress}")
      errorHandler.http404Fut(req)
    }
  }

  /** Промежуточный трейт из-за использования в ExpireSession модификатора abstract override. */
  sealed abstract class AdnNodeMaybeAuthAbstract
    extends AdnNodeMaybeAuthBase
    with ExpireSession[MNodeReq]
    with PlayMacroLogsDyn


  /**
   * Реализация [[AdnNodeMaybeAuthBase]] с поддержкой таймаута сессии.
   * @param nodeId id узла.
   */
  case class AdnNodeMaybeAuth(override val nodeId: String)
    extends AdnNodeMaybeAuthAbstract
  {
    override def isNodeValid(mnode: MNode): Boolean = true
  }

}
