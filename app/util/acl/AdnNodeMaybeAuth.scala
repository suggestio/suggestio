package util.acl

import models.MNode
import models.mproj.IMCommonDi
import models.req.MNodeReq
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
      val mnodeOptFut = mNodeCache.getById(nodeId)

      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      mnodeOptFut.flatMap {
        case Some(mnode) =>
          if (isNodeValid(mnode)) {
            val req1 = MNodeReq(mnode, request, user)
            block(req1)
          } else {
            accessProhibited(mnode, request)
          }

        case None =>
          nodeNotFound(request)
      }
    }

    def isNodeValid(adnNode: MNode): Boolean

    def accessProhibited[A](adnNode: MNode, request: Request[A]): Future[Result] = {
      LOGGER.warn(s"Failed access to acl-prohibited node: ${adnNode.id.get} (${adnNode.meta.basic.name}) :: Returning 404 to ${request.remoteAddress}")
      nodeNotFound(request)
    }

    def nodeNotFound(implicit request: Request[_]): Future[Result] = {
      LOGGER.warn(s"Node $nodeId not found, requested by ${request.remoteAddress}")
      errorHandler.http404Fut
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
