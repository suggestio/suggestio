package util.acl

import io.suggest.di.{IExecutionContext, IEsClient}
import models.MNode
import models.req.SioReqMd
import play.api.mvc.{Result, ActionBuilder, Request}
import util.di.{IErrorHandler, INodeCache}
import util.{PlayMacroLogsDyn, PlayMacroLogsI}
import util.acl.PersonWrapper.PwOpt_t

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 18:49
 * Description: Смесь принципов MaybeAuth и считывания указанного узла.
 */
trait AdnNodeMaybeAuth
  extends IEsClient
  with IExecutionContext
  with INodeCache
  with IErrorHandler
{

  /** Общий код проверок типа AdnNodeMaybeAuth. */
  sealed trait AdnNodeMaybeAuthBase
    extends ActionBuilder[SimpleRequestForAdnNode]
    with PlayMacroLogsI
  {

    /** id запрашиваемого узла. */
    def adnId: String

    override def invokeBlock[A](request: Request[A], block: (SimpleRequestForAdnNode[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      val srmFut = SioReqMd.fromPwOpt(pwOpt)
      mNodeCache.getById(adnId) flatMap {
        case Some(adnNode) =>
          if (isNodeValid(adnNode)) {
            srmFut flatMap { srm =>
              val req1 = SimpleRequestForAdnNode(adnNode, request, pwOpt, srm)
              block(req1)
            }
          } else {
            accessProhibited(adnNode, request)
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
      LOGGER.warn(s"Node $adnId not found, requested by ${request.remoteAddress}")
      errorHandler.http404Fut
    }
  }

  /** Промежуточный трейт из-за использования в ExpireSession модификатора abstract override. */
  sealed abstract class AdnNodeMaybeAuthAbstract
    extends AdnNodeMaybeAuthBase
    with ExpireSession[SimpleRequestForAdnNode]
    with PlayMacroLogsDyn


  /**
   * Реализация [[AdnNodeMaybeAuthBase]] с поддержкой таймаута сессии.
   * @param adnId id узла.
   */
  case class AdnNodeMaybeAuth(override val adnId: String)
    extends AdnNodeMaybeAuthAbstract
  {
    override def isNodeValid(adnNode: MNode): Boolean = true
  }

}


case class SimpleRequestForAdnNode[A](
  adnNode   : MNode,
  request   : Request[A],
  pwOpt     : PwOpt_t,
  sioReqMd  : SioReqMd
)
  extends AbstractRequestForAdnNode(request)
{
  override lazy val isMyNode = IsAdnNodeAdmin.isAdnNodeAdminCheck(adnNode, pwOpt)
}


