package controllers.sc

import controllers.routes
import io.suggest.util.logs.IMacroLogs
import models.mctx.Context
import play.twirl.api.JavaScript
import util.acl.IIgnoreAuth
import views.js.sc.jsRouterTpl
import japgolly.univeq._

import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 17:22
 * Description: Раздача js-router'а выдачи всем страждущим.
 */
object ScJsRouter {

  def jsRouterCacheHash(implicit ctx: Context): Int = {
    // Рендер зависит только от констант и прочего DI-инстансов в контексте: request.host, .api и т.д.
    jsRouterCacheHash( jsRouterTpl() )
  }
  def jsRouterCacheHash(jsRouterTplJS: JavaScript): Int = {
    jsRouterCacheHash( jsRouterTplJS.body )
  }
  def jsRouterCacheHash(jsRouterTplJsStr: String): Int = {
    jsRouterTplJsStr.hashCode
  }

}


trait ScJsRouter
  extends ScController
  with IIgnoreAuth
  with IMacroLogs
{

  import mCommonDi._

  /**
    * Отрендерить js-код роутера вопрошающему.
    * Только ignoreAuth и всегда без CSRF, т.к. роутер раздаётся через CDN.
    *
    * @return 200 OK, text/javascript.
    */
  def scJsRouter = ignoreAuth() { implicit request =>
    // CSRF тут нельзя. Вообще. Это общий для всех js-роутер. Иначе это будет дыра в CSRF-защите.
    // TODO Нужно получать параметры кеширования через qs на клиенте и выдавать соответствующие заголовки кеширования.
    // TODO Выставлять заголовки ETag, Last-Modified.
    val cacheSeconds = if (isDev) 1 else 100
    Ok(jsRouterTpl())
      .withHeaders(
        CACHE_CONTROL -> ("public, max-age=" + cacheSeconds)
      )
  }


  /** Экшен реакции на запрос js-роутера.
    *
    * @param cachedHashCode Ключ кэша.
    * @return 200 Ok + код роутера.
    *         304 Not Modifier, если хэш совпадает.
    *         Редирект, если роутер кэшируется по другому адресу.
    */
  def scJsRouterCache(cachedHashCode: Int) = ignoreAuth() { implicit request =>
    val renderedHtml = jsRouterTpl()
    val renderedHtmlString = renderedHtml.body

    val realHashCode = ScJsRouter.jsRouterCacheHash( renderedHtmlString )

    lazy val logPrefix = s"scJsRouter($cachedHashCode):"

    if (cachedHashCode != realHashCode) {
      LOGGER.trace(s"$logPrefix Obsoleted hashCode $cachedHashCode, but current hash = $realHashCode. Redirecting")
      Redirect( routes.Sc.scJsRouterCache(realHashCode) )

    } else {
      val realHashCodeString = realHashCode.toString
      val realHashCodeStringQuouted = s""""$realHashCodeString""""
      val ifNoneMatchOpt = request.headers.get( IF_NONE_MATCH )

      if ( ifNoneMatchOpt.exists { inm =>
        (inm ==* realHashCodeString) || (inm ==* realHashCodeStringQuouted)
      }) {
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
