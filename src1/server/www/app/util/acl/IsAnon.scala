package util.acl

import com.google.inject.Inject
import io.suggest.sec.util.ExpireSession
import models.mproj.ICommonDi
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
class IsAnon @Inject()(
                        identUtil              : IdentUtil,
                        val csrf               : Csrf,
                        mCommonDi              : ICommonDi
                      ) {

  import mCommonDi._

  sealed trait Base extends ActionBuilder[MReq] {
    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      personIdOpt.fold {
        val user = mSioUsers(personIdOpt)
        val req1 = MReq(request, user)
        block(req1)

      } { personId =>
        // Юзер залогинен уже как бэ. Этот билдер для него не подходит.
        identUtil.redirectUserSomewhere(personId)
      }
    }
  }

  sealed abstract class Abstract
    extends Base
    with ExpireSession[MReq]

  /** Без CSRF. */
  object NoCsrf extends Abstract
  @inline
  def apply() = NoCsrf

  // CSRF:
  /** GET-запросы с выставлением CSRF-токена. */
  object Get
    extends Abstract
    with csrf.Get[MReq]

  /** POST-запросы с проверкой CSRF-токена, выставленного ранее через csrf.Get. */
  object Post
    extends Abstract
    with csrf.Post[MReq]

}


/** Интерфейс для DI-поля с инстансом [[IsAnon]]. */
trait IIsAnonAcl {
  val isAnon: IsAnon
}
