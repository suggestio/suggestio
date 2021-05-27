package util.acl

import controllers.routes
import io.suggest.err.HttpResultingException, HttpResultingException._
import io.suggest.es.model.EsModel
import io.suggest.flash.FlashConstants
import io.suggest.n2.edge.MEdge
import io.suggest.n2.edge.edit.MNodeEdgeIdQs
import io.suggest.n2.node.MNodes
import javax.inject.Inject
import models.mproj.ICommonDi
import models.req._
import play.api.mvc._
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import play.api.http.Status

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.10.16 16:26
  * Description: ACL-аддон для sys-контроллеров для экшенов управления эджами.
  */
final class IsSuNodeEdge @Inject() (
                                     reqUtil    : ReqUtil,
                                     mCommonDi  : ICommonDi
                                   )
  extends MacroLogsImpl
{

  import mCommonDi.{ec, errorHandler}
  import mCommonDi.current.injector

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val isSu = injector.instanceOf[IsSu]


  /** Комбинация IsSuperuser + IsAdnAdmin + доступ к эджу по индексу.
    *
    * @param qs query string.
    * @param noEdgeIdOk Разрешить ситуацию, когда эдж не задан (т.е. подразумевается возможность создание нового эджа в экшене).
    */
  def apply(qs: MNodeEdgeIdQs, noEdgeIdOk: Boolean = false, canRdr: Boolean = false): ActionBuilder[MNodeEdgeOptReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeEdgeOptReq] {

      protected[this] def logPrefix = s"${getClass.getSimpleName}(${qs.nodeId}/v=${qs.nodeVsn}/e=${qs.edgeId}):"

      override def invokeBlock[A](request: Request[A], block: (MNodeEdgeOptReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        if (user.isSuper) {
          import esModel.api._

          def req1 = MReq(request, user)

          (for {
            // Поискать запрашиваемый узел:
            mnodeOpt <- mNodes.getByIdCache(qs.nodeId)
            mnode = mnodeOpt getOrElse {
              LOGGER.trace(s"$logPrefix Node#${qs.nodeId} not found")
              throw HttpResultingException( nodeNotFound(req1) )
            }

            nodeReq = () => MNodeReq(mnode, request, user)

            // Сверить es-версию узла:
            if {
              val r = mnode.versioning.version contains[Long] qs.nodeVsn
              if (!r) {
                LOGGER.debug(s"$logPrefix node version invalid. Expected=${qs.nodeVsn}, real=${mnode.versioning}")
                throw HttpResultingException( nodeVsnInvalid(nodeReq()) )
              } else {
                r
              }
            }

            // Если edgeId не задан, и !noEdgeIdOk, то прервать исполнение.
            if {
              val r = qs.edgeId.isDefined || noEdgeIdOk
              if (!r) {
                LOGGER.debug(s"$logPrefix EdgeId is None, but should be defined")
                val result =
                  if (canRdr) rdrToSysNode( qs.nodeId )
                  else edgeNotFound( nodeReq() )
                throw HttpResultingException( result )
              } else {
                r
              }
            }

            // Если edgeId задан, то извлечь и проверить это.
            edgeOpt: Option[MEdge] = qs.edgeId.flatMap { edgeId =>
              val edgeOpt2 = mnode.edges.withIndex( edgeId )
              if (edgeOpt2.isEmpty) {
                LOGGER.debug(s"$logPrefix Edge index#$edgeId missing.")
                throw HttpResultingException( edgeNotFound(nodeReq()) )
              } else {
                edgeOpt2
              }
            }

            // Исполнение реквеста:
            req1 = MNodeEdgeOptReq(mnode, edgeOpt, request, user)
            res <- block( req1 )
          } yield {
            res
          })
            .recoverHttpResEx

        } else {
          val req1 = MReq(request, user)
          isSu.supOnUnauthFut(req1)
        }
      }

      /** Узел не найден. */
      def nodeNotFound(req: IReqHdr): Future[Result] =
        errorHandler.onClientError(req, Status.NOT_FOUND)

      /** Запрошенная версия узла неактуальна. */
      def nodeVsnInvalid(req: INodeReq[_]): Future[Result] = {
        // Отредиректить суперюзера в админку, сообщив о проблеме через FLASH.
        // Так отрабатываем переход в редактор эджа через историю браузера по устаревшей ссылке.
        val res = Results.Redirect( routes.SysMarket.showAdnNode( qs.nodeId ) )
          .flashing( FlashConstants.Statuses.ERROR -> "Edge version conflict." )
        Future.successful(res)
      }

      /** Не найден эдж с указанным id. */
      def edgeNotFound(req: INodeReq[_]): Future[Result] =
        errorHandler.onClientError(req, Status.NOT_FOUND, s"Node ${qs.nodeId} does NOT have edge #${qs.edgeId}.")

      def rdrToSysNode(nodeId: String): Future[Result] =
        Future.successful( Results.Redirect( routes.SysMarket.showAdnNode(qs.nodeId) ) )

    }
  }

}
