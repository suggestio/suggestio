package util.acl

import controllers.SioController
import models.req.SioReq
import play.api.mvc._
import util.di.IIdentUtil
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.14 17:42
 * Description: Является ли текущий юзер НЕзалогиненным (анонимусом)?
 */
trait IsAnon
  extends SioController
  with IIdentUtil
  with Csrf
{

  import mCommonDi._

  trait IsAnonBase extends ActionBuilder[SioReq] {
    override def invokeBlock[A](request: Request[A], block: (SioReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      personIdOpt.fold {
        val user = mSioUsers(personIdOpt)
        val req1 = SioReq(request, user)
        block(req1)

      } { personId =>
        // Юзер залогинен уже как бэ. Этот билдер для него не подходит.
        identUtil.redirectUserSomewhere(personId)
      }
    }
  }

  abstract class IsAnonBase2
    extends IsAnonBase
    with ExpireSession[SioReq]

  // CSRF:
  /** GET-запросы с выставлением CSRF-токена. */
  object IsAnonGet
    extends IsAnonBase2
    with CsrfGet[SioReq]

  /** POST-запросы с проверкой CSRF-токена, выставленного ранее через [[IsAnonGet]] или иной [[CsrfGet]]. */
  object IsAnonPost
    extends IsAnonBase2
    with CsrfPost[SioReq]

}
