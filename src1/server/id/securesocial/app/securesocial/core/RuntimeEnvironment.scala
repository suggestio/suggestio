package securesocial.core

import javax.inject.Inject
import securesocial.core.providers._
import securesocial.core.services._

import scala.collection.immutable.ListMap

/**
 * A runtime environment where the services needed are available
 */
trait RuntimeEnvironment {
  def routes: RoutesService

  def httpService: HttpService
  def cacheService: CacheService

  def providers: Map[String, IdentityProvider]
}

class RuntimeEnvironments @Inject() (
                                      serviceInfoHelpers: ServiceInfoHelpers,
                                      oAuth2SettingsUtil: OAuth2SettingsUtil,
                                    ) {

  /**
   * A default runtime environment.  All built in services are included.
   * You can start your app with with by only adding a userService to handle users.
   */
  abstract class Default extends RuntimeEnvironment {

    protected def include(p: IdentityProvider) = p.id -> p
    protected def oauth1ClientFor(provider: String) = new OAuth1Client.Default(serviceInfoHelpers.forProvider(provider), httpService)
    protected def oauth2ClientFor(provider: String) = new OAuth2Client.Default(httpService, oAuth2SettingsUtil.forProvider(provider))

    override lazy val providers = ListMap(
      // oauth 2 client providers
      include(new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook))),
      //include(new FoursquareProvider(routes, cacheService, oauth2ClientFor(FoursquareProvider.Foursquare))),
      //include(new GitHubProvider(routes, cacheService, oauth2ClientFor(GitHubProvider.GitHub))),
      //include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google))),
      //include(new InstagramProvider(routes, cacheService, oauth2ClientFor(InstagramProvider.Instagram))),
      //include(new LinkedInOAuth2Provider(routes, cacheService,oauth2ClientFor(LinkedInOAuth2Provider.LinkedIn))),
      include(new VkProvider(routes, cacheService, oauth2ClientFor(VkProvider.Vk)))
      // oauth 1 client providers
      //include(new LinkedInProvider(routes, cacheService, oauth1ClientFor(LinkedInProvider.LinkedIn))),
      //include(new TwitterProvider(routes, cacheService, oauth1ClientFor(TwitterProvider.Twitter)))
      // username password
    )
  }

}
