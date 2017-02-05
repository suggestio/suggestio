package util.acl

import controllers.SioController
import models.msys.MNodeEdgeIdQs
import models.req._
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.10.16 16:26
  * Description: ACL-аддон для sys-контроллеров для экшенов управления эджами.
  */
trait IsSuNodeEdge
  extends SioController
  with Csrf
{

  import mCommonDi._


  /** Комбинация IsSuperuser + IsAdnAdmin + доступ к эджу по индексу. */
  sealed trait IsSuNodeEdgeBase
    extends ActionBuilder[MNodeEdgeReq]
    with IsSuperuserUtil
  {

    /** query string. */
    def qs: MNodeEdgeIdQs

    protected[this] def logPrefix = s"${getClass.getSimpleName}(${qs.nodeId}/v=${qs.nodeVsn}/e=${qs.edgeId}):"

    override def invokeBlock[A](request: Request[A], block: (MNodeEdgeReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)
      val _qs = qs

      if (user.isSuper) {
        val mnodeOptFut = mNodesCache.getById(_qs.nodeId)
        mnodeOptFut.flatMap {

          // Запрошенный узел найден.
          case Some(mnode) =>
            def nodeReq = MNodeReq(mnode, request, user)
            if ( mnode.versionOpt.contains(_qs.nodeVsn) ) {
              mnode.edges
                .withIndex( _qs.edgeId )
                .fold [Future[Result]] {
                  // Нет запрошенного эджа
                  edgeNotFound(nodeReq)
                } { medge =>
                  val req1 = MNodeEdgeReq(mnode, medge, request, user)
                  block(req1)
                }

            } else {
              // Узел был изменён до начала этого запроса. Порядок эджей может быть нарушен.
              LOGGER.debug(s"$logPrefix node version invalid. Expected=${_qs.nodeVsn}, real=${mnode.versionOpt}")
              nodeVsnInvalid(nodeReq)
            }

          // Нет узла, почему-то.
          case None =>
            val req1 = MReq(request, user)
            nodeNotFound(req1)
        }

      } else {
        val req1 = MReq(request, user)
        supOnUnauthFut(req1)
      }
    }

    /** Узел не найден. */
    def nodeNotFound(req: IReqHdr): Future[Result] = {
      errorHandler.http404Fut(req)
    }

    /** Запрошенная версия узла неактуальна. */
    def nodeVsnInvalid(req: INodeReq[_]): Future[Result] = {
      Conflict(s"Node ${qs.nodeId} has been changed by someone, requested version ${qs.nodeVsn} is outdated.")
    }

    /** Не найден эдж с указанным id. */
    def edgeNotFound(req: INodeReq[_]): Future[Result] = {
      NotFound(s"Node ${qs.nodeId} does NOT have edge #${qs.edgeId}.")
    }

  }


  /** Неполная реализация IsSuNodeBase для снижения объемов кодогенерации. */
  abstract class IsSuNodeEdgeAbstract
    extends IsSuNodeEdgeBase
    with ExpireSession[MNodeEdgeReq]


  /** Доступ к node edge для суперюзеров. */
  case class IsSuNodeEdge(override val qs: MNodeEdgeIdQs)
    extends IsSuNodeEdgeAbstract

  /** Доступ к node edge для суперюзеров в рамках CSRF GET. */
  case class IsSuNodeEdgeGet(override val qs: MNodeEdgeIdQs)
    extends IsSuNodeEdgeAbstract
    with CsrfGet[MNodeEdgeReq]

  /** Доступ к node edge для суперюзеров во время CSRF POST. */
  case class IsSuNodeEdgePost(override val qs: MNodeEdgeIdQs)
    extends IsSuNodeEdgeAbstract
    with CsrfPost[MNodeEdgeReq]

}
