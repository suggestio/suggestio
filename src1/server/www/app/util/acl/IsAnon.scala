package util.acl

import com.google.inject.Inject
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
                        override val mCommonDi : ICommonDi
                      )
  extends Csrf
{

  import mCommonDi._

  sealed trait IsAnonBase extends ActionBuilder[MReq] {
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

  sealed abstract class IsAnonBase2
    extends IsAnonBase
    with ExpireSession[MReq]

  // CSRF:
  /** GET-запросы с выставлением CSRF-токена. */
  object Get
    extends IsAnonBase2
    with CsrfGet[MReq]

  /** POST-запросы с проверкой CSRF-токена, выставленного ранее через [[Get]] или иной [[CsrfGet]]. */
  object Post
    extends IsAnonBase2
    with CsrfPost[MReq]

}


/** Интерфейс для DI-поля с инстансом [[IsAnon]]. */
trait IIsAnonAcl {
  val isAnon: IsAnon
}
