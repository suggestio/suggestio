package util.acl

import controllers.SioController
import io.suggest.di.{IExecutionContext, IEsClient}
import models._
import models.req.SioReqMd
import util.di.INodeCache
import util.{PlayMacroLogsI, PlayMacroLogsDyn, PlayLazyMacroLogsImpl}
import scala.concurrent.Future
import util.acl.PersonWrapper.PwOpt_t
import play.api.mvc._
import play.api.mvc.Result

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 18:35
 * Description: Проверка прав на управление абстрактным узлом рекламной сети.
 */

trait OnUnauthNodeCtl
  extends SioController
  with OnUnauthUtilCtl
{
  trait OnUnauthNode extends OnUnauthUtil {
    /** Что делать, когда юзер не авторизован, но долбится в ЛК? */
    def onUnauthNode(req: RequestHeader, pwOpt: PwOpt_t): Future[Result] = {
      pwOpt match {
        case None =>
          onUnauth(req)
        case _ =>
          Future successful Forbidden(FORBIDDEN + " Forbidden")
      }
    }
  }
}


/** Аддон для сборки ctl-аддонов с проверкой admin-доступа на узел. */
trait IsAdnNodeAdminUtilCtl
  extends IEsClient
  with IExecutionContext
  with INodeCache
{
  trait IsAdnNodeAdminUtil extends PlayMacroLogsDyn {

    def checkAdnNodeCredsFut(adnNodeOptFut: Future[Option[MAdnNode]], adnId: String, pwOpt: PwOpt_t): Future[Either[Option[MAdnNode], MAdnNode]] = {
      adnNodeOptFut map {
        checkAdnNodeCreds(_, adnId, pwOpt)
      }
    }

    def checkAdnNodeCreds(adnNodeOpt: Option[MAdnNode], adnId: String, pwOpt: PwOpt_t): Either[Option[MAdnNode], MAdnNode] = {
      adnNodeOpt.fold [Either[Option[MAdnNode], MAdnNode]] {
        LOGGER.warn(s"checkAdnNodeCreds(): Node[$adnId] does not exist!")
        Left(None)
      } { adnNode =>
        val isAllowed = IsAdnNodeAdmin.isAdnNodeAdminCheck(adnNode, pwOpt)
        if (isAllowed) {
          Right(adnNode)
        } else {
          if (pwOpt.isDefined)
            LOGGER.warn(s"checkAdnNodeCreds(): User $pwOpt not allowed to access to node ${adnNode.id.get}")
          Left(adnNodeOpt)
        }
      }
    }

    def checkAdnNodeCredsOpt(adnNodeOptFut: Future[Option[MAdnNode]], adnId: String, pwOpt: PwOpt_t): Future[Option[MAdnNode]] = {
      checkAdnNodeCredsFut(adnNodeOptFut, adnId, pwOpt) map {
        case Right(adnNode) => Some(adnNode)
        case _ => None
      }
    }

    def isAdnNodeAdmin(adnId: String, pwOpt: PwOpt_t): Future[Option[MAdnNode]] = {
      val fut = mNodeCache.getById(adnId)
      checkAdnNodeCredsOpt(fut, adnId, pwOpt)
    }

  }
}


object IsAdnNodeAdmin extends PlayLazyMacroLogsImpl {

  /** Проверка прав на управления узлом с учётом того, что юзер может быть суперюзером s.io. */
  def isAdnNodeAdminCheck(adnNode: MAdnNode, pwOpt: PwOpt_t): Boolean = {
    PersonWrapper.isSuperuser(pwOpt) || isAdnNodeAdminCheckStrict(adnNode, pwOpt)
  }

  /** Проверка прав на домен без учёта суперюзеров. */
  def isAdnNodeAdminCheckStrict(adnNode: MAdnNode, pwOpt: PwOpt_t): Boolean = {
    pwOpt.exists { pw =>
      adnNode.personIds contains pw.personId
    }
  }

}


/** Аддон для контроллеров для проверки admin-прав доступа к узлу. */
trait IsAdnNodeAdmin
  extends IsAdnNodeAdminUtilCtl
  with IExecutionContext
  with OnUnauthNodeCtl
{

  /** В реквесте содержится администрируемый узел, если всё ок. */
  sealed trait IsAdnNodeAdminBase
    extends ActionBuilder[AbstractRequestForAdnNode]
    with PlayMacroLogsI
    with IsAdnNodeAdminUtil
    with OnUnauthNode
  {

    /** id запрашиваемого узла. */
    def adnId: String

    override def invokeBlock[A](request: Request[A], block: (AbstractRequestForAdnNode[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      val srmFut = SioReqMd.fromPwOptAdn(pwOpt, adnId)
      isAdnNodeAdmin(adnId, pwOpt) flatMap {
        case Some(adnNode) =>
          srmFut flatMap { srm =>
            val req1 = RequestForAdnNodeAdm(adnNode, isMyNode = true, request, pwOpt, srm)
            block(req1)
          }

        case _ =>
          LOGGER.debug(s"User $pwOpt has NO admin access to node $adnId")
          onUnauthNode(request, pwOpt)
      }
    }
  }
  /** Трейт [[IsAdnNodeAdminBase]], обвешанный всеми необходимыми для работы надстройками. */
  sealed abstract class IsAdnNodeAdminBase2
    extends IsAdnNodeAdminBase
    with ExpireSession[AbstractRequestForAdnNode]
    with PlayMacroLogsDyn


  /** Просто проверка прав на узел перед запуском экшена. */
  case class IsAdnNodeAdmin(override val adnId: String)
    extends IsAdnNodeAdminBase2

  /** Рендер формы редактирования требует защиты от CSRF. */
  case class IsAdnNodeAdminGet(override val adnId: String)
    extends IsAdnNodeAdminBase2
    with CsrfGet[AbstractRequestForAdnNode]

  /** Сабмит формы редактирования требует проверки CSRF-Token'а. */
  case class IsAdnNodeAdminPost(override val adnId: String)
    extends IsAdnNodeAdminBase2
    with CsrfPost[AbstractRequestForAdnNode]

}


abstract class AbstractRequestForAdnNode[A](request: Request[A])
  extends AbstractRequestWithPwOpt(request) {
  def adnNode: MAdnNode
  def isMyNode: Boolean
}

case class RequestForAdnNodeAdm[A](
  adnNode   : MAdnNode,
  isMyNode  : Boolean,
  request   : Request[A],
  pwOpt     : PwOpt_t,
  sioReqMd  : SioReqMd
)
  extends AbstractRequestForAdnNode(request)
