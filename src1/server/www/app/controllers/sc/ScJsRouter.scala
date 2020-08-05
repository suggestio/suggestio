package controllers.sc

import controllers.SioControllerApi
import io.suggest.util.logs.MacroLogsImplLazy
import models.mctx.Context
import util.acl.IgnoreAuth
import views.js.sc.ScJsRouterTpl
import japgolly.univeq._
import javax.inject.{Inject, Singleton}

import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 17:22
 * Description: Раздача js-router'а выдачи всем страждущим.
 */
@Singleton
final class ScJsRouter @Inject() (
                                   ignoreAuth           : IgnoreAuth,
                                   sioControllerApi     : SioControllerApi,
                                 )
  extends MacroLogsImplLazy
{

  import sioControllerApi._
  import mCommonDi.current.injector

  private lazy val scJsRouterTpl = injector.instanceOf[ScJsRouterTpl]


  def jsRouterCacheHash(implicit ctx: Context): Int = {
    // Рендер зависит только от констант и прочего DI-инстансов в контексте: request.host, .api и т.д.
    jsRouterCacheHash( scJsRouterTpl().body )
  }
  def jsRouterCacheHash(jsRouterTplBody: String): Int = {
    jsRouterTplBody.hashCode
  }


  /** Экшен реакции на запрос js-роутера.
    *
    * @param cachedHashCode Ключ кэша.
    * @return 200 Ok + код роутера.
    *         304 Not Modifier, если хэш совпадает.
    *         Редирект, если роутер кэшируется по другому адресу.
    */
  def scJsRouterCache(cachedHashCode: Int) = ignoreAuth() { implicit request =>
    val renderedHtml = scJsRouterTpl()
    val renderedHtmlString = renderedHtml.body

    val realHashCode = jsRouterCacheHash( renderedHtmlString )

    lazy val logPrefix = s"scJsRouter($cachedHashCode):"

    if (cachedHashCode != realHashCode) {
      LOGGER.trace(s"$logPrefix Obsoleted hashCode $cachedHashCode, but current hash = $realHashCode. Redirecting")
      Redirect( controllers.sc.routes.ScJsRouter.scJsRouterCache(realHashCode) )

    } else {
      val realHashCodeString = realHashCode.toString
      val realHashCodeStringQuouted = s""""$realHashCodeString""""
      val ifNoneMatchOpt = request.headers.get( IF_NONE_MATCH )

      if (
        ifNoneMatchOpt.exists { inm =>
          (inm ==* realHashCodeString) ||
          (inm ==* realHashCodeStringQuouted)
        }
      ) {
        LOGGER.trace(s"$logPrefix 304, $ifNoneMatchOpt == $realHashCodeString")
        NotModified

      } else {
        Ok( renderedHtmlString )
          .as( JAVASCRIPT )
          .withHeaders(
            ETAG          -> realHashCodeStringQuouted,
            // TODO По мере стабилизации, увеличить max-age до года:
            CACHE_CONTROL -> s"public, max-age=${1.day.toSeconds}, immutable"
          )
      }
    }
  }

}
