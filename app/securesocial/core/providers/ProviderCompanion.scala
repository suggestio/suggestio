package securesocial.core.providers

import securesocial.core.{IdentityProvider, OAuth2Client}
import securesocial.core.services.{CacheService, RoutesService}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 14:53
 * Description: Intrerface for providers objects.
 */
trait OAuth2ProviderCompanion {

  def apply(routesService: RoutesService, cacheService: CacheService, client: OAuth2Client): IdentityProvider

  def name: String
}
