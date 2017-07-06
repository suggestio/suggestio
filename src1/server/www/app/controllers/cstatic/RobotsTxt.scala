package controllers.cstatic

import controllers.SioController
import util.acl.IIgnoreAuth
import views.txt.static.robotsTxtTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 11:18
 * Description: Трейт для контроллеров с экшеном поддержки раздачи robots.txt.
 */
trait RobotsTxt extends SioController with IIgnoreAuth {

  import mCommonDi._

  /** Время кеширования /robots.txt ответа на клиенте. */
  private val ROBOTS_TXT_CACHE_TTL_SECONDS: Int = {
    if (isDev) 5 else 120
  }

  /** Раздача содержимого robots.txt. */
  def robotsTxt = ignoreAuth() { implicit request =>
    Ok( robotsTxtTpl() )
      .withHeaders(
        //CONTENT_TYPE  -> "text/plain; charset=utf-8",
        CACHE_CONTROL -> s"public, max-age=$ROBOTS_TXT_CACHE_TTL_SECONDS"
      )
  }

}
