package util.acl

import com.google.inject.{Inject, Singleton}
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.mproj.ICommonDi
import models.req._
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.www.util.acl.SioActionBuilderOuter

import scala.concurrent.Future
import play.api.mvc._
import play.api.mvc.Result

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.14 18:35
 * Description: Проверка прав на управление абстрактным узлом рекламной сети.
 */

/** Аддон для контроллеров для проверки admin-прав доступа к узлу. */
@Singleton
class IsNodeAdmin @Inject()(
                             aclUtil          : AclUtil,
                             isAuth           : IsAuth,
                             mCommonDi        : ICommonDi
                           )
  extends SioActionBuilderOuter
  with MacroLogsImpl
{

  import mCommonDi._


  /** Что делать, когда юзер не авторизован, но долбится в ЛК? */
  def onUnauthNode(req: IReqHdr): Future[Result] = {
    if (req.user.isAuth) {
      Results.Forbidden("403 Forbidden")
    } else {
      isAuth.onUnauth(req)
    }
  }

  def checkAdnNodeCredsFut(nodeOptFut: Future[Option[MNode]], adnId: String, user: ISioUser): Future[Either[Option[MNode], MNode]] = {
    for (nodeOpt <- nodeOptFut) yield {
      checkAdnNodeCreds(nodeOpt, adnId, user)
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



  /** Проверка прав на управления узлом с учётом того, что юзер может быть суперюзером s.io. */
  def isAdnNodeAdminCheck(mnode: MNode, user: ISioUser): Boolean = {
    user.isSuper ||
      isAdnNodeAdminCheckStrict(mnode, user)
  }

  /** Проверка прав на домен без учёта суперюзеров. */
  def isAdnNodeAdminCheckStrict(mnode: MNode, user: ISioUser): Boolean = {
    user.personIdOpt.exists { personId =>
      val haveOwnedBy = mnode.edges
        .withPredicateIterIds( MPredicates.OwnedBy )
        .contains( personId )
      // 2017.feb.8 Юзеру разрешается админить узел самого себя. Возникло в контексте реализации системы управления подузлами в ЛК.
      haveOwnedBy || mnode.id.contains(personId)
    }
  }


  /**
    * Собрать ACL ActionBuilder, проверяющий права admin-доступа к узлу.
    * @param nodeId id запрашиваемого узла.
    */
  def apply(nodeId: String, userInits1: MUserInit*): ActionBuilder[MNodeReq] = {
    new SioActionBuilderImpl[MNodeReq] with InitUserCmds {
      override def userInits = userInits1

      override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        val isAllowedFut = isAdnNodeAdmin(nodeId, user)

        if (user.personIdOpt.nonEmpty)
          maybeInitUser(user)

        isAllowedFut.flatMap {
          case Some(mnode) =>
            val req1 = MNodeReq(mnode, request, user)
            block(req1)

          case None =>
            LOGGER.debug(s"User#${user.personIdOpt.orNull} has NO admin access to node $nodeId")
            val req1 = MReq(request, user)
            onUnauthNode(req1)
        }
      }

    }
  }

}

/** Интерфейс для поля c DI-инстансом [[IsNodeAdmin]]. */
trait IIsNodeAdmin {
  val isNodeAdmin: IsNodeAdmin
}
