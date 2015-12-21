package util.acl

import models.req.{MReq, SioReqMd}
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
  trait MaybeAuthBase extends ActionBuilder[MReq] {

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
      val req1 = MReq(
        request = request,
        user    = mSioUsers(personIdOpt)
      )
      block(req1)
    }

  }

  class MaybeAuth
    extends MaybeAuthBase
    with ExpireSession[MReq]
    with CookieCleanup[MReq]

  /** Сборка данных по текущей сессии юзера в реквест. */
  object MaybeAuth
    extends MaybeAuth

  object MaybeAuthGet
    extends MaybeAuth
    with CsrfGet[MReq]

  object MaybeAuthPost
    extends MaybeAuth
    with CsrfPost[MReq]

}
