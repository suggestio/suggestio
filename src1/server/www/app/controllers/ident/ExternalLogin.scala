package controllers.ident

import controllers.{SioController, routes}
import io.suggest.auth.{AuthenticationException, AuthenticationResult}
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.EsModelDi
import io.suggest.ext.svc.MExtService
import io.suggest.model.n2.edge._
import io.suggest.model.n2.node.{IMNodes, MNode, MNodeTypes}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.sec.m.msession.{CustomTtl, Keys}
import io.suggest.util.logs.IMacroLogs
import models.ExtRegConfirmForm_t
import models.mctx.p4j.{P4jWebContext, P4jWebContextFactory}
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
  with EsModelDi
  with IIdentUtil
{

  val mPersonIdentModel: MPersonIdentModel
  val canConfirmIdpReg: CanConfirmIdpReg

  import mCommonDi._
  import esModel.api._
  import mPersonIdentModel.api._

  /** Доступ к DI-инстансу */
  val secureSocialLogin = current.injector.instanceOf[SecureSocialLoginAdp]
  val canLoginVia = current.injector.instanceOf[CanLoginVia]

  /**
   * GET-запрос идентификации через внешнего провайдера.
   * @param r Обратный редирект.
   * @return Redirect.
   */
  def idViaProvider(extService: MExtService, r: Option[String]) = handleAuth1(extService, r)

  /**
   * POST-запрос идентификации через внешнего провайдера.
   * @param r Редирект обратно.
   * @return Redirect.
   */
  def idViaProviderByPost(extService: MExtService, r: Option[String]) = handleAuth1(extService, r)

  // Код handleAuth() спасён из умирающего securesocial c целью отпиливания от грёбаных authentificator'ов,
  // которые по сути являются переусложнёнными stateful(!)-сессиями, которые придумал какой-то нехороший человек.
  // TODO Сделать MExtService в аргументах вместо провайдера. Вычистить код логин-провайдера от qsb/pb-мусора.
  protected def handleAuth1(extService: MExtService, redirectTo: Option[String]) = canLoginVia(extService).async { implicit request =>
    lazy val logPrefix = s"handleAuth1($extService):"
    request.apiAdp.authenticate(extService).flatMap {
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

        for {
          // Поиск уже известного юзера:
          knownUserOpt <- mNodes.getByUserIdProv( extService, profile.userId )

          // Обработка результата поиска существующего юзера.
          mperson2 <- {
            knownUserOpt.fold {
              // Юзер отсутствует. Создать нового юзера:
              // TODO Для сохранения перс.данных показать вопрос.
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
                    emails      = profile.emails.toList
                  )
                  // Ссылку на страничку юзера в соц.сети можно генерить на ходу через ident'ы и костыли самописные.
                ),
                edges = MNodeEdges(
                  out = {
                    val extIdentEdge = MEdge(
                      predicate = MPredicates.Ident.Id,
                      nodeIds   = Set( profile.userId ),
                      info      = MEdgeInfo(
                        extService = Some( extService )
                      )
                    )
                    var identEdgesAcc: List[MEdge] = extIdentEdge :: Nil

                    def _maybeAddTrustedIdents(pred: MPredicate, keys: Iterable[String]) = {
                      if (keys.nonEmpty) {
                        identEdgesAcc ::= MEdge(
                          predicate = pred,
                          nodeIds   = keys.toSet,
                          info = MEdgeInfo(
                            flag = Some(true)
                          )
                        )
                      }
                    }

                    _maybeAddTrustedIdents( MPredicates.Ident.Email, profile.emails )
                    _maybeAddTrustedIdents( MPredicates.Ident.Phone, profile.phones )

                    MNodeEdges.edgesToMap1( identEdgesAcc )
                  }
                )
              )
              for {
                personId <- mNodes.save(mperson0)
              } yield {
                LOGGER.debug(s"$logPrefix Registered new user#${personId} via service#${extService}:\n $profile")
                MNode.id.set( Some(personId) )(mperson0)
              }

            } { knownPerson0 =>
              // Юзер с таким id уже найден.
              // TODO Дополнить ident'ы какой-либо новой инфой, подчистив старую (новая почта, новый номер телефона)?
              // Например, если изменился телефон/email, то старый ident за-false-ить или удалить, новый - добавить, если есть.
              Future.successful( knownPerson0 )
            }
          }

          personId = mperson2.id.get

          // TODO Нужно редиректить юзера на подтверждение сохранения перс.данных, и только после этого сохранять.
          rdrFut: Future[Result] = if ( knownUserOpt.isDefined ) {
            Redirect(routes.Ident.idpConfirm())
          } else {
            val rdrUrlFut = toUrl2(request.session, personId)
            for (url <- rdrUrlFut) yield
              Redirect(url)
          }

          // Сборка новой сессии: чистка исходника, добавление новых ключей, относящихся к идентификации.
          session1 = {
            val addToSession0 = (Keys.PersonId.value -> personId) :: Nil
            (for {
              oa2Info   <- authenticated.profile.oAuth2Info
              expiresIn <- oa2Info.expiresIn
              if expiresIn <= request.apiAdp.MAX_SESSION_TTL_SECONDS
            } yield {
              CustomTtl(expiresIn.toLong)
                .addToSessionAcc(addToSession0)
            })
              .getOrElse( addToSession0 )
              .foldLeft( request.apiAdp.clearSession(request.session))(_ + _)
          }

          // Выставить в сессию юзера и локаль:
          rdr <- rdrFut
          rdr2 = rdr.withSession(session1)
          langOpt = getLangFrom( Some(mperson2) )
        } yield {
          setLangCookie( rdr2, langOpt )
        }

    }.recover {
      case e =>
        LOGGER.error("Unable to log user in. An exception was thrown", e)
        Redirect(routes.Ident.mySioStartPage())
          .flashing(FLASH.ERROR -> "login.errorLoggingIn")
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


