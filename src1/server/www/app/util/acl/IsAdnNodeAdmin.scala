package util.acl

import com.google.inject.{Inject, Singleton}
import io.suggest.util.logs.MacroLogsDyn
import models._
import models.mproj.ICommonDi
import models.req._
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut

import scala.concurrent.Future
import play.api.mvc._
import play.api.mvc.Result

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 18:35
 * Description: Проверка прав на управление абстрактным узлом рекламной сети.
 */

trait OnUnauthNode extends OnUnauthUtil {
  /** Что делать, когда юзер не авторизован, но долбится в ЛК? */
  def onUnauthNode(req: IReqHdr): Future[Result] = {
    if (req.user.isAuth) {
      Results.Forbidden("403 Forbidden")
    } else {
      onUnauth(req)
    }
  }
}


/** Аддон для контроллеров для проверки admin-прав доступа к узлу. */
@Singleton
class IsAdnNodeAdmin @Inject() (override val mCommonDi: ICommonDi) extends Csrf
{

  import mCommonDi._


  trait IsAdnNodeAdminUtil extends MacroLogsDyn {

    def checkAdnNodeCredsFut(adnNodeOptFut: Future[Option[MNode]], adnId: String, user: ISioUser): Future[Either[Option[MNode], MNode]] = {
      adnNodeOptFut.map {
        checkAdnNodeCreds(_, adnId, user)
      }
    }

    def checkAdnNodeCreds(adnNodeOpt: Option[MNode], adnId: String, user: ISioUser): Either[Option[MNode], MNode] = {
      adnNodeOpt.fold [Either[Option[MNode], MNode]] {
        LOGGER.warn(s"checkAdnNodeCreds(): Node[$adnId] does not exist!")
        Left(None)
      } { adnNode =>
        val isAllowed = isAdnNodeAdminCheck(adnNode, user)
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
      checkAdnNodeCredsFut(adnNodeOptFut, adnId, user).map {
        case Right(adnNode) => Some(adnNode)
        case _ => None
      }
    }

    def isAdnNodeAdmin(adnId: String, user: ISioUser): Future[Option[MNode]] = {
      val fut = mNodesCache.getById(adnId)
      checkAdnNodeCredsOpt(fut, adnId, user)
    }

  }


  /** Проверка прав на управления узлом с учётом того, что юзер может быть суперюзером s.io. */
  def isAdnNodeAdminCheck(adnNode: MNode, user: ISioUser): Boolean = {
    user.isSuper || isAdnNodeAdminCheckStrict(adnNode, user)
  }

  /** Проверка прав на домен без учёта суперюзеров. */
  def isAdnNodeAdminCheckStrict(mnode: MNode, user: ISioUser): Boolean = {
    user.personIdOpt.exists { personId =>
      mnode.edges
        .withPredicateIterIds( MPredicates.OwnedBy )
        .contains( personId )
    }
  }

  /** В реквесте содержится администрируемый узел, если всё ок. */
  sealed trait Base
    extends ActionBuilder[MNodeReq]
    with IsAdnNodeAdminUtil
    with OnUnauthNode
    with InitUserCmds
  {

    /** id запрашиваемого узла. */
    def nodeId: String

    override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)
      val isAllowedFut = isAdnNodeAdmin(nodeId, user)

      maybeInitUser(user)

      isAllowedFut.flatMap {
        case Some(mnode) =>
          val req1 = MNodeReq(mnode, request, user)
          block(req1)

        case _ =>
          LOGGER.debug(s"User $personIdOpt has NO admin access to node $nodeId")
          val req1 = MReq(request, user)
          onUnauthNode(req1)
      }
    }
  }

  /** Трейт [[Base]], обвешанный всеми необходимыми для работы надстройками. */
  sealed abstract class BaseAbstract
    extends Base
    with ExpireSession[MNodeReq]


  /** Просто проверка прав на узел перед запуском экшена. */
  case class IsAdnNodeAdmin(
    override val nodeId     : String,
    override val userInits  : MUserInit*
  )
    extends BaseAbstract
  @inline
  def apply(nodeId: String, userInits: MUserInit*) = IsAdnNodeAdmin(nodeId, userInits: _*)

  /** Рендер формы редактирования требует защиты от CSRF. */
  case class Get(
    override val nodeId     : String,
    override val userInits  : MUserInit*
  )
    extends BaseAbstract
    with CsrfGet[MNodeReq]

  /** Сабмит формы редактирования требует проверки CSRF-Token'а. */
  case class Post(
    override val nodeId     : String,
    override val userInits  : MUserInit*
  )
    extends BaseAbstract
    with CsrfPost[MNodeReq]

}

/** Интерфейс для поля c DI-инстансом [[IsAdnNodeAdmin]]. */
trait IIsAdnNodeAdmin {
  val isAdnNodeAdmin: IsAdnNodeAdmin
}
