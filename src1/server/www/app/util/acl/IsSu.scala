package util.acl

import com.google.inject.{Inject, Singleton}
import io.suggest.util.logs.MacroLogsDyn
import models.mproj.ICommonDi
import models.req.{IReqHdr, ISioUser, MReq}

import scala.concurrent.Future
import play.api.mvc.{ActionBuilder, Request, Result}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.13 13:48
 * Description: Суперпользователи сервиса имеют все необходимые права, в т.ч. для доступа в /sys/.
 */

trait IsSuUtil extends OnUnauthUtil with MacroLogsDyn {

  def supOnUnauthFut(req: IReqHdr): Future[Result] = {
    import req._
    LOGGER.warn(s"$method $path <- BLOCKED access to hidden/priveleged place from $remoteAddress user=${req.user.personIdOpt}")
    supOnUnauthResult(req)
  }

  def supOnUnauthResult(req: IReqHdr): Future[Result] = {
    onUnauth(req)
  }

}


@Singleton
final class IsSu @Inject() (
                             val cookieCleanup  : CookieCleanup,
                             val csrf           : Csrf,
                             mCommonDi          : ICommonDi
                           )
{

  import mCommonDi._

  trait Base
    extends ActionBuilder[MReq]
    with IsSuUtil
  {

    protected def isAllowed(user: ISioUser): Boolean = {
      user.isSuper
    }

    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)
      val req1 = MReq(request, user)
      if ( isAllowed(user) ) {
        LOGGER.trace(s"for user $personIdOpt :: ${request.method} ${request.path}")
        block(req1)

      } else {
        supOnUnauthFut(req1)
      }
    }

  }


  sealed abstract class BaseAbstract
    extends Base
    with ExpireSession[MReq]
    with cookieCleanup.CookieCleanup[MReq]

  object IsSu
    extends BaseAbstract
  @inline
  def apply() = IsSu

  object Get
    extends BaseAbstract
    with csrf.Get[MReq]

  object Post
    extends BaseAbstract
    with csrf.Post[MReq]

}

/** Интерфейс для DI-поля с инстансом [[IsSu]]. */
trait IIsSu {
  val isSu: IsSu
}
