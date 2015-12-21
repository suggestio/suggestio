package controllers.cstatic

import play.api.Play
import util.acl.IgnoreAuth
import views.txt.static.robotsTxtTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 11:18
 * Description: Трейт для контроллеров с экшеном поддержки раздачи robots.txt.
 */
trait RobotsTxt extends IgnoreAuth {

  import mCommonDi._

  /** Время кеширования /robots.txt ответа на клиенте. */
  private val ROBOTS_TXT_CACHE_TTL_SECONDS: Int = {
    configuration.getInt("robots.txt.cache.ttl.seconds") getOrElse {
      if (Play.isDev) 5 else 120
    }
  }

  /** Раздача содержимого robots.txt. */
  def robotsTxt = IgnoreAuth { implicit request =>
    Ok( robotsTxtTpl() )
      .withHeaders(
        CONTENT_TYPE  -> "text/plain; charset=utf-8",
        CACHE_CONTROL -> s"public, max-age=$ROBOTS_TXT_CACHE_TTL_SECONDS"
      )
  }

}
