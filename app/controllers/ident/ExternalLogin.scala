package controllers.ident

import com.google.inject.Inject
import controllers.{IExecutionContext, IEsClient, routes, SioController}
import io.suggest.playx.ICurrentApp
import models.mext.{MExtServices, ILoginProvider}
import models.msession.{CustomTtl, Keys}
import models.{MNode, MNodeTypes, ExtRegConfirmForm_t, ExternalCall, Context}
import models.usr._
import play.api.data.Form
import play.api.mvc._
import play.api.Play.{current, configuration}
import play.twirl.api.Html
import securesocial.controllers.ProviderControllerHelper._
import securesocial.core.RuntimeEnvironment.Default
import securesocial.core.services.RoutesService
import securesocial.core._
import util.adn.NodesUtil
import util.xplay.SetLangCookieUtil
import util.{PlayMacroLogsDyn, FormUtil, PlayMacroLogsI}
import util.acl.{AbstractRequestWithPwOpt, CanConfirmIdpRegPost, CanConfirmIdpRegGet, MaybeAuth}
import util.ident.IdentUtil
import views.html.ident.reg._
import views.html.ident.reg.ext._

import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.01.15 17:16
 * Description: Поддержка логина через соц.сети или иные внешние сервисы.
 */

class ExternalLogin_ @Inject() (
  routesSvc                       : SsRoutesService,
  override implicit val ec        : ExecutionContext
)
  extends PlayMacroLogsDyn
  with IExecutionContext
{

  /** Фильтровать присылаемый ttl. */
  val MAX_SESSION_TTL_SECONDS = {
    configuration.getInt("login.ext.session.ttl.max.minutes")
      .getOrElse(86400)
      .minutes
      .toSeconds
  }

  /** secure-social настраивается через этот Enviroment. */
  implicit val env: RuntimeEnvironment[SsUser] = {
    new Default[SsUser] {
      override lazy val routes = routesSvc
      override def userService = SsUserService
      override lazy val providers: ListMap[String, IdentityProvider] = {
        // Аккуратная инициализация доступных провайдеров и без дубликации кода.
        val provs = MExtServices.values
          .iterator
          .flatMap { _.loginProvider }
          .flatMap { prov =>
            val provSt = prov.ssProvider
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
    ses.get(Keys.OrigUrl.name) match {
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


trait ExternalLogin
  extends SioController
  with PlayMacroLogsI
  with SetLangCookieUtil
  with ICurrentApp
  with IEsClient
{

  /** Доступ к DI-инстансу */
  val externalLogin: ExternalLogin_ = current.injector.instanceOf[ExternalLogin_]

  import externalLogin.env

  /**
   * GET-запрос идентификации через внешнего провайдера.
   * @param provider провайдер идентификации.
   * @param r Обратный редирект.
   * @return Redirect.
   */
  def idViaProvider(provider: ILoginProvider, r: Option[String]) = handleAuth1(provider, r)

  /**
   * POST-запрос идентификации через внешнего провайдера.
   * @param provider Провайдер идентификации.
   * @param r Редирект обратно.
   * @return Redirect.
   */
  def idViaProviderByPost(provider: ILoginProvider, r: Option[String]) = handleAuth1(provider, r)

  // Код handleAuth() спасён из умирающего securesocial c целью отпиливания от грёбаных authentificator'ов,
  // которые по сути являются переусложнёнными stateful(!)-сессиями, которые придумал какой-то нехороший человек.
  protected def handleAuth1(provider: ILoginProvider, redirectTo: Option[String]) = MaybeAuth.async { implicit request =>
    lazy val logPrefix = s"handleAuth1($provider):"
    env.providers.get(provider.ssProvName).map {
      _.authenticate().flatMap {
        case denied: AuthenticationResult.AccessDenied =>
          val res = Redirect( routes.Ident.mySioStartPage() )
            .flashing(FLASH.ERROR -> "securesocial.login.accessDenied")
          Future successful res
        case failed: AuthenticationResult.Failed =>
          LOGGER.error(s"$logPrefix authentication failed, reason: ${failed.error}")
          throw new AuthenticationException()
        case flow: AuthenticationResult.NavigationFlow => Future.successful {
          redirectTo.map { url =>
            flow.result
              .addingToSession(Keys.OrigUrl.name -> url)
          } getOrElse flow.result
        }
        case authenticated: AuthenticationResult.Authenticated =>
          // TODO Отрабатывать случаи, когда юзер уже залогинен под другим person_id.
          val profile = authenticated.profile
          MExtIdent.getByUserIdProv(provider, profile.userId).flatMap { maybeExisting =>
            // Сохраняем, если требуется. В результате приходит также новосохранный person MNode.
            val saveFut: Future[(MExtIdent, Option[MNode])] = maybeExisting match {
              case None =>
                val mperson0 = MNode.applyPerson(lang = request2lang.code)
                val mpersonSaveFut = mperson0.save
                val meiFut = mpersonSaveFut.flatMap { personId =>
                  // Сохранить данные идентификации через соц.сеть.
                  val mei = MExtIdent(
                    personId  = personId,
                    provider  = provider,
                    userId    = profile.userId,
                    email     = profile.email
                  )
                  val save2Fut = mei.save
                  LOGGER.debug(s"$logPrefix Registered new user $personId from ext.login service, remote user_id = ${profile.userId}")
                  save2Fut.map { savedId => mei }
                }
                val mpersonFut = mpersonSaveFut.map { personId =>
                  mperson0.copy(id = Some(personId))
                }
                for {
                  mei       <- meiFut
                  mperson   <- mpersonFut
                } yield {
                  (mei, Some(mperson))
                }

              // Регистрация юзера не требуется. Возвращаем то, что есть в наличии.
              case Some(ident) =>
                LOGGER.trace(s"$logPrefix Existing user[${ident.personId}] logged-in from ${profile.userId}")
                Future successful (ident -> None)
            }
            saveFut.flatMap { case (ident, newMpersonOpt) =>
              // Можно перенести внутрь match всю эту логику. Т.к. она очень предсказуема. Но это наверное ещё добавит сложности кода.
              val mpersonOptFut = newMpersonOpt match {
                case None =>
                  MNode.getByIdType(ident.personId, MNodeTypes.Person)
                case some =>
                  Future successful some
              }
              val isNew = newMpersonOpt.isDefined
              val rdrFut: Future[Result] = if (isNew) {
                Redirect(routes.Ident.idpConfirm())
              } else {
                val rdrUrlFut = externalLogin.toUrl2(request.session, ident.personId)
                rdrUrlFut map { url =>
                  Redirect(url)
                }
              }
              // Сборка новой сессии: чистка исходника, добавление новых ключей, относящихся к идентификации.
              var addToSessionAcc: List[(String, String)] = List(Keys.PersonId.name -> ident.personId)
              addToSessionAcc = authenticated.profile.oAuth2Info
                .flatMap { _.expiresIn }
                .filter { _ <= externalLogin.MAX_SESSION_TTL_SECONDS }
                .map { ein => CustomTtl(ein.toLong).addToSessionAcc(addToSessionAcc) }
                .getOrElse { addToSessionAcc }
              val session1 = addToSessionAcc.foldLeft(cleanupSession(request.session))(_ + _)
              val resFut = rdrFut
                .map { _.withSession(session1) }
              setLangCookie2(resFut, mpersonOptFut)
            }
          }

      } recover {
        case e =>
          LOGGER.error("Unable to log user in. An exception was thrown", e)
          Redirect(routes.Ident.mySioStartPage())
            .flashing(FLASH.ERROR -> "securesocial.login.errorLoggingIn")
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
    val form = externalLogin.extRegConfirmFormM
    Ok( _idpConfirm(form) )
  }

  /** Общий код рендера idpConfig вынесен сюда. */
  protected def _idpConfirm(form: ExtRegConfirmForm_t)(implicit request: AbstractRequestWithPwOpt[_]): Html = {
    confirmTpl(form)
  }

  /** Сабмит формы подтверждения регистрации через внешнего провайдера идентификации. */
  def idpConfirmSubmit = CanConfirmIdpRegPost.async { implicit request =>
    externalLogin.extRegConfirmFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug("idpConfirmSubmit(): Failed to bind form:\n " + formatFormErrors(formWithErrors))
        NotAcceptable( _idpConfirm(formWithErrors) )
      },
      {nodeName =>
        // Развернуть узел для юзера, отобразить страницу успехоты.
        NodesUtil.createUserNode(name = nodeName, personId = request.pwOpt.get.personId)
          .map { adnNode => Ok(regSuccessTpl(adnNode)) }
      }
    )
  }

}


class SsRoutesService extends RoutesService.Default {

  override def absoluteUrl(call: Call)(implicit req: RequestHeader): String = {
    if(call.isInstanceOf[ExternalCall])
      call.url
    else
      Context.LK_URL_PREFIX + call.url
  }

  override def authenticationUrl(providerId: String, redirectTo: Option[String])
                                (implicit req: RequestHeader): String = {
    val prov = ILoginProvider.maybeWithName(providerId).get
    val relUrl = routes.Ident.idViaProvider(prov, redirectTo)
    absoluteUrl(relUrl)
  }

  override def loginPageUrl(implicit req: RequestHeader): String = {
    absoluteUrl( routes.Ident.emailPwLoginForm() )
  }

}
