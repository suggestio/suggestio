package util.acl

import controllers.ErrorHandler
import io.suggest.ext.svc.{MExtService, MExtServices}
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.mext.MExtServicesJvm
import models.req.MLoginViaReq
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}
import play.api.http.Status
import play.api.inject.Injector
import util.ident.IExtLoginAdp
import util.ident.esia.EsiaLoginUtil
import util.ident.ss.SecureSocialLoginUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.02.19 21:18
  * Description: ACL поддержки внешнего логина.
  */
final class CanLoginVia @Inject()(
                                   aclUtil        : AclUtil,
                                   reqUtil        : ReqUtil,
                                   injector       : Injector,
                                 )
  extends MacroLogsImpl
{

  private lazy val errorHandler = injector.instanceOf[ErrorHandler]
  private lazy val ssLoginUtil = injector.instanceOf[SecureSocialLoginUtil]
  private lazy val esiaLoginUtil = injector.instanceOf[EsiaLoginUtil]


  /** Сборка action-builder, фильтрующий экшены логина через внешний сервис. */
  def apply(extService: MExtService): ActionBuilder[MLoginViaReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MLoginViaReq] {
      override def invokeBlock[A](request: Request[A], block: MLoginViaReq[A] => Future[Result]): Future[Result] = {
        // Надо понять, есть ли возможность логина через указанный внешний сервис.
        val mreq = aclUtil.reqFromRequest( request )

        lazy val logPrefix = s"${mreq.remoteClientAddress} ${mreq.user.personIdOpt.fold("")(" u#" + _)}:"

        if (extService.hasLogin) {
          // Можно логинится через указанный сервис. Организовать адаптер:
          val loginAdp: IExtLoginAdp = extService match {
            // ESIA login.
            case MExtServices.GosUslugi =>
              esiaLoginUtil

            // securesocial-логин для вк, фб, твиттера и прочего старья
            case _ =>
              ssLoginUtil.loginAdpFor( extService )
          }

          val svcJvm = MExtServicesJvm.forService( extService )

          val req1 = MLoginViaReq( loginAdp, svcJvm, mreq, mreq.user )
          block(req1)

        } else {
          // Нет логин-провайдера у данного сервиса.
          LOGGER.warn(s"$logPrefix Tried to login via $extService, but service does not have login provider.")
          errorHandler.onClientError(request, Status.NOT_FOUND)
        }
      }
    }
  }

}
