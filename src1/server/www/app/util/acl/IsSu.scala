package util.acl

import com.google.inject.{Inject, Singleton}
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.req.{IReqHdr, ISioUser, MReq}

import scala.concurrent.Future
import play.api.mvc.{ActionBuilder, Request, Result}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.13 13:48
 * Description: Суперпользователи сервиса имеют все необходимые права, в т.ч. для доступа в /sys/.
 */

@Singleton
final class IsSu @Inject() (
                             aclUtil            : AclUtil,
                             isAuth             : IsAuth
                           )
  extends SioActionBuilderOuter
  with MacroLogsImpl
{

  def logBlockedAccess(req: IReqHdr): Unit = {
    import req._
    LOGGER.warn(s"$method $path <- BLOCKED access to hidden/priveleged place from $remoteAddress user=${req.user.personIdOpt}")
  }

  def supOnUnauthFut(req: IReqHdr): Future[Result] = {
    logBlockedAccess(req)
    isAuth.onUnauth(req)
  }


  class Base extends SioActionBuilderImpl[MReq] {

    protected def isAllowed(user: ISioUser): Boolean = {
      user.isSuper
    }

    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val user = aclUtil.userFromRequest(request)

      val req1 = MReq(request, user)
      if ( isAllowed(user) ) {
        LOGGER.trace(s"for user#${user.personIdOpt.orNull} :: ${request.method} ${request.path}")
        block(req1)

      } else {
        supOnUnauthFut(req1)
      }
    }

    protected def _onUnauth(req: IReqHdr): Future[Result] = {
      supOnUnauthFut(req)
    }

  }

  val Impl = new Base

  @inline
  def apply(): ActionBuilder[MReq] = Impl

}

/** Интерфейс для DI-поля с инстансом [[IsSu]]. */
trait IIsSu {
  val isSu: IsSu
}
