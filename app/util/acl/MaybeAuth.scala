package util.acl

import models.req.{MReq, MUserInit}
import play.api.mvc._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 15:10
 * Description: ActionBuilder для определения залогиненности юзера.
 */
trait MaybeAuth
  extends CookieCleanupSupport
  with Csrf
{

  import mCommonDi._

  /** Здесь логика MaybeAuth action-builder'а. */
  sealed trait MaybeAuthBase
    extends ActionBuilder[MReq]
    with InitUserCmds
  {

    /**
     * Вызывается генератор экшена в билдере.
     * @param request Реквест.
     * @param block Суть действий в виде функции, возвращающей фьючерс.
     * @tparam A Подтип реквеста.
     * @return Фьючерс, описывающий результат.
     */
    override def invokeBlock[A](request: Request[A],
                                block: (MReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)
      maybeInitUser(user)
      val req1 = MReq(request, user)
      block(req1)
    }

  }

  sealed abstract class MaybeAuthAbstract
    extends MaybeAuthBase
    with ExpireSession[MReq]
    with CookieCleanup[MReq]

  /** Сборка данных по текущей сессии юзера в реквест. */
  case class MaybeAuth(override val userInits: MUserInit*)
    extends MaybeAuthAbstract

  case class MaybeAuthGet(override val userInits: MUserInit*)
    extends MaybeAuthAbstract
    with CsrfGet[MReq]

  case class MaybeAuthPost(override val userInits: MUserInit*)
    extends MaybeAuthAbstract
    with CsrfPost[MReq]

}
