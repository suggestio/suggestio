package util.acl

import javax.inject.Inject

import io.suggest.req.ReqUtil
import models.req.{MReq, MSioUsers}
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 11:28
 * Description: Быстрый ActionBuilder с игнором всех данных сессии с минимальным инстансом sio-реквеста.
 * Полезно, когда нужен нормальный реквест, но абсолютно не важно, какой именно.
 */

final class IgnoreAuth @Inject() (
                                   reqUtil    : ReqUtil,
                                   mSioUsers  : MSioUsers
                                 ) {

  private class ImplC extends reqUtil.SioActionBuilderImpl[MReq] {

    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val user = mSioUsers.empty
      val req1 = MReq(request, user)
      block(req1)
    }

  }

  private val Impl: ActionBuilder[MReq, AnyContent] = new ImplC

  @inline
  def apply() = Impl

}
