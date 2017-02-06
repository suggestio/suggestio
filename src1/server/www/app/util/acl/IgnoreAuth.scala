package util.acl

import com.google.inject.Inject
import models.req.{MReq, MSioUsers}
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 11:28
 * Description: Быстрый ActionBuilder с игнором всех данных сессии с минимальным инстансом sio-реквеста.
 * Полезно, когда нужен нормальный реквест, но абсолютно не важно, какой именно.
 */

class IgnoreAuth @Inject() (mSioUsers: MSioUsers) {

  object IgnoreAuth extends ActionBuilder[MReq] {

    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val user = mSioUsers(None)
      val req1 = MReq(request, user)
      block(req1)
    }

  }

}

trait IIgnoreAuth {
  val ignoreAuth: IgnoreAuth
}
