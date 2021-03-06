package util.ident.ss

import controllers.routes
import io.suggest.auth.AuthenticationResult
import io.suggest.ext.svc.{MExtService, MExtServices}
import io.suggest.playx.ExternalCall
import io.suggest.util.logs.MacroLogsDyn
import javax.inject.Inject
import models.mctx.ContextUtil
import models.mext.MExtServicesJvm
import models.req.MLoginViaReq
import play.api.cache.AsyncCacheApi
import play.api.inject.Injector
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc._
import securesocial.core._
import securesocial.core.services.{CacheService, HttpService, RoutesService}
import util.ident.IExtLoginAdp
import securesocial.controllers.{ProviderControllerHelper => SsHelper}
import japgolly.univeq._

import scala.collection.immutable.ListMap
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.reflect.ClassTag

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.02.19 16:35
  * Description: Утиль для логина .
  */
final class SecureSocialLoginUtil @Inject()(
                                             injector                        : Injector,
                                           )
  extends MacroLogsDyn
{

  /** secure-social настраивается через этот Enviroment. */
  lazy val env: RuntimeEnvironment = {
    // Только ленивая инжекция, т.к. securesocial уходит в прошлое, и это всё не актуально.
    val ssRuntimeEnvironments = injector.instanceOf[RuntimeEnvironments]
    new ssRuntimeEnvironments.Default {
      override lazy val cacheService = injector.instanceOf[SsCacheService]
      override lazy val httpService = injector.instanceOf[SsHttpService]
      override lazy val routes = injector.instanceOf[SsRoutesService]
      override lazy val providers: ListMap[String, IdentityProvider] = {
        // Аккуратная инициализация доступных провайдеров и без дубликации кода.
        val provs = (
          for {
            service <- MExtServices.values.iterator
            prov <- {
              MExtServicesJvm
                .forService( service )
                .ssLoginProvider
                .iterator
            }
            idp <- {
              val provSt = injector.instanceOf( prov.ssProviderClass )
              try {
                provSt(routes, cacheService, httpService) :: Nil
              } catch {
                case ex: Throwable =>
                  LOGGER.warn("Cannot initialize " + provSt.getClass.getSimpleName, ex)
                  Nil
              }
            }
          } yield {
            include(idp)
          }
        )
          .toSeq

        ListMap(provs: _*)
      }
    }
  }


  case class loginAdpFor( extService: MExtService ) extends IExtLoginAdp {

    /** Фильтровать присылаемый ttl. */
    override def MAX_SESSION_TTL_SECONDS = 3.days.toSeconds

    override def clearSession(s: Session): Session =
      SsHelper.cleanupSession( s )

    override def authenticateFromRequest()(implicit req: MLoginViaReq[AnyContent]): Future[AuthenticationResult] = {
      (for {
        ssLoginProv <- req.svcJvm.ssLoginProvider
        ssIdProv <- env.providers.get( ssLoginProv.ssProvName )
      } yield {
        ssIdProv.authenticate()
      }).getOrElse {
        LOGGER.error(s"authenticateFromRequest($extService): No login provider exists")
        Future.failed( new NoSuchElementException )
      }
    }

  }

}


class SsRoutesService @Inject() (ctxUtil: ContextUtil) extends RoutesService {

  private def absoluteUrl(call: Call)(implicit req: RequestHeader): String = {
    if(call.isInstanceOf[ExternalCall])
      call.url
    else
      ctxUtil.LK_URL_PREFIX + call.url
  }

  override def authenticationUrl(providerId: String, redirectTo: Option[String])
                                (implicit req: RequestHeader): String = {
    val svc = (for {
      svc <- MExtServices.values.iterator
      svcJvm = MExtServicesJvm.forService( svc )
      idp <- svcJvm.ssLoginProvider
      if idp.ssProvName ==* providerId
    } yield {
      svc
    })
      .next()
    val relUrl = routes.Ident.idViaProvider(svc, redirectTo)
    absoluteUrl( relUrl )
  }

}


class SsHttpService @Inject() (ws: WSClient) extends HttpService {
  override def url(url: String): WSRequest = ws.url(url)
}


class SsCacheService @Inject() (cacheApi: AsyncCacheApi) extends CacheService {

  override def set[T](key: String, value: T, ttlInSeconds: Int): Future[_] = {
    cacheApi.set(key, value, expiration = ttlInSeconds.seconds)
  }

  override def getAs[T](key: String)(implicit ct: ClassTag[T]): Future[Option[T]] = {
    cacheApi.get[T](key)
  }

  override def remove(key: String): Future[_] = {
    cacheApi.remove(key)
  }

}

