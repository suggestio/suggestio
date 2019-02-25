package util.acl

import controllers.ErrorHandler
import io.suggest.ext.svc.MExtService
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.{Inject, Singleton}
import models.mext.MExtServicesJvm
import models.req.MLoginViaReq
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}
import play.api.http.Status
import play.api.inject.Injector
import util.ident.ss.SecureSocialLoginAdp

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.02.19 21:18
  * Description: ACL поддержки внешнего логина.
  */
@Singleton
class CanLoginVia @Inject()(
                             aclUtil        : AclUtil,
                             reqUtil        : ReqUtil,
                             errorHandler   : ErrorHandler,
                             injector       : Injector,
                           )
  extends MacroLogsImpl
{

  /** Сборка action-builder, фильтрующий экшены логина через внешний сервис. */
  def apply(extService: MExtService): ActionBuilder[MLoginViaReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MLoginViaReq] {
      override def invokeBlock[A](request: Request[A], block: MLoginViaReq[A] => Future[Result]): Future[Result] = {
        // Надо понять, есть ли возможность логина через указанный внешний сервис.
        val mreq = aclUtil.reqFromRequest( request )

        lazy val logPrefix = s"${mreq.remoteClientAddress} ${mreq.user.personIdOpt.fold("")(" u#" + _)}:"

        val svcJvm = MExtServicesJvm
          .forService( extService )

        svcJvm
          .loginProvider
          .fold [Future[Result]] {
            // Нет логин-провайдера у данного сервиса.
            LOGGER.warn(s"$logPrefix Tried to login via $extService, but service does not have login provider.")
            errorHandler.onClientError(request, Status.NOT_FOUND)

          } { loginProvider =>
            // Можно логинится через указанный сервис.
            // TODO Выбирать адаптер динамически на основе провайдера.
            val apiAdp = injector.instanceOf[SecureSocialLoginAdp]
            val req1 = MLoginViaReq( apiAdp, loginProvider, mreq, mreq.user )
            block(req1)
          }
      }
    }
  }

}
