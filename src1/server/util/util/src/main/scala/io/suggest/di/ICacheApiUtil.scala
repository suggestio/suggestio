package io.suggest.di

import io.suggest.playx.CacheApiUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.15 13:00
 * Description: Интерфейс для доступа к DI-полю с экземпляром [[io.suggest.playx.CacheApiUtil]].
 */
trait ICacheApiUtil {

  def cacheApiUtil: CacheApiUtil

}
