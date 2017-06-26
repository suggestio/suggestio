package securesocial.core

import securesocial.core.authenticator._
import securesocial.core.providers._
import securesocial.core.services._

import scala.collection.immutable.ListMap

/**
 * A runtime environment where the services needed are available
 */
trait RuntimeEnvironment[U] {
  def routes: RoutesService

  def httpService: HttpService
  def cacheService: CacheService

  def providers: Map[String, IdentityProvider]

  def idGenerator: IdGenerator
  def authenticatorService: AuthenticatorService[U]

  def eventListeners: List[EventListener[U]]

  def userService: UserService[U]
}

object RuntimeEnvironment {

  /**
   * A default runtime environment.  All built in services are included.
   * You can start your app with with by only adding a userService to handle users.
   */
  abstract class Default[U] extends RuntimeEnvironment[U] {
    override lazy val httpService: HttpService = new HttpService.Default()
    override lazy val cacheService: CacheService = new CacheService.Default()
    override lazy val idGenerator: IdGenerator = new IdGenerator.Default()

    override lazy val authenticatorService = new AuthenticatorService(
      new CookieAuthenticatorBuilder[U](new AuthenticatorStore.Default(cacheService), idGenerator),
      new HttpHeaderAuthenticatorBuilder[U](new AuthenticatorStore.Default(cacheService), idGenerator)
    )

    override lazy val eventListeners: List[EventListener[U]] = List()

    protected def include(p: IdentityProvider) = p.id -> p
    protected def oauth1ClientFor(provider: String) = new OAuth1Client.Default(ServiceInfoHelper.forProvider(provider), httpService)
    protected def oauth2ClientFor(provider: String) = new OAuth2Client.Default(httpService, OAuth2Settings.forProvider(provider))

    override lazy val providers = ListMap(
      // oauth 2 client providers
      include(new FacebookProvider(routes, cacheService, oauth2ClientFor(FacebookProvider.Facebook))),
      //include(new FoursquareProvider(routes, cacheService, oauth2ClientFor(FoursquareProvider.Foursquare))),
      //include(new GitHubProvider(routes, cacheService, oauth2ClientFor(GitHubProvider.GitHub))),
      //include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google))),
      //include(new InstagramProvider(routes, cacheService, oauth2ClientFor(InstagramProvider.Instagram))),
      //include(new LinkedInOAuth2Provider(routes, cacheService,oauth2ClientFor(LinkedInOAuth2Provider.LinkedIn))),
      include(new VkProvider(routes, cacheService, oauth2ClientFor(VkProvider.Vk)))
      //include(new DropboxProvider(routes, cacheService, oauth2ClientFor(DropboxProvider.Dropbox))),
      // oauth 1 client providers
      //include(new LinkedInProvider(routes, cacheService, oauth1ClientFor(LinkedInProvider.LinkedIn))),
      //include(new TwitterProvider(routes, cacheService, oauth1ClientFor(TwitterProvider.Twitter)))
      // username password
    )
  }
}
