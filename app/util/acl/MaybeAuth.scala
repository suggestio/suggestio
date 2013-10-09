package util.acl

import play.api.mvc._
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 15:10
 * Description: ActionBuilder для определения залогиненности юзера.
 */
object MaybeAuth extends ActionBuilder[AbstractRequestWithPwOpt] {

  /**
   * Вызывается генератор экшена в билдере.
   * @param request Реквест.
   * @param block Суть действий в виде функции, возвращающей фьючерс.
   * @tparam A Подтип реквеста.
   * @return Фьючерс, описывающий результат.
   */
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    block(RequestWithPwOpt(pwOpt, request))
  }

}



