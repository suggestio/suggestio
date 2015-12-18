package util.acl

import controllers.SioController
import models._
import models.mproj.IMCommonDi
import models.req._
import util.{PlayMacroLogsI, PlayMacroLogsDyn, PlayLazyMacroLogsImpl}
import scala.concurrent.Future
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
    def onUnauthNode(req: ISioReqHdr): Future[Result] = {
      if (req.user.isAuth) {
        Forbidden(FORBIDDEN + " Forbidden")
      } else {
        onUnauth(req)
      }
    }
  }
}


/** Аддон для сборки ctl-аддонов с проверкой admin-доступа на узел. */
trait IsAdnNodeAdminUtilCtl
  extends IMCommonDi
{

  import mCommonDi._

  trait IsAdnNodeAdminUtil extends PlayMacroLogsDyn {

    def checkAdnNodeCredsFut(adnNodeOptFut: Future[Option[MNode]], adnId: String, user: ISioUser): Future[Either[Option[MNode], MNode]] = {
      adnNodeOptFut map {
        checkAdnNodeCreds(_, adnId, user)
      }
    }

    def checkAdnNodeCreds(adnNodeOpt: Option[MNode], adnId: String, user: ISioUser): Either[Option[MNode], MNode] = {
      adnNodeOpt.fold [Either[Option[MNode], MNode]] {
        LOGGER.warn(s"checkAdnNodeCreds(): Node[$adnId] does not exist!")
        Left(None)
      } { adnNode =>
        val isAllowed = IsAdnNodeAdmin.isAdnNodeAdminCheck(adnNode, user)
        if (isAllowed) {
          Right(adnNode)
        } else {
          for (personId <- user.personIdOpt) {
            LOGGER.warn(s"checkAdnNodeCreds(): User $personId not allowed to access to node $adnId")
          }
          Left(adnNodeOpt)
        }
      }
    }

    def checkAdnNodeCredsOpt(adnNodeOptFut: Future[Option[MNode]], adnId: String, user: ISioUser): Future[Option[MNode]] = {
      checkAdnNodeCredsFut(adnNodeOptFut, adnId, user) map {
        case Right(adnNode) => Some(adnNode)
        case _ => None
      }
    }

    def isAdnNodeAdmin(adnId: String, user: ISioUser): Future[Option[MNode]] = {
      val fut = mNodeCache.getById(adnId)
      checkAdnNodeCredsOpt(fut, adnId, user)
    }

  }
}


object IsAdnNodeAdmin extends PlayLazyMacroLogsImpl {

  /** Проверка прав на управления узлом с учётом того, что юзер может быть суперюзером s.io. */
  def isAdnNodeAdminCheck(adnNode: MNode, user: ISioUser): Boolean = {
    user.isSuperUser || isAdnNodeAdminCheckStrict(adnNode, user)
  }

  /** Проверка прав на домен без учёта суперюзеров. */
  def isAdnNodeAdminCheckStrict(mnode: MNode, user: ISioUser): Boolean = {
    user.personIdOpt.exists { personId =>
      mnode.edges
        .withPredicateIterIds( MPredicates.OwnedBy )
        .contains( personId )
    }
  }

}


/** Аддон для контроллеров для проверки admin-прав доступа к узлу. */
trait IsAdnNodeAdmin
  extends IsAdnNodeAdminUtilCtl
  with OnUnauthNodeCtl
  with Csrf
{

  import mCommonDi._

  /** В реквесте содержится администрируемый узел, если всё ок. */
  sealed trait IsAdnNodeAdminBase
    extends ActionBuilder[MNodeReq]
    with PlayMacroLogsI
    with IsAdnNodeAdminUtil
    with OnUnauthNode
  {

    /** id запрашиваемого узла. */
    def adnId: String

    override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      isAdnNodeAdmin(adnId, user) flatMap {
        case Some(mnode) =>
          val req1 = MNodeReq(mnode, request, user)
          block(req1)

        case _ =>
          LOGGER.debug(s"User $personIdOpt has NO admin access to node $adnId")
          val req1 = SioReq(request, user)
          onUnauthNode(req1)
      }
    }
  }

  /** Трейт [[IsAdnNodeAdminBase]], обвешанный всеми необходимыми для работы надстройками. */
  sealed abstract class IsAdnNodeAdminBase2
    extends IsAdnNodeAdminBase
    with ExpireSession[MNodeReq]


  /** Просто проверка прав на узел перед запуском экшена. */
  case class IsAdnNodeAdmin(override val adnId: String)
    extends IsAdnNodeAdminBase2

  /** Рендер формы редактирования требует защиты от CSRF. */
  case class IsAdnNodeAdminGet(override val adnId: String)
    extends IsAdnNodeAdminBase2
    with CsrfGet[MNodeReq]

  /** Сабмит формы редактирования требует проверки CSRF-Token'а. */
  case class IsAdnNodeAdminPost(override val adnId: String)
    extends IsAdnNodeAdminBase2
    with CsrfPost[MNodeReq]

}


@deprecated("Use smthg like m.req.INodeReq instead", "2015.dec.18")
abstract class AbstractRequestForAdnNode[A](request: Request[A])
  extends AbstractRequestWithPwOpt(request) {
  def adnNode : MNode
  def isMyNode: Boolean
}

