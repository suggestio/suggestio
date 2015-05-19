package controllers.sc

import util.acl.MaybeAuth
import views.js.sc.jsRouterTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 17:22
 * Description: Раздача js-router'а выдачи всем страждущим.
 */
trait ScJsRouter extends ScController {

  /**
   * Отрендерить js-код роутера вопрошающему.
   * @return 200 OK, text/javascript.
   */
  def scJsRouter = MaybeAuth { implicit request =>
    // TODO Нужно получать параметры кеширования на клиенте и выдавать соответствующие заголовки кеширования.
    Ok(jsRouterTpl()).withHeaders(
      CACHE_CONTROL -> ("public, max-age=" + 100)
    )
  }

}
