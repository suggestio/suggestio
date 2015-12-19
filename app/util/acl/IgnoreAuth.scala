package util.acl

import controllers.SioController
import models.req.SioReq
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 11:28
 * Description: Быстрый ActionBuilder с игнором всех данных сессии с минимальным инстансом sio-реквеста.
 * Полезно, когда нужен нормальный реквест, но абсолютно не важно, какой именно.
 */

trait IgnoreAuth extends SioController {

  import mCommonDi._

  object IgnoreAuth extends ActionBuilder[SioReq] {

    override def invokeBlock[A](request: Request[A], block: (SioReq[A]) => Future[Result]): Future[Result] = {
      val user = mSioUsers(None)
      val req1 = SioReq(request, user)
      block(req1)
    }

  }

}
