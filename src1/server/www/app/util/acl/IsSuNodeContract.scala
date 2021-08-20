package util.acl

import javax.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.EsModel
import io.suggest.mbill2.m.contract.MContracts
import io.suggest.model.SlickHolder
import io.suggest.n2.node.MNodes
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.req.{MNodeContractReq, MReq}
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 11:59
 * Description: Доступ к узлу и его контракту для суперюзера.
 */
final class IsSuNodeContract @Inject() (
                                         injector     : Injector,
                                         aclUtil      : AclUtil,
                                         reqUtil      : ReqUtil,
                                       )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val mContracts = injector.instanceOf[MContracts]
  private lazy val isSu = injector.instanceOf[IsSu]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]
  private lazy val slickHolder = injector.instanceOf[SlickHolder]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  import slickHolder.slick

  /** Доступ к узлу с контрактом.
    * @param nodeId id запрашиваемого узла.
    */
  def apply(nodeId: String): ActionBuilder[MNodeContractReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeContractReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeContractReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        def reqErr = MReq(request, user)

        if (user.isSuper) {
          import esModel.api._

          val mnodeOptFut = mNodes.getByIdCache(nodeId)
          mnodeOptFut.flatMap {
            case Some(mnode) =>
              val countractIOpt = mnode.billing.contractId
              val mcOptFut = FutureUtil.optFut2futOpt(countractIOpt) { contractId =>
                val act = mContracts.getById(contractId)
                slick.db.run(act)
              }

              mcOptFut.flatMap {
                case Some(mc) =>
                  val req1 = MNodeContractReq(mnode, mc, request, user)
                  block(req1)

                case None =>
                  val msg = s"Countract#${countractIOpt.orNull} not found"
                  LOGGER.debug(msg)
                  errorHandler.onClientError(reqErr, Status.NOT_FOUND, msg)
              }

            case None =>
              val msg = s"Node#${nodeId} not found"
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
