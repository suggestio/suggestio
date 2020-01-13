package util.acl

import io.suggest.es.model.EsModel
import io.suggest.n2.node.MNodes
import javax.inject.Inject
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi
import models.req.{MNodeReq, MReq}
import play.api.http.Status
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 12:15
 * Description: Доступ суперюзера к узлу, обязательно БЕЗ контракта.
 */
class IsSuNodeNoContract @Inject() (
                                     esModel    : EsModel,
                                     mNodes     : MNodes,
                                     aclUtil    : AclUtil,
                                     isSu       : IsSu,
                                     reqUtil    : ReqUtil,
                                     mCommonDi  : ICommonDi
                                   )
  extends MacroLogsImpl
{

  import mCommonDi._
  import esModel.api._

  /**
    * @param nodeId id запрашиваемого узла.
    */
  def apply(nodeId: String): ActionBuilder[MNodeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        def reqErr = MReq(request, user)

        if (user.isSuper) {
          val mnodeOptFut = mNodes.getByIdCache(nodeId)
          mnodeOptFut.flatMap {
            case Some(mnode) =>
              if (mnode.billing.contractId.isEmpty) {
                val req1 = MNodeReq(mnode, request, user)
                block(req1)
              } else {
                val msg = s"Node#$nodeId has contract#${mnode.billing.contractId.orNull}, but must not."
                errorHandler.onClientError(reqErr, Status.NOT_FOUND, msg)
              }

            case None =>
              val msg = s"Node#$nodeId is missing"
              LOGGER.debug(msg)
              errorHandler.onClientError(reqErr, Status.NOT_FOUND, msg)
          }

        } else {
          isSu.supOnUnauthFut(reqErr)
        }
      }

    }
  }

}

