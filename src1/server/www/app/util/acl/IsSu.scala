package util.acl

import io.suggest.id.login.MLoginTabs

import javax.inject.Inject
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.req.{IReqHdr, ISioUser, MReq}
import play.api.inject.Injector

import scala.concurrent.Future
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.13 13:48
 * Description: Суперпользователи сервиса имеют все необходимые права, в т.ч. для доступа в /sys/.
 */

final class IsSu @Inject() (
                             injector               : Injector,
                           )
  extends MacroLogsImpl
{ outer =>

  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val aclUtil = injector.instanceOf[AclUtil]
  protected lazy val reqUtil = injector.instanceOf[ReqUtil]

  def logBlockedAccess(req: IReqHdr): Unit = {
    import req._
    LOGGER.warn(s"$method $path <- BLOCKED access to hidden/priveleged place from $remoteClientAddress user=${req.user.personIdOpt}")
  }

  def supOnUnauthFut(req: IReqHdr): Future[Result] = {
    logBlockedAccess(req)
    isAuth.onUnauth(req, MLoginTabs.EpwLogin)
  }


  protected[acl] class Base extends reqUtil.SioActionBuilderImpl[MReq] {

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

  private val Impl: ActionBuilder[MReq, AnyContent] = new Base

  @inline
  def apply() = Impl

}
