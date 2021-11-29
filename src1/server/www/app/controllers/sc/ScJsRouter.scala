package controllers.sc

import io.suggest.util.logs.MacroLogsImplLazy
import models.mctx.Context
import util.acl.{IgnoreAuth, SioControllerApi}
import views.js.sc.ScJsRouterTpl
import japgolly.univeq._

import javax.inject.Inject
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 17:22
 * Description: Раздача js-router'а выдачи всем страждущим.
 */
final class ScJsRouter @Inject() (
                                   sioControllerApi     : SioControllerApi,
                                 )
  extends MacroLogsImplLazy
{

  import sioControllerApi._

  private lazy val scJsRouterTpl = injector.instanceOf[ScJsRouterTpl]
  private lazy val ignoreAuth = injector.instanceOf[IgnoreAuth]

  def jsRouterBody(implicit ctx: Context): String =
    scJsRouterTpl().body

  def jsRouterCacheHash(implicit ctx: Context): Int = {
    // Рендер зависит только от констант и прочего DI-инстансов в контексте: request.host, .api и т.д.
    jsRouterCacheHash( jsRouterBody )
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
    val renderedJsString = jsRouterBody
    val realHashCode = jsRouterCacheHash( renderedJsString )

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
        Ok( renderedJsString )
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
