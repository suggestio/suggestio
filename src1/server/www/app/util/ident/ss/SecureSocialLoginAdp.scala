package util.ident.ss

import controllers.routes
import io.suggest.ext.svc.MExtServices
import io.suggest.playx.ExternalCall
import io.suggest.util.logs.MacroLogsDyn
import javax.inject.{Inject, Singleton}
import models.mctx.ContextUtil
import models.mext.{ILoginProvider, MExtServicesJvm}
import models.usr._
import play.api.Configuration
import play.api.cache.AsyncCacheApi
import play.api.inject.Injector
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc._
import securesocial.core._
import securesocial.core.services.{CacheService, HttpService, RoutesService}
import util.ident.IdentUtil
import securesocial.controllers.{ProviderControllerHelper => SsHelper}

import scala.collection.immutable.ListMap
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.02.19 16:35
  * Description: Утиль для логина .
  */
@Singleton
class SecureSocialLoginAdp @Inject()(
                                      ssRuntimeEnvironments           : RuntimeEnvironments,
                                      identUtil                       : IdentUtil,
                                      configuration                   : Configuration,
                                      injector                        : Injector,
                                      implicit private val ec         : ExecutionContext,
                                    )
  extends MacroLogsDyn
{

  /** Фильтровать присылаемый ttl. */
  lazy val MAX_SESSION_TTL_SECONDS = {
    configuration.getOptional[Int]("login.ext.session.ttl.max.minutes")
      .fold(3.day)(_.minutes)
      .toSeconds
  }

  /** secure-social настраивается через этот Enviroment. */
  def env: RuntimeEnvironment[SsUser] = {
    new ssRuntimeEnvironments.Default[SsUser] {
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
                .loginProvider
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

  def clearSession(s: Session): Session =
    SsHelper.cleanupSession( s )

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
    val prov = ILoginProvider.maybeWithName(providerId).get
    val relUrl = routes.Ident.idViaProvider(prov, redirectTo)
    absoluteUrl( relUrl )
  }

  override def loginPageUrl(implicit req: RequestHeader): String = {
    val call = routes.Ident.emailPwLoginForm()
    absoluteUrl( call )
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

