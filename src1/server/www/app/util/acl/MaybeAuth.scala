package util.acl

import com.google.inject.{Inject, Singleton}
import models.mproj.ICommonDi
import models.req.{MReq, MUserInit}
import play.api.mvc._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 15:10
 * Description: ActionBuilder для определения залогиненности юзера.
 */
@Singleton
class MaybeAuth @Inject() (
                            val csrf                : Csrf,
                            override val mCommonDi  : ICommonDi
                          )
  extends CookieCleanupSupport
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
      // Подготовить базовые данные реквеста.
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)
      maybeInitUser(user)

      // Сразу переходим к исполнению экшена, т.к. больше нечего делать.
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
  @inline
  def apply(userInits: MUserInit*) = MaybeAuth(userInits: _*)

  case class Get(override val userInits: MUserInit*)
    extends MaybeAuthAbstract
    with csrf.Get[MReq]

  case class Post(override val userInits: MUserInit*)
    extends MaybeAuthAbstract
    with csrf.Post[MReq]

}

/** Интерфейс для поля с DI-инстансом [[MaybeAuth]]. */
trait IMaybeAuth {
  val maybeAuth: MaybeAuth
}
