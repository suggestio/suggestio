package controllers.ident

import controllers.{SioController, routes}
import io.suggest.auth.{AuthenticationException, AuthenticationResult}
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.EsModelDi
import io.suggest.model.n2.node.{IMNodes, MNode, MNodeTypes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.sec.m.msession.{CustomTtl, Keys}
import io.suggest.util.logs.IMacroLogs
import models.ExtRegConfirmForm_t
import models.mctx.p4j.{P4jWebContext, P4jWebContextFactory}
import models.mext.ILoginProvider
import models.req.IReq
import models.usr._
import org.pac4j.core.engine.DefaultCallbackLogic
import org.pac4j.core.config.{Config => P4jConfig}
import play.api.data.Form
import play.api.mvc._
import play.twirl.api.Html
import util.acl._
import util.adn.INodesUtil
import util.xplay.SetLangCookieUtil
import util.FormUtil
import util.ident.IIdentUtil
import util.ident.ss.SecureSocialLoginAdp
import views.html.ident.reg._
import views.html.ident.reg.ext._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.01.15 17:16
 * Description: Поддержка логина через соц.сети или иные внешние сервисы.
 */
trait ExternalLogin
  extends SioController
  with IMacroLogs
  with SetLangCookieUtil
  with INodesUtil
  with IMNodes
  with IMaybeAuth
  with IMExtIdentsDi
  with EsModelDi
  with IIdentUtil
{

  import mCommonDi._
  import esModel.api._

  val canConfirmIdpReg: CanConfirmIdpReg

  /** Доступ к DI-инстансу */
  val secureSocialLogin: SecureSocialLoginAdp = current.injector.instanceOf[SecureSocialLoginAdp]

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
  protected def handleAuth1(provider: ILoginProvider, redirectTo: Option[String]) = maybeAuth().async { implicit request =>
    lazy val logPrefix = s"handleAuth1($provider):"
    secureSocialLogin.env.providers
      .get(provider.ssProvName)
      .fold[Future[Result]] {
        errorHandler.onClientError(request, NOT_FOUND)
      } { idProv =>
        idProv.authenticate().flatMap {
          case _: AuthenticationResult.AccessDenied =>
            Redirect( routes.Ident.mySioStartPage() )
              .flashing(FLASH.ERROR -> "login.accessDenied")

          case failed: AuthenticationResult.Failed =>
            LOGGER.error(s"$logPrefix authentication failed, reason: ${failed.error}")
            throw AuthenticationException()

          case flow: AuthenticationResult.NavigationFlow => Future.successful {
            val r0 = flow.result
            redirectTo.fold( r0 ) { url =>
              r0.addingToSession(Keys.OrigUrl.value -> url)
            }
          }

          case authenticated: AuthenticationResult.Authenticated =>
            // TODO Отрабатывать случаи, когда юзер уже залогинен под другим person_id.
            val profile = authenticated.profile
            mExtIdents.getByUserIdProv(provider, profile.userId).flatMap { maybeExisting =>
              // Сохраняем, если требуется. В результате приходит также новосохранный person MNode.
              val saveFut: Future[(MExtIdent, Option[MNode])] = maybeExisting match {
                case None =>
                  val mperson0 = MNode(
                    common = MNodeCommon(
                      ntype       = MNodeTypes.Person,
                      isDependent = false
                    ),
                    meta = MMeta(
                      basic = MBasicMeta(
                        nameOpt   = profile.fullName,
                        techName  = Some(profile.providerId + ":" + profile.userId),
                        langs     = request.messages.lang.code :: Nil
                      ),
                      person  = MPersonMeta(
                        nameFirst   = profile.firstName,
                        nameLast    = profile.lastName,
                        extAvaUrls  = profile.avatarUrl.toList,
                        emails      = profile.email.toList
                      )
                      // Ссылку на страничку юзера в соц.сети можно генерить на ходу через ident'ы и костыли самописные.
                    )
                  )
                  val mpersonSaveFut = mNodes.save(mperson0)
                  val meiFut = for {
                    personId <- mpersonSaveFut
                    mei = MExtIdent(
                      personId  = personId,
                      provider  = provider,
                      userId    = profile.userId,
                      email     = profile.email
                    )
                    save2Fut = mExtIdents.save(mei)
                    _ <- {
                      LOGGER.debug(s"$logPrefix Registered new user $personId from ext.login service, remote user_id = ${profile.userId}")
                      save2Fut
                    }
                  } yield mei

                  for {
                    personId  <- mpersonSaveFut
                    mperson = mperson0.copy(id = Some(personId))
                    mei       <- meiFut
                  } yield {
                    (mei, Some(mperson))
                  }

                // Регистрация юзера не требуется. Возвращаем то, что есть в наличии.
                case Some(ident) =>
                  LOGGER.trace(s"$logPrefix Existing user[${ident.personId}] logged-in from ${profile.userId}")
                  Future.successful( ident -> None )
              }

              // После сохранения - приступать к сборке ответа.
              for {
                (ident, newMpersonOpt) <- saveFut

                mpersonOptFut = FutureUtil.opt2futureOpt( newMpersonOpt ) {
                  mNodes
                    .getByIdCache(ident.personId)
                    .withNodeType(MNodeTypes.Person)
                }

                rdrFut: Future[Result] = if ( newMpersonOpt.isDefined ) {
                  Redirect(routes.Ident.idpConfirm())
                } else {
                  val rdrUrlFut = toUrl2(request.session, ident.personId)
                  for (url <- rdrUrlFut) yield
                    Redirect(url)
                }

                // Сборка новой сессии: чистка исходника, добавление новых ключей, относящихся к идентификации.
                session1 = {
                  val addToSession0 = (Keys.PersonId.value -> ident.personId) :: Nil
                  (for {
                    oa2Info   <- authenticated.profile.oAuth2Info
                    expiresIn <- oa2Info.expiresIn
                    if expiresIn <= secureSocialLogin.MAX_SESSION_TTL_SECONDS
                  } yield {
                    CustomTtl(expiresIn.toLong)
                      .addToSessionAcc(addToSession0)
                  })
                    .getOrElse( addToSession0 )
                    .foldLeft( secureSocialLogin.clearSession(request.session))(_ + _)
                }

                // Выставить в сессию юзера и локаль:
                rdr <- rdrFut
                rdr2 = rdr.withSession(session1)
                mpersonOpt <- mpersonOptFut
                langOpt = getLangFrom( mpersonOpt )
              } yield {
                setLangCookie( rdr2, langOpt )
              }
            }

        }.recover {
          case e =>
            LOGGER.error("Unable to log user in. An exception was thrown", e)
            Redirect(routes.Ident.mySioStartPage())
              .flashing(FLASH.ERROR -> "login.errorLoggingIn")
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
  private def toUrl2(ses: Session, personId: String): Future[String] = {
    FutureUtil.opt2future( ses.get(Keys.OrigUrl.value) ) {
      identUtil.redirectCallUserSomewhere(personId)
        .map(_.url)
    }
  }

  /** Маппинг формы подтверждения регистрации через id-провайдера. */
  private def extRegConfirmFormM: ExtRegConfirmForm_t = {
    Form(
      "nodeName" -> FormUtil.nameM
    )
  }

  /**
   * Юзер, залогинившийся через провайдера, хочет создать ноду.
   * @return Страницу с колонкой подтверждения реги.
   */
  def idpConfirm = csrf.AddToken {
    canConfirmIdpReg() { implicit request =>
      Ok( _idpConfirm( extRegConfirmFormM ) )
    }
  }

  /** Общий код рендера idpConfig вынесен сюда. */
  protected def _idpConfirm(form: ExtRegConfirmForm_t)(implicit request: IReq[_]): Html = {
    confirmTpl(form)
  }

  /** Сабмит формы подтверждения регистрации через внешнего провайдера идентификации. */
  def idpConfirmSubmit = csrf.Check {
    canConfirmIdpReg().async { implicit request =>
      extRegConfirmFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug("idpConfirmSubmit(): Failed to bind form:\n " + formatFormErrors(formWithErrors))
          NotAcceptable( _idpConfirm(formWithErrors) )
        },
        {nodeName =>
          // Развернуть узел для юзера, отобразить страницу успехоты.
          for {
            mnode <- nodesUtil.createUserNode(name = nodeName, personId = request.user.personIdOpt.get)
          } yield {
            val args = nodesUtil.nodeRegSuccessArgs( mnode )
            Ok( regSuccessTpl(args) )
          }
        }
      )
    }
  }



  //--------------------------------------------------------------------------------------------------//
  //----------------------------------------- v2 ext login -------------------------------------------//
  //--------------------------------------------------------------------------------------------------//

  // TODO logic-кода здесь быть не должно, нужно допилить на базе собтсвенной интеграции с сервисами.
  private lazy val callbackLogic = new DefaultCallbackLogic[Result, P4jWebContext]()
  private val p4jWebContextFactory = current.injector.instanceOf[P4jWebContextFactory]
  private val p4jConfig = current.injector.instanceOf[P4jConfig]


  /** v2 ext login: через Pac4j.
    * Контроллер дёргает pac4j, и отрабатывает итог используется.
    */
  def callback2(r: Option[String]) = maybeAuth().async { implicit request =>
    val p4jCtx = p4jWebContextFactory.fromRequest()

    // TODO Портировать handleAuth1
    // p4jCtx,
    // p4jConfig,
    // defaultUrl = routes.Sc.geoSite().url,
    ???
  }

  /**
   * GET-запрос идентификации через pac4j.
   * @param provider провайдер идентификации.
   * @param r Обратный редирект.
   * @return Redirect.
   */
  def extIdCbGet(r: Option[String]) = callback2(r)

  /**
   * POST-запрос идентификации через pac4j.
   * @param provider Провайдер идентификации.
   * @param r Редирект обратно.
   * @return Redirect.
   */
  def extIdCbPost(r: Option[String]) = callback2(r)

}


