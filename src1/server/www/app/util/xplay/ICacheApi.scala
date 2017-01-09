package util.xplay

import play.api.cache.CacheApi

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 12:08
 * Description: Интерфейс для контроллеров, шарящих DI-инстанс play-cache.
 */
trait ICacheApi {

  def cache: CacheApi

}
