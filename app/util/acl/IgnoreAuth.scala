package util.acl

import models.req.SioReqMd
import play.api.mvc.{Result, Request, ActionBuilder}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 11:28
 * Description: Быстрый ActionBuilder с игнором всех данных сессии с минимальным инстансом [[AbstractRequestWithPwOpt]].
 * Полезно, когда нужен нормальный реквест, но абсолютно не важно, какой именно.
 */

object IgnoreAuth extends ActionBuilder[RequestWithPwOpt] {
  override def invokeBlock[A](request: Request[A], block: (RequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
    val req1 = RequestWithPwOpt(
      pwOpt = None,
      request = request,
      sioReqMd = SioReqMd.empty
    )
    block(req1)
  }
}
