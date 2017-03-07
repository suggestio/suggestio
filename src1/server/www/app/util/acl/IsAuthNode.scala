package util.acl

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.MNodeType
import models.mproj.ICommonDi
import models.req.{MNodeReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.10.15 16:15
 * Description: Аддон для контроллеров c гибридом IsAuth и тривиального чтения узла MNode.
 */
class IsAuthNode @Inject() (
                             aclUtil                : AclUtil,
                             isAuth                 : IsAuth,
                             mCommonDi              : ICommonDi
                           )
  extends SioActionBuilderOuter
  with MacroLogsImpl
{

  import mCommonDi._


  /** ActionBuilder с логикой проверки залогиненности вкупе с доступом к узлу.
    *
    * @param nodeId id запрашиваемого узла.
    * @param ntypes Допустимые типы для запроса или empty, если не важно.
    */
  def apply(nodeId: String, ntypes: MNodeType*): ActionBuilder[MNodeReq] = {
    new SioActionBuilderImpl[MNodeReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        if (!user.isAuth) {
          // Не залогинен.
          isAuth.onUnauth(request)

        } else {
          // Юзер залогинен, нужно продолжать запрос.
          val nodeOptFut = mNodesCache.getById(nodeId)
          val _ntypes = ntypes
          nodeOptFut.flatMap {
            case Some(mnode) if _ntypes.isEmpty || _ntypes.contains(mnode.common.ntype) =>
              val req1 = MNodeReq(mnode, request, user)
              block(req1)

            case other =>
              for (mnode <- other) {
                LOGGER.warn(s"404 for existing node[$nodeId] with unexpected ntype. Expected one of [${_ntypes.mkString(",")}], but node[$nodeId] has ntype ${mnode.common.ntype}.")
              }
              val req1 = MReq(request, user)
              errorHandler.http404Fut(req1)
          }
        }
      }

    }
  }

}
