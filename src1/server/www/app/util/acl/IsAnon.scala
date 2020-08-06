package util.acl

import javax.inject.Inject
import io.suggest.req.ReqUtil
import models.req.MReq
import play.api.http.HeaderNames
import play.api.inject.Injector
import play.api.mvc._
import util.ident.IdentUtil

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.14 17:42
 * Description: Является ли текущий юзер НЕзалогиненным (анонимусом)?
 */
final class IsAnon @Inject()(
                              aclUtil                : AclUtil,
                              reqUtil                : ReqUtil,
                              injector               : Injector,
                            ) {

  private lazy val identUtil = injector.instanceOf[IdentUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  private class ImplC extends reqUtil.SioActionBuilderImpl[MReq] {

    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val user = aclUtil.userFromRequest(request)

      user.personIdOpt.fold {
        val req1 = MReq(request, user)
        block(req1)
      } { personId =>
        // Юзер залогинен уже как бэ. Этот билдер для него не подходит.
        request.headers
          .get( HeaderNames.X_REQUESTED_WITH )
          .fold {
            identUtil.redirectUserSomewhere(personId)
          } { _ =>
            // Это Ajax-реквест. Нельзя слать HTTP-редирект. Необходимо вернуть ссылку для редиректа.
            for {
              rdrCall <- identUtil.redirectCallUserSomewhere(personId)
            } yield {
              // TODO Проверить Accept:, может быть реализовать какие-либо варианты.
              Results.Ok( rdrCall.absoluteURL()(request) )
            }
          }
      }
    }

  }

  private val Impl: ActionBuilder[MReq, AnyContent] = new ImplC

  @inline
  def apply() = Impl

}


/** Интерфейс для DI-поля с инстансом [[IsAnon]]. */
trait IIsAnonAcl {
  val isAnon: IsAnon
}
