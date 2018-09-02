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

  def oAuth2SettingsUtil: OAuth2SettingsUtil

  def apply(routesService: RoutesService, cacheService: CacheService, client: OAuth2Client): IdentityProvider

  override def apply(routesService: RoutesService, cacheService: CacheService, httpService: HttpService): IdentityProvider = {
    val cl = new OAuth2Client.Default(httpService, oAuth2SettingsUtil.forProvider(name))
    apply(routesService, cacheService, cl)
  }
}
