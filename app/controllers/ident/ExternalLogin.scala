package controllers.ident

import controllers.SioController
import models.{ExternalCall, Context}
import models.usr._
import play.api.i18n.Messages
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import securesocial.controllers.ProviderControllerHelper._
import securesocial.core.RuntimeEnvironment.Default
import securesocial.core.providers.VkProvider
import securesocial.core.services.{RoutesService, UserService}
import securesocial.core._
import util.PlayMacroLogsI
import util.acl.MaybeAuth
import util.SiowebEsUtil.client

import scala.collection.immutable.ListMap
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.01.15 17:16
 * Description: Поддержка логина через соц.сети или иные внешние сервисы.
 */

trait ExternalLogin extends SioController with PlayMacroLogsI {

  /** secure-social настраивается через этот Enviroment. */
  implicit val env: RuntimeEnvironment[SsUser] = {
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

  def idViaProvider(provider: IdProvider, r: Option[String]) = handleAuth1(provider, r)
  def idViaProviderByPost(provider: IdProvider, r: Option[String]) = handleAuth1(provider, r)

  // Код handleAuth() спасён из умирающего securesocial c целью отпиливания от грёбаных authentificator'ов,
  // которые по сути являются переусложнёнными stateful(!)-сессиями, которые придумал какой-то нехороший человек.

  protected def handleAuth1(provider: IdProvider, redirectTo: Option[String]) = MaybeAuth.async { implicit request =>
    env.providers.get(provider.strId).map {
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
          val profile = authenticated.profile
          MExtIdent.getByUserIdProv(provider, profile.userId).flatMap { maybeExisting =>
            val saveFut: Future[MExtIdent] = maybeExisting match {
              case None =>
                MPerson(lang = request2lang.code).save.flatMap { personId =>
                  // Сохранить данные идентификации через соц.сеть.
                  val mei = MExtIdent(
                    personId  = personId,
                    provider  = provider,
                    userId    = profile.userId,
                    email     = profile.email
                  )
                  mei.save
                    .map { savedId => mei }
                }

              case Some(ident) =>
                Future successful ident
            }
            saveFut.map { ident =>
              LOGGER.debug(s"handleAuth2(): user completed authentication: provider = ${profile.providerId}, userId: ${profile.userId}")
              val session1 = cleanupSession(request.session) + (Security.username -> ident.personId)
              Redirect(toUrl(request.session))
                .withSession(session1)
            }
          }

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

  override def authenticationUrl(providerId: String, redirectTo: Option[String])(implicit req: RequestHeader): String = {
    val prov = IdProviders.withName(providerId)
    val relUrl = controllers.routes.Ident.idViaProvider(prov, redirectTo)
    absoluteUrl(relUrl)
  }

}
