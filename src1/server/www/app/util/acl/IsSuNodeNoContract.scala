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
final class IsSuNodeNoContract @Inject() (
                                           aclUtil    : AclUtil,
                                           reqUtil    : ReqUtil,
                                           mCommonDi  : ICommonDi
                                         )
  extends MacroLogsImpl
{

  import mCommonDi.{ec, errorHandler}
  import mCommonDi.current.injector


  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val isSu = injector.instanceOf[IsSu]

  /**
    * @param nodeId id запрашиваемого узла.
    */
  def apply(nodeId: String): ActionBuilder[MNodeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        def reqErr = MReq(request, user)

        if (user.isSuper) {
          import esModel.api._

          mNodes.getByIdCache(nodeId).flatMap {
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

