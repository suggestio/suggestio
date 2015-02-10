package securesocial.core.providers

import securesocial.core._
import securesocial.core.services.{HttpService, CacheService, RoutesService}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 14:53
 * Description: Intrerface for providers objects.
 */

trait ProviderCompanion {
  def apply(routesService: RoutesService, cacheService: CacheService, httpService: HttpService): IdentityProvider
  def name: String
}


trait OAuth2ProviderCompanion extends ProviderCompanion {

  def apply(routesService: RoutesService, cacheService: CacheService, client: OAuth2Client): IdentityProvider

  override def apply(routesService: RoutesService, cacheService: CacheService, httpService: HttpService): IdentityProvider = {
    val cl = new OAuth2Client.Default(httpService, OAuth2Settings.forProvider(name))
    apply(routesService, cacheService, cl)
  }
}


trait OAuth1ProviderCompanion extends ProviderCompanion {

  def apply(routesService: RoutesService, cacheService: CacheService, client: OAuth1Client): IdentityProvider

  override def apply(routesService: RoutesService, cacheService: CacheService, httpService: HttpService): IdentityProvider = {
    val cl = new OAuth1Client.Default(ServiceInfoHelper.forProvider(name), httpService)
    apply(routesService, cacheService, cl)
  }
}
