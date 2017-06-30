package util.acl

import javax.inject.{Inject, Singleton}
import io.suggest.www.util.req.ReqUtil
import models.req.{MReq, MUserInit}
import play.api.mvc._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.13 15:10
 * Description: ActionBuilder для определения залогиненности юзера.
 */
@Singleton
class MaybeAuth @Inject() (
                            reqUtil: ReqUtil,
                            aclUtil: AclUtil
                          ) {

  /** Собрать MaybeAuth action-builder. */
  def apply(userInits1: MUserInit*): ActionBuilder[MReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MReq] with InitUserCmds {

      override def userInits = userInits1

      override def invokeBlock[A](request: Request[A],
                                  block: (MReq[A]) => Future[Result]): Future[Result] = {
        // Подготовить базовые данные реквеста.
        val user = aclUtil.userFromRequest(request)

        maybeInitUser(user)

        // Сразу переходим к исполнению экшена, т.к. больше нечего делать.
        val req1 = MReq(request, user)
        block(req1)
      }

    }
  }

}

/** Интерфейс для поля с DI-инстансом [[MaybeAuth]]. */
trait IMaybeAuth {
  val maybeAuth: MaybeAuth
}
