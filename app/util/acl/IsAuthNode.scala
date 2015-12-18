package util.acl

import models.MNodeType
import models.req.MNodeReq
import play.api.mvc.{Result, Request, ActionBuilder}
import util.PlayMacroLogsDyn

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.10.15 16:15
 * Description: Аддон для контроллеров c гибридом IsAuth и тривиального чтения узла MNode.
 */
trait IsAuthNode
  extends OnUnauthUtilCtl
{

  import mCommonDi._

  /** Трейт с логикой проверки залогиненности вкупе с доступом к узлу. */
  trait IsAuthNodeBase
    extends ActionBuilder[MNodeReq]
    with OnUnauthUtil
    with PlayMacroLogsDyn
  {

    /** id запрашиваемого узла. */
    def nodeId: String

    /** Допустимые типы для запроса или empty, если не важно. */
    // TODO Seq используется для доступа к contains(), который до конца не абстрагирован в разных scala.collections.
    def ntypes: Seq[MNodeType]

    override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {

      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      if (!user.isAuth) {
        // Не залогинен.
        onUnauth(request)

      } else {
        // Юзер залогинен, нужно продолжать запрос.
        val nodeOptFut = mNodeCache.getById( nodeId )
        val _ntypes = ntypes
        nodeOptFut flatMap {
          case Some(mnode) if _ntypes.isEmpty || _ntypes.contains(mnode.common.ntype) =>
            val req1 = MNodeReq(mnode, request, user)
            block(req1)

          case other =>
            other.foreach { mnode =>
              LOGGER.warn(s"404 for existing node[$nodeId] with unexpected ntype. Expected one of [${_ntypes.mkString(",")}], but node[$nodeId] has ntype ${mnode.common.ntype}.")
            }
            errorHandler.http404Fut(request)
        }
      }
    }
  }


  /** Абстрактный класс с недореализацией IsAuthNodeBase. */
  abstract class IsAuthNodeAbstract
    extends IsAuthNodeBase
    with ExpireSession[MNodeReq]


  case class IsAuthNode(override val nodeId: String,
                        override val ntypes: MNodeType*)
    extends IsAuthNodeAbstract

}
