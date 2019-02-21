package controllers.ident

import javax.inject.Inject
import controllers.{SioController, routes}
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.EsModelDi
import io.suggest.ext.svc.MExtServices
import io.suggest.model.n2.node.{IMNodes, MNode, MNodeTypes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.playx.ExternalCall
import io.suggest.sec.m.msession.{CustomTtl, Keys}
import io.suggest.util.logs.{IMacroLogs, MacroLogsDyn}
import models.ExtRegConfirmForm_t
import models.mctx.ContextUtil
import models.mctx.p4j.{P4jWebContext, P4jWebContextFactory}
import models.mext.{ILoginProvider, MExtServicesJvm}
import models.mproj.ICommonDi
import models.req.IReq
import models.usr._
import org.pac4j.core.engine.DefaultCallbackLogic
import org.pac4j.core.config.{Config => P4jConfig}
import play.api.cache.AsyncCacheApi
import play.api.data.Form
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.mvc._
import play.twirl.api.Html
import securesocial.controllers.ProviderControllerHelper._
import securesocial.core.RuntimeEnvironments
import securesocial.core._
import securesocial.core.services.{CacheService, HttpService, RoutesService}
import util.acl._
import util.adn.INodesUtil
import util.di.IIdentUtil
import util.ident.IdentUtil
import util.xplay.SetLangCookieUtil
import util.FormUtil
import views.html.ident.reg._
import views.html.ident.reg.ext._

import scala.collection.immutable.ListMap
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.01.15 17:16
 * Description: Поддержка логина через соц.сети или иные внешние сервисы.
 */

class ExternalLogin_ @Inject() (
                                 routesSvc                       : SsRoutesService,
                                 ssUserService                   : SsUserService,
                                 ssHttpService                   : SsHttpService,
                                 ssCacheService                  : SsCacheService,
                                 ssRuntimeEnvironments           : RuntimeEnvironments,
                                 override val identUtil          : IdentUtil,
                                 mCommonDi                       : ICommonDi
                               )
  extends MacroLogsDyn
  with IIdentUtil
{

  import mCommonDi._

  /** Фильтровать присылаемый ttl. */
  val MAX_SESSION_TTL_SECONDS = {
    configuration.getOptional[Int]("login.ext.session.ttl.max.minutes")
      .getOrElse(86400)
      .minutes
      .toSeconds
  }

  /** secure-social настраивается через этот Enviroment. */
  implicit val env: RuntimeEnvironment[SsUser] = {
    new ssRuntimeEnvironments.Default[SsUser] {
      override def cacheService = ssCacheService
      override def httpService = ssHttpService
      override lazy val routes = routesSvc
      override def userService = ssUserService
      override lazy val providers: ListMap[String, IdentityProvider] = {
        // Аккуратная инициализация доступных провайдеров и без дубликации кода.
        val provs = MExtServices.values
          .iterator
          .flatMap { service =>
            val svcJvm = MExtServicesJvm.forService( service )
            svcJvm.loginProvider
          }
          .flatMap { prov =>
            val provSt = current.injector.instanceOf( prov.ssProviderClass )
            try {
              provSt(routes, cacheService, httpService) :: Nil
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
    FutureUtil.opt2future( ses.get(Keys.OrigUrl.value) ) {
      identUtil.redirectCallUserSomewhere(personId)
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
  with IMacroLogs
  with SetLangCookieUtil
  with INodesUtil
  with IMNodes
  with IMaybeAuth
  with IMExtIdentsDi
  with EsModelDi
{

  import mCommonDi._
  import esModel.api._

  val canConfirmIdpReg: CanConfirmIdpReg

  /** Доступ к DI-инстансу */
  val externalLogin: ExternalLogin_ = current.injector.instanceOf[ExternalLogin_]

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
    externalLogin.env.providers
      .get(provider.ssProvName)
      .fold[Future[Result]] {
        errorHandler.onClientError(request, NOT_FOUND)
      } { idProv =>
        idProv.authenticate().flatMap {
          case _: AuthenticationResult.AccessDenied =>
            val res = Redirect( routes.Ident.mySioStartPage() )
              .flashing(FLASH.ERROR -> "securesocial.login.accessDenied")
            res

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
                  val meiFut = mpersonSaveFut.flatMap { personId =>
                    // Сохранить данные идентификации через соц.сеть.
                    val mei = MExtIdent(
                      personId  = personId,
                      provider  = provider,
                      userId    = profile.userId,
                      email     = profile.email
                    )
                    val save2Fut = mExtIdents.save(mei)
                    LOGGER.debug(s"$logPrefix Registered new user $personId from ext.login service, remote user_id = ${profile.userId}")
                    save2Fut.map { _ => mei }
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
                  Future.successful( ident -> None )
              }

              saveFut.flatMap { case (ident, newMpersonOpt) =>
                // Можно перенести внутрь match всю эту логику. Т.к. она очень предсказуема. Но это наверное ещё добавит сложности кода.
                val mpersonOptFut = newMpersonOpt match {
                  case None =>
                    mNodes
                      .getByIdCache(ident.personId)
                      .withNodeType(MNodeTypes.Person)
                  case some =>
                    Future.successful( some )
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
                var addToSessionAcc: List[(String, String)] = List(Keys.PersonId.value -> ident.personId)
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

        }.recover {
          case e =>
            LOGGER.error("Unable to log user in. An exception was thrown", e)
            Redirect(routes.Ident.mySioStartPage())
              .flashing(FLASH.ERROR -> "securesocial.login.errorLoggingIn")
        }
      }
  }


  /**
   * Юзер, залогинившийся через провайдера, хочет создать ноду.
   * @return Страницу с колонкой подтверждения реги.
   */
  def idpConfirm = csrf.AddToken {
    canConfirmIdpReg() { implicit request =>
      val form = externalLogin.extRegConfirmFormM
      Ok( _idpConfirm(form) )
    }
  }

  /** Общий код рендера idpConfig вынесен сюда. */
  protected def _idpConfirm(form: ExtRegConfirmForm_t)(implicit request: IReq[_]): Html = {
    confirmTpl(form)
  }

  /** Сабмит формы подтверждения регистрации через внешнего провайдера идентификации. */
  def idpConfirmSubmit = csrf.Check {
    canConfirmIdpReg().async { implicit request =>
      externalLogin.extRegConfirmFormM.bindFromRequest().fold(
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


class SsRoutesService @Inject() (ctxUtil: ContextUtil) extends RoutesService {

  private def absoluteUrl(call: Call)(implicit req: RequestHeader): String = {
    if(call.isInstanceOf[ExternalCall])
      call.url
    else
      ctxUtil.LK_URL_PREFIX + call.url
  }

  override def authenticationUrl(providerId: String, redirectTo: Option[String])
                                (implicit req: RequestHeader): String = {
    val prov = ILoginProvider.maybeWithName(providerId).get
    val relUrl = routes.Ident.idViaProvider(prov, redirectTo)
    absoluteUrl( relUrl )
  }

  override def loginPageUrl(implicit req: RequestHeader): String = {
    val call = routes.Ident.emailPwLoginForm()
    absoluteUrl( call )
  }

}


class SsHttpService @Inject() (ws: WSClient) extends HttpService {
  override def url(url: String): WSRequest = ws.url(url)
}


class SsCacheService @Inject() (cacheApi: AsyncCacheApi) extends CacheService {

  override def set[T](key: String, value: T, ttlInSeconds: Int): Future[_] = {
    cacheApi.set(key, value, expiration = ttlInSeconds.seconds)
  }

  override def getAs[T](key: String)(implicit ct: ClassTag[T]): Future[Option[T]] = {
    cacheApi.get[T](key)
  }

  override def remove(key: String): Future[_] = {
    cacheApi.remove(key)
  }

}

