package util.acl

import com.google.inject.Inject
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.req.MReq
import play.api.mvc._
import util.ident.IdentUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.14 17:42
 * Description: Является ли текущий юзер НЕзалогиненным (анонимусом)?
 */
final class IsAnon @Inject()(
                              aclUtil                : AclUtil,
                              identUtil              : IdentUtil
                            )
  extends SioActionBuilderOuter
{

  class ImplC extends SioActionBuilderImpl[MReq] {

    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val user = aclUtil.userFromRequest(request)

      user.personIdOpt.fold {
        val req1 = MReq(request, user)
        block(req1)
      } { personId =>
        // Юзер залогинен уже как бэ. Этот билдер для него не подходит.
        identUtil.redirectUserSomewhere(personId)
      }
    }

  }

  val Impl = new ImplC

  @inline
  def apply(): ActionBuilder[MReq] = {
    Impl
  }

}


/** Интерфейс для DI-поля с инстансом [[IsAnon]]. */
trait IIsAnonAcl {
  val isAnon: IsAnon
}
