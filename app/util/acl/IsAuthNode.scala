package util.acl

import io.suggest.di.{IEsClient, IExecutionContext}
import models.MNodeType
import models.req.SioReqMd
import play.api.mvc.{Result, Request, ActionBuilder}
import util.di.{INodeCache, IErrorHandler}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.10.15 16:15
 * Description: Аддон для контроллеров c гибридом IsAuth и тривиального чтения узла MNode.
 */
trait IsAuthNode
  extends OnUnauthUtilCtl
  with IExecutionContext
  with IEsClient
  with IErrorHandler
  with INodeCache
{

  /** Трейт с логикой проверки залогиненности вкупе с доступом к узлу. */
  trait IsAuthNodeBase
    extends ActionBuilder[SimpleRequestForAdnNode]
    with OnUnauthUtil
  {

    /** id запрашиваемого узла. */
    def nodeId: String

    /** Допустимые типы для запроса или empty, если не важно. */
    // TODO Seq используется для доступа к contains(), который до конца не абстрагирован в разных scala.collections.
    def ntypes: Seq[MNodeType]

    override def invokeBlock[A](request: Request[A],
                                block: (SimpleRequestForAdnNode[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      if (pwOpt.isEmpty) {
        onUnauth(request)

      } else {
        val nodeOptFut = mNodeCache.getById( nodeId )
        val srmFut = SioReqMd.fromPwOpt( pwOpt )
        nodeOptFut flatMap {
          case Some(mnode) if ntypes.contains(mnode.common.ntype) =>
            srmFut flatMap { srm =>
              val req1 = SimpleRequestForAdnNode(mnode, request, pwOpt, srm)
              block(req1)
            }

          case None =>
            errorHandler.http404Fut(request)
        }
      }
    }
  }


  /** Абстрактный класс с недореализацией IsAuthNodeBase. */
  abstract class IsAuthNodeAbstract
    extends IsAuthNodeBase
    with ExpireSession[ SimpleRequestForAdnNode ]


  case class IsAuthNode(override val nodeId: String,
                        override val ntypes: MNodeType*)
    extends IsAuthNodeAbstract
}
