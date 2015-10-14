package controllers.sc

import io.suggest.playx.ICurrentApp
import util.acl.MaybeAuth
import views.js.sc.jsRouterTpl
import play.api.Play

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 17:22
 * Description: Раздача js-router'а выдачи всем страждущим.
 */
trait ScJsRouter
  extends ScController
  with ICurrentApp
  with MaybeAuth
{

  /**
   * Отрендерить js-код роутера вопрошающему.
   * @return 200 OK, text/javascript.
   */
  def scJsRouter = MaybeAuth { implicit request =>
    // TODO Нужно получать параметры кеширования через qs на клиенте и выдавать соответствующие заголовки кеширования.
    // TODO Выставлять заголовки ETag, Last-Modified.
    val cacheSeconds = if (Play.isDev) 1 else 100
    Ok(jsRouterTpl()).withHeaders(
      CACHE_CONTROL -> ("public, max-age=" + cacheSeconds)
    )
  }

}
