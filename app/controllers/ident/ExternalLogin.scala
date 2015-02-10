package controllers.ident

import controllers.{routes, SioController}
import models.{ExtRegConfirmForm_t, ExternalCall, Context}
import models.usr._
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.twirl.api.Html
import securesocial.controllers.ProviderControllerHelper._
import securesocial.core.RuntimeEnvironment.Default
import securesocial.core.providers.{TwitterProvider, FacebookProvider, VkProvider}
import securesocial.core.services.{RoutesService, UserService}
import securesocial.core._
import util.adn.NodesUtil
import util.{PlayMacroLogsDyn, FormUtil, PlayMacroLogsI}
import util.acl.{AbstractRequestWithPwOpt, CanConfirmIdpRegPost, CanConfirmIdpRegGet, MaybeAuth}
import util.SiowebEsUtil.client
import util.ident.IdentUtil
import views.html.ident.mySioStartTpl
import views.html.ident.reg._
import views.html.ident.reg.ext._

import scala.collection.immutable.ListMap
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.01.15 17:16
 * Description: Поддержка логина через соц.сети или иные внешние сервисы.
 */

object ExternalLogin extends PlayMacroLogsDyn {

  /** secure-social настраивается через этот Enviroment. */
  implicit protected val env: RuntimeEnvironment[SsUser] = {
    new Default[SsUser] {
      override lazy val routes: RoutesService = SsRoutesService
      override def userService: UserService[SsUser] = SsUserService
      override lazy val providers: ListMap[String, IdentityProvider] = {
        // Аккуратная инициализация доступных провайдеров и без дубликации кода.
        val provs = Iterator(VkProvider, FacebookProvider, TwitterProvider)
          .flatMap { provSt =>
            try {
              Seq( provSt(routes, cacheService, httpService) )
            } catch {
              case ex: Throwable =>
                LOGGER.warn("Cannot initialize " + provSt.getClass.getSimpleName, ex)
                Nil
            }
          }
          .map(include)
          .toSeq
        ListMap(provs : _*)
      }
    }
  }

  /**
   * Извлечь из сессии исходную ссылку для редиректа.
   * Если ссылки нет, то отправить в ident-контроллер.
   * @param ses Сессия.
   * @param personId id залогиненного юзера.
   * @return Ссылка в виде строки.
   */
  def toUrl2(ses: Session, personId: String): Future[String] = {
    ses.get(SecureSocial.OriginalUrlKey) match {
      case Some(url) =>
        Future successful url
      case None =>
      IdentUtil.redirectCallUserSomewhere(personId)
        .map(_.url)
    }
  }


  /** Маппинг формы подтверждения регистрации через id-провайдера. */
  def extRegConfirmFormM: ExtRegConfirmForm_t = {
    Form(
      "nodeName" -> FormUtil.nameM
    )
  }

}

import ExternalLogin._

trait ExternalLogin extends SioController with PlayMacroLogsI {

  /**
   * GET-запрос идентификации через внешнего провайдера.
   * @param provider провайдер идентификации.
   * @param r Обратный редирект.
   * @return Redirect.
   */
  def idViaProvider(provider: IdProvider, r: Option[String]) = handleAuth1(provider, r)

  /**
   * POST-запрос идентификации через внешнего провайдера.
   * @param provider Провайдер идентификации.
   * @param r Редирект обратно.
   * @return Redirect.
   */
  def idViaProviderByPost(provider: IdProvider, r: Option[String]) = handleAuth1(provider, r)

