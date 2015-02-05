package controllers.ident

import controllers.SioController
import models.{ExternalCall, Context}
import models.usr.{SsUserService, SsUser}
import play.api.i18n.Messages
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import securesocial.controllers._
import securesocial.controllers.ProviderControllerHelper.toUrl
import securesocial.core.RuntimeEnvironment.Default
import securesocial.core.providers.VkProvider
import securesocial.core.services.{SaveMode, RoutesService, UserService}
import securesocial.core._
import util.PlayMacroLogsI
import util.acl.MaybeAuth

import scala.collection.immutable.ListMap
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.01.15 17:16
 * Description: Поддержка логина через соц.сети или иные внешние сервисы.
 */

trait ExternalLogin extends SioController with BaseProviderController[SsUser] with PlayMacroLogsI {

  /** secure-social настраивается через этот Enviroment. */
  override implicit val env: RuntimeEnvironment[SsUser] = {
    new Default[SsUser] {
      override lazy val routes: RoutesService = SsRoutesService
      override def userService: UserService[SsUser] = SsUserService
      override lazy val providers: ListMap[String, IdentityProvider] = {
        ListMap(
          include(new VkProvider(routes, cacheService, oauth2ClientFor(VkProvider.Vk)))
        )
      }
    }
  }


  // Код handleAuth() спасён из умирающего securesocial c целью отпиливания от грёбаных authentificator'ов,
  // которые по сути являются переусложнёнными stateful(!)-сессиями, которые придумал какой-то нехороший человек.

  override def handleAuth(provider: String, redirectTo: Option[String]) = MaybeAuth.async { implicit request =>
    env.providers.get(provider).map {
      _.authenticate().flatMap {
        case denied: AuthenticationResult.AccessDenied =>
          Future.successful(Redirect(env.routes.loginPageUrl).flashing("error" -> Messages("securesocial.login.accessDenied")))
        case failed: AuthenticationResult.Failed =>
          LOGGER.error(s"[securesocial] authentication failed, reason: ${failed.error}")
          throw new AuthenticationException()
        case flow: AuthenticationResult.NavigationFlow => Future.successful {
          redirectTo.map { url =>
            flow.result
              .addingToSession(SecureSocial.OriginalUrlKey -> url)
          } getOrElse flow.result
        }
        case authenticated: AuthenticationResult.Authenticated =>
          /*request.pwOpt match {
            // Юзер был анонимом на момент логина.
            case None =>*/
              val profile = authenticated.profile
              env.userService.find(profile.providerId, profile.userId).flatMap { maybeExisting =>
                val mode = if (maybeExisting.isDefined) SaveMode.LoggedIn else SaveMode.SignUp
                env.userService.save(authenticated.profile, mode).map { userForAction =>
                  LOGGER.debug(s"handleAuth2(): user completed authentication: provider = ${profile.providerId}, userId: ${profile.userId}, mode = $mode")
                  val evt = if (mode == SaveMode.LoggedIn) new LoginEvent(userForAction) else new SignUpEvent(userForAction)
                  val sessionAfterEvents = Events.fire(evt).getOrElse(request.session)
                  val session1 = cleanupSession(sessionAfterEvents) + (Security.username -> userForAction.personId)
                  Redirect(toUrl(sessionAfterEvents))
                    .withSession(session1)
                }
              }

            // Юзер был уже залогинен на моммент логина.
            /*case Some(pw) =>
              // TODO Линковать с оригинальной учёткой? Для этого нужно написать много букв, которые отработают MPerson.
              ???
              val modifiedSession = overrideOriginalUrl(request.session, redirectTo) + (Security.username -> pw.personId)
              Redirect(toUrl(modifiedSession))
                .withSession(cleanupSession(modifiedSession))
                .touchingAuthenticator(updatedAuthenticator)
              for (
                linked <- env.userService.link(currentUser, authenticated.profile);
                updatedAuthenticator <- request.authenticator.get.updateUser(linked);
                result <- {

                }
              ) yield {
                logger.debug(s"[securesocial] linked $currentUser to: providerId = ${authenticated.profile.providerId}")
                result
              }*/
         //}
      } recover {
        case e =>
          LOGGER.error("Unable to log user in. An exception was thrown", e)
          Redirect(env.routes.loginPageUrl)
            .flashing("error" -> Messages("securesocial.login.errorLoggingIn"))
      }
    } getOrElse {
      Future.successful(NotFound)
    }
  }

}

object SsRoutesService extends RoutesService.Default {

  override def absoluteUrl(call: Call)(implicit req: RequestHeader): String = {
    if(call.isInstanceOf[ExternalCall])
      call.url
    else
      Context.LK_URL_PREFIX + call.url
  }

  override def authenticationUrl(provider: String, redirectTo: Option[String])(implicit req: RequestHeader): String = {
    val relUrl = controllers.routes.Ident.authenticate(provider, redirectTo)
    absoluteUrl(relUrl)
  }

}
