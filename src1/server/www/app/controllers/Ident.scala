package controllers

import javax.inject.{Inject, Singleton}
import controllers.ident._
import io.suggest.ctx.CtxData
import io.suggest.es.model.EsModel
import io.suggest.i18n.MsgCodes
import io.suggest.id.login.{ILoginFormPages, MEpwLoginReq}
import io.suggest.init.routed.{MJsInitTarget, MJsInitTargets}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.MNodes
import io.suggest.sec.util.ScryptUtil
import io.suggest.session.{LongTtl, MSessionKeys, ShortTtl, Ttl}
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.mproj.ICommonDi
import models.req.IReqHdr
import models.usr._
import util.acl._
import util.adn.NodesUtil
import util.captcha.CaptchaUtil
import util.ident.IdentUtil
import util.mail.IMailerWrapper
import views.html.ident._
import views.html.ident.login.epw._loginColumnTpl
import views.html.ident.reg.email._regColumnTpl
import views.html.lk.login._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 11:47
 * Description: Контроллер обычного логина в систему.
 * Обычно логинятся через email+password.
 * 2015.jan.27: вынос разжиревших кусков контроллера в util.acl.*, controllers.ident.* и рефакторинг.
 */

@Singleton
class Ident @Inject() (
                        override val esModel              : EsModel,
                        override val mPersonIdentModel    : MPersonIdentModel,
                        override val mNodes               : MNodes,
                        override val mailer               : IMailerWrapper,
                        override val identUtil            : IdentUtil,
                        override val nodesUtil            : NodesUtil,
                        override val captchaUtil          : CaptchaUtil,
                        override val canConfirmEmailPwReg : CanConfirmEmailPwReg,
                        override val bruteForceProtect    : BruteForceProtect,
                        override val scryptUtil           : ScryptUtil,
                        override val maybeAuth            : MaybeAuth,
                        override val canConfirmIdpReg     : CanConfirmIdpReg,
                        override val canRecoverPw         : CanRecoverPw,
                        override val isAnon               : IsAnon,
                        override val isAuth               : IsAuth,
                        override val mCommonDi            : ICommonDi
                      )
  extends SioControllerImpl
  with MacroLogsImpl
  with EmailPwLogin
  with CaptchaValidator
  with ChangePw
  with PwRecover
  with EmailPwReg
  with ExternalLogin
{

  import mCommonDi._

  /**
   * Юзер разлогинивается. Выпилить из сессии данные о его логине.
   * @return Редирект на главную, ибо анонимусу идти больше некуда.
   */
  // TODO Добавить CSRF
  def logout = Action { implicit request =>
    Redirect(models.MAIN_PAGE_CALL)
      .removingFromSession(
        MSessionKeys.PersonId.value,
        MSessionKeys.Timestamp.value
      )
  }


  /** Отредиректить юзера куда-нибудь. */
  def rdrUserSomewhere = isAuth().async { implicit request =>
    identUtil.redirectUserSomewhere(request.user.personIdOpt.get)
  }

  /**
   * Стартовая страница my.suggest.io. Здесь лежит предложение логина/регистрации и возможно что-то ещё.
   * @param r Возврат после логина куда?
   * @return 200 Ok для анонимуса.
   *         Иначе редирект в личный кабинет.
   */
  def mySioStartPage(r: Option[String]) = csrf.AddToken {
    isAnon().async { implicit request =>
      implicit val ctxData = CtxData(
        jsInitTargets = MJsInitTargets.CaptchaForm :: MJsInitTargets.HiddenCaptcha :: Nil
      )
      // TODO Затолкать это в отдельный шаблон!
      val ctx = implicitly[Context]
      val formFut = emailPwLoginFormStubM
      val title = ctx.messages( MsgCodes.`Login.page.title` )
      val rc = _regColumnTpl(emailRegFormM, captchaShown = false)(ctx)
      for (lf <- formFut) yield {
        val lc = _loginColumnTpl(lf, r)(ctx)
        Ok( mySioStartTpl(title, Seq(lc, rc))(ctx) )
      }
    }
  }


  /** Вернуть список целей инициализации js.
    * Страницы ident-контроллера нуждаются в доп.центровке колонок по вертикали. */
  override def jsiTgs(req: IReqHdr): List[MJsInitTarget] = {
    MJsInitTargets.IdentVCenterContent :: super.jsiTgs(req)
  }



  // -----------------------------------------------------------------------------------
  // - Login v2 ------------------------------------------------------------------------
  // -----------------------------------------------------------------------------------
  import esModel.api._
  import mPersonIdentModel.api._


  /** Страница с отдельной формой логина по имени-паролю.
    * @param lfp Парсится на стороне сервера.
    * @return Страница с логином.
    */
  def loginFormPage(lfp: ILoginFormPages) = csrf.AddToken {
    isAnon().async { implicit request =>
      implicit val ctxData = CtxData(
        jsInitTargets = MJsInitTargets.LoginForm :: Nil
      )
      Ok( LkLoginTpl() )
    }
  }


  /** Сабмит react-формы логина по имени-паролю.
    * В теле запроса содержится JSON с данными
    *
    * @param r опциональный редирект.
    * @return 200 OK + JSON с данными для дальнейшенго редиректа.
    */
  def epw2Submit(r: Option[String]) = csrf.Check {
    bruteForceProtect {
      isAnon().async( parse.json[MEpwLoginReq] ) { implicit request =>

        // Поискать в БД юзера с таким именем и паролем.
        lazy val logPrefix = s"epwSubmit()#${System.currentTimeMillis()}"
        LOGGER.trace(s"$logPrefix Login request from ${request.remoteClientAddress}: name'${request.body.name}' pw#${request.body.password.hashCode} foreign?${request.body.isForeignPc}")

        for {
          // Запуск поиска юзеров, имеющих указанное имя пользователя.
          personsFound <- mNodes.findUsersByEmailWithPw( request.body.name )

          res <- {
            // Проверить пароль, вернуть ответ.
            if (personsFound.isEmpty) {
              // Не найдена ни одного юзера с таким именем.
              LOGGER.debug(s"$logPrefix User'${request.body.name}' not found")
              Future.successful( NotFound )

            } else if (personsFound.lengthCompare(1) > 0) {
              // Сразу несколько юзеров. Это может быть какая-то ошибка или уязвимость.
              val ex = new IllegalStateException(s"$logPrefix Many nodes with same username: internal data-defect or vulnerability. Giving up.")
              Future.failed( ex )

            } else {
              // Один юзер с таким именем.
              val mperson = personsFound.head
              LOGGER.trace(s"$logPrefix Found user#${mperson.idOrNull} name'${request.body.name}'")

              if (
                mperson.edges
                  .withPredicateIter( MPredicates.Ident.Password )
                  .flatMap( _.info.textNi )
                  .exists { pwHash =>
                    scryptUtil.checkHash( request.body.password, pwHash )
                  }
              ) {
                // Пароль подходит - залогинить юзера.
                val personId = mperson.id.get
                LOGGER.debug(s"$logPrefix Epw login ok, person#$personId from ${request.remoteClientAddress}")

                val rdrPathFut = SioController.getRdrUrl(r) { emailSubmitOkCall(personId) }
                // Реализация длинной сессии при наличии флага rememberMe.
                val ttl: Ttl =
                  if (request.body.isForeignPc) ShortTtl
                  else LongTtl

                val addToSession = ttl.addToSessionAcc(
                  (MSessionKeys.PersonId.value -> personId) ::
                  Nil
                )

                // Выставить язык, сохраненный ранее в MPerson
                val langOpt = getLangFrom( Some(mperson) )
                for (rdrPath <- rdrPathFut) yield {
                  LOGGER.trace(s"$logPrefix rdrPath=>$rdrPath r=${r.orNull} lang=${langOpt.orNull}")
                  var rdr2 = Ok(rdrPath)
                    .addingToSession( addToSession : _* )
                  for (lang <- langOpt)
                    rdr2 = rdr2.withLang( lang )( mCommonDi.messagesApi )
                  rdr2
                }

              } else {
                // Пароль не совпадает.
                LOGGER.debug(s"$logPrefix Password does not match.")
                Future.successful( NotFound )
              }

            }
          }

        } yield {
          res
        }

      }
    }
  }

}

