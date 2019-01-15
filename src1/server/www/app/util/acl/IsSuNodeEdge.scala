package util.acl

import io.suggest.es.model.EsModel
import io.suggest.model.n2.node.MNodes
import javax.inject.Inject
import models.mproj.ICommonDi
import models.msys.MNodeEdgeIdQs
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
class IsSuNodeEdge @Inject() (
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


  /** Комбинация IsSuperuser + IsAdnAdmin + доступ к эджу по индексу.
    *
    * @param qs query string.
    */
  def apply(qs: MNodeEdgeIdQs): ActionBuilder[MNodeEdgeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeEdgeReq] {

      protected[this] def logPrefix = s"${getClass.getSimpleName}(${qs.nodeId}/v=${qs.nodeVsn}/e=${qs.edgeId}):"

      override def invokeBlock[A](request: Request[A], block: (MNodeEdgeReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        if (user.isSuper) {
          val mnodeOptFut = mNodes.getByIdCache(qs.nodeId)
          mnodeOptFut.flatMap {

            // Запрошенный узел найден.
            case Some(mnode) =>
              def nodeReq = MNodeReq(mnode, request, user)

              if (mnode.versionOpt.contains(qs.nodeVsn)) {
                mnode.edges
                  .withIndex(qs.edgeId)
                  .fold[Future[Result]] {
                    // Нет запрошенного эджа
                    edgeNotFound(nodeReq)
                  } { medge =>
                    val req1 = MNodeEdgeReq(mnode, medge, request, user)
                    block(req1)
                  }

              } else {
                // Узел был изменён до начала этого запроса. Порядок эджей может быть нарушен.
                LOGGER.debug(s"$logPrefix node version invalid. Expected=${qs.nodeVsn}, real=${mnode.versionOpt}")
                nodeVsnInvalid(nodeReq)
              }

            // Нет узла, почему-то.
            case None =>
              val req1 = MReq(request, user)
              nodeNotFound(req1)
          }

        } else {
          val req1 = MReq(request, user)
          isSu.supOnUnauthFut(req1)
        }
      }

      /** Узел не найден. */
      def nodeNotFound(req: IReqHdr): Future[Result] =
        errorHandler.onClientError(req, Status.NOT_FOUND)

      /** Запрошенная версия узла неактуальна. */
      def nodeVsnInvalid(req: INodeReq[_]): Future[Result] =
        errorHandler.onClientError(req, Status.CONFLICT, s"Node ${qs.nodeId} has been changed by someone, requested version ${qs.nodeVsn} is outdated.")

      /** Не найден эдж с указанным id. */
      def edgeNotFound(req: INodeReq[_]): Future[Result] =
        errorHandler.onClientError(req, Status.NOT_FOUND, s"Node ${qs.nodeId} does NOT have edge #${qs.edgeId}.")

    }
  }

}