  // Код handleAuth() спасён из умирающего securesocial c целью отпиливания от грёбаных authentificator'ов,
  // которые по сути являются переусложнёнными stateful(!)-сессиями, которые придумал какой-то нехороший человек.
  protected def handleAuth1(provider: IdProvider, redirectTo: Option[String]) = MaybeAuth.async { implicit request =>
    lazy val logPrefix = s"handleAuth1($provider):"
    env.providers.get(provider.strId).map {
      _.authenticate().flatMap {
        case denied: AuthenticationResult.AccessDenied =>
          val res = Redirect( routes.Ident.mySioStartPage() )
            .flashing("error" -> Messages("securesocial.login.accessDenied"))
          Future successful res
        case failed: AuthenticationResult.Failed =>
          LOGGER.error(s"$logPrefix authentication failed, reason: ${failed.error}")
          throw new AuthenticationException()
        case flow: AuthenticationResult.NavigationFlow => Future.successful {
          redirectTo.map { url =>
            flow.result
              .addingToSession(SecureSocial.OriginalUrlKey -> url)
          } getOrElse flow.result
        }
        case authenticated: AuthenticationResult.Authenticated =>
          // TODO Отрабатывать случаи, когда юзер уже залогинен под другим person_id.
          val profile = authenticated.profile
          MExtIdent.getByUserIdProv(provider, profile.userId).flatMap { maybeExisting =>
            val saveFut: Future[(MExtIdent, Boolean)] = maybeExisting match {
              case None =>
                MPerson(lang = request2lang.code).save.flatMap { personId =>
                  // Сохранить данные идентификации через соц.сеть.
                  val mei = MExtIdent(
                    personId  = personId,
                    provider  = provider,
                    userId    = profile.userId,
                    email     = profile.email
                  )
                  val save2Fut = mei.save
                  LOGGER.debug(s"$logPrefix Registered new user $personId from ext.login service, remote user_id = ${profile.userId}")
                  save2Fut.map { savedId => mei -> true }
                }

              case Some(ident) =>
                LOGGER.trace(s"$logPrefix Existing user[${ident.personId}] logged-in from ${profile.userId}")
                Future successful (ident -> false)
            }
            saveFut.flatMap { case (ident, isNew) =>
              val rdrFut: Future[Result] = if (isNew) {
                Redirect(routes.Ident.idpConfirm())
              } else {
                val rdrUrlFut = toUrl2(request.session, ident.personId)
                rdrUrlFut map { url =>
                  Redirect(url)
                }
              }
              val session1 = cleanupSession(request.session) + (Security.username -> ident.personId)
              rdrFut
                .map { _.withSession(session1) }
            }
          }

      } recover {
        case e =>
          LOGGER.error("Unable to log user in. An exception was thrown", e)
          Redirect(routes.Ident.mySioStartPage())
            .flashing("error" -> Messages("securesocial.login.errorLoggingIn"))
      }
    } getOrElse {
      Future.successful(NotFound)
    }
  }


  /**
   * Юзер, залогинившийся через провайдера, хочет создать ноду.
   * @return Страницу с колонкой подтверждения реги.
   */
  def idpConfirm = CanConfirmIdpRegGet { implicit request =>
    val form = extRegConfirmFormM
    Ok( _idpConfirm(form) )
  }

  /** Общий код рендера idpConfig вынесен сюда. */
  protected def _idpConfirm(form: ExtRegConfirmForm_t)(implicit request: AbstractRequestWithPwOpt[_]): Html = {
    val ctx = implicitly[Context]
    val colHtml = _columnTpl(form)(ctx)
    mySioStartTpl(Seq(colHtml))(ctx)
  }

  /** Сабмит формы подтверждения регистрации через внешнего провайдера идентификации. */
  def idpConfirmSubmit = CanConfirmIdpRegPost.async { implicit request =>
    extRegConfirmFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug("idpConfirmSubmit(): Failed to bind form:\n " + formatFormErrors(formWithErrors))
        NotAcceptable( _idpConfirm(formWithErrors) )
      },
      {nodeName =>
        // Развернуть узел для юзера, отобразить страницу успехоты.
        NodesUtil.createUserNode(name = nodeName, personId = request.pwOpt.get.personId) map { adnNode =>
          Ok(regSuccessTpl(adnNode))
        }
      }
    )
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
    val relUrl = routes.Ident.idViaProvider(prov, redirectTo)
    absoluteUrl(relUrl)
  }

  override def loginPageUrl(implicit req: RequestHeader): String = {
    absoluteUrl( routes.Ident.emailPwLoginForm() )
  }
}
