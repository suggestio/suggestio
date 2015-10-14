package util.acl

import io.suggest.di.IExecutionContext
import models.MAd
import models.req.SioReqMd
import play.api.mvc._
import util.acl.PersonWrapper.PwOpt_t
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 15:10
 * Description: ActionBuilder для определения залогиненности юзера.
 */
trait MaybeAuth extends IExecutionContext {

  /** Здесь логика MaybeAuth action-builder'а. */
  trait MaybeAuthBase extends ActionBuilder[AbstractRequestWithPwOpt] {

    /**
     * Вызывается генератор экшена в билдере.
     * @param request Реквест.
     * @param block Суть действий в виде функции, возвращающей фьючерс.
     * @tparam A Подтип реквеста.
     * @return Фьючерс, описывающий результат.
     */
    override def invokeBlock[A](request: Request[A],
                                block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      val srmFut = SioReqMd.fromPwOpt(pwOpt)
      srmFut flatMap { srm =>
        block(RequestWithPwOpt(pwOpt, request, srm))
      }
    }

  }

  class MaybeAuth
    extends MaybeAuthBase
    with ExpireSession[AbstractRequestWithPwOpt]
    with CookieCleanup[AbstractRequestWithPwOpt]

  /** Сборка данных по текущей сессии юзера в реквест. */
  object MaybeAuth
    extends MaybeAuth

  object MaybeAuthGet
    extends MaybeAuth
    with CsrfGet[AbstractRequestWithPwOpt]

  object MaybeAuthPost
    extends MaybeAuth
    with CsrfPost[AbstractRequestWithPwOpt]

}


/** Абстрактный реквест в сторону какой-то рекламной карточки. */
abstract class AbstractRequestWithAd[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def mad: MAd
}

/** Экземпляр реквеста, содержащего рекламную запрашиваемую карточку. */
case class RequestWithAd[A](
  mad       : MAd,
  request   : Request[A],
  pwOpt     : PwOpt_t,
  sioReqMd  : SioReqMd
)
  extends AbstractRequestWithAd(request)



