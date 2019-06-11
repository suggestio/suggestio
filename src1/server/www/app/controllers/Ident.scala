package controllers

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant

import javax.inject.{Inject, Singleton}
import controllers.ident._
import io.suggest.ctx.CtxData
import io.suggest.es.model.EsModel
import io.suggest.i18n.MsgCodes
import io.suggest.id.login.{ILoginFormPages, MEpwLoginReq}
import io.suggest.id.reg.{MRegCaptchaReq, MRegTokenResp}
import io.suggest.id.token.{MIdMessage, MIdMessageData, MIdToken, MIdTokenInfo, MIdTokenTypes}
import io.suggest.init.routed.MJsInitTargets
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.MNodes
import io.suggest.sec.csp.Csp
import io.suggest.sec.util.{PgpUtil, ScryptUtil}
import io.suggest.session.{LongTtl, MSessionKeys, ShortTtl, Ttl}
import io.suggest.text.Validators
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.mproj.ICommonDi
import models.usr._
import play.api.data.validation.{Constraints, Valid}
import util.acl._
import util.adn.NodesUtil
import util.captcha.CaptchaUtil
import util.ident.IdentUtil
import util.mail.IMailerWrapper
import io.suggest.ueq.UnivEqUtilJvm._
import views.html.ident._
import views.html.ident.login.epw._loginColumnTpl
import views.html.ident.reg.email.{_regColumnTpl, emailRegMsgTpl}
import views.html.ident.recover.emailPwRecoverTpl
import views.html.lk.login._
import japgolly.univeq._
import models.im.MCaptchaSecret
import models.sms.MSmsSend
import org.apache.commons.io.IOUtils
import play.api.libs.json.Json
import util.sec.CspUtil
import util.sms.SmsSendUtil

import scala.concurrent.Future
import scala.concurrent.duration._

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
                        pgpUtil                           : PgpUtil,
                        cspUtil                           : CspUtil,
                        smsSendUtil                       : SmsSendUtil,
                        override val canRecoverPw         : CanRecoverPw,
                        override val isAnon               : IsAnon,
                        override val isAuth               : IsAuth,
                        override val ignoreAuth           : IgnoreAuth,
                        override val sioControllerApi     : SioControllerApi,
                        override val mCommonDi            : ICommonDi
                      )
  extends MacroLogsImpl
  with EmailPwLogin
  with ChangePw
  with PwRecover
  with EmailPwReg
  with ExternalLogin
{

  import mCommonDi._
  import sioControllerApi._

  /** Длина смс-кодов (кол-во цифр). */
  def SMS_CODE_LEN = 5


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



  // -----------------------------------------------------------------------------------
  // - Login v2 ------------------------------------------------------------------------
  // -----------------------------------------------------------------------------------
  import esModel.api._
  import mPersonIdentModel.api._


  /** CSP-заголовок, разрешающий работу системы внешнего размещения карточек. */
  private def CSP_HDR_OPT: Option[(String, String)] = {
    cspUtil
      .mkCustomPolicyHdr( _.addImgSrc( Csp.Sources.BLOB ) )
  }

  /** Страница с отдельной формой логина по имени-паролю.
    * @param lfp Парсится на стороне сервера.
    * @return Страница с логином.
    */
  def loginFormPage(lfp: ILoginFormPages) = csrf.AddToken {
    isAnon().async { implicit request =>
      implicit val ctxData = CtxData(
        jsInitTargets = MJsInitTargets.LoginForm :: Nil
      )
      cspUtil.applyCspHdrOpt( CSP_HDR_OPT ) {
        Ok( LkLoginTpl() )
      }
    }
  }


  /** Сабмит react-формы логина по имени-паролю.
    * В теле запроса содержится JSON с данными
    *
    * @param r опциональный редирект.
    * @return 200 OK + JSON с данными для дальнейшенго редиректа.
    */
  def epw2LoginSubmit(r: Option[String]) = csrf.Check {
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


  /** Экшен сабмита react-формы регистрации по паролю.
    * В теле - JSON.
    *
    * @return 200 OK + JSON, или ошибка.
    */
  def epw2RegSubmit() = csrf.Check {
    val now = Instant.now()
    lazy val logPrefix = s"epw2RegSubmit()#${now.toEpochMilli}:"

    isAnon().async[(MRegCaptchaReq, MCaptchaSecret)] {
      parse
        .json[MRegCaptchaReq]
        // Начать с синхронной валидации данных:
        .validate { epwReg =>
          // Проверить валидность email:
          val vldRes = Constraints.emailAddress.apply( epwReg.creds0.email )
          Either.cond(
            test = vldRes ==* Valid,
            right = epwReg,
            left = {
              LOGGER.warn(s"$logPrefix Invalid email address:\n email = ${epwReg.creds0.email}\n vld res = $vldRes")
              NotAcceptable("email.invalid")
            }
          )
          // Проверить валидность номера телефона.
          .filterOrElse(
            {epwReg =>
              Validators.isPhoneValid( epwReg.creds0.phone )
            },
            {
              LOGGER.warn(s"$logPrefix Invalid phone number: ${epwReg.creds0.phone}")
              NotAcceptable("phone.invalid")
            }
          )
        }
        // Отработать проверку капчи:
        .validateM { epwReg =>
          for {
            // TODO Сайд-эффект: транзакция с блокировкой одноразового токена текущей капчи.
            vldRes <- captchaUtil.validateAndMarkAsUsed( epwReg.captcha )
          } yield {
            vldRes
              .left.map { NotAcceptable(_) }
              .right.map { captchaSecret =>
                (epwReg, captchaSecret)
              }
          }
        }

    } { implicit request =>
      val (mreg, _) = request.body
      LOGGER.trace(s"$logPrefix email=${mreg.creds0.email} capTyped=${mreg.captcha.typed}")

      // Проверки выполнены. Причесать исходные данные:
      val creds02 = mreg.creds0.copy(
        email = mreg.creds0
          .email
          .trim
          .toLowerCase(),
        // TODO Задействовать libphonenumber или что-то этакое.
        phone = mreg.creds0.phone.trim,
      )

      // Отправить смс с кодом проверки.
      val smsCode = captchaUtil.createCaptchaDigits(5)

      implicit val ctx = implicitly[Context]

      val smsSendResFut = smsSendUtil.smsSend(
        MSmsSend(
          msgs = Map(
            creds02.phone -> (ctx.messages( MsgCodes.`Registration.code`, smsCode ) :: Nil)
          ),
          ttl       = Some( 10.minutes ),
          translit  = false,
        )
      )
      val pgpKeyFut = pgpUtil.getLocalStorKey()

      val idTokPayload = Json.toJson( creds02 )

      // TODO Проверка капчи выполнена - вернуть токен для дальнейшей валидации смс-кода.
      for {
        smsSendRes <- smsSendResFut

        idToken = {
          for (r <- smsSendRes.iterator; balance <- r.restBalance)
            LOGGER.info(s"$logPrefix New balance => $balance ${r.requestId.getOrElse("")}")

          MIdToken(
            typ       = MIdTokenTypes.PhoneCheck,
            info      = MIdTokenInfo(
              idMsgs = (for {
                r <- smsSendRes.iterator
                (smsRuPhoneNumber, sentStatus) <- r.smsInfo.headOption
                //if sentStatus.isOk  // Плевать на ошибку, возможно какой-то спам-фильтр сработал или что-то ещё.
              } yield {
                if (!sentStatus.isOk)
                  LOGGER.warn(s"$logPrefix Looks like, sms to $smsRuPhoneNumber was NOT sent.")
                MIdMessage(
                  rcptType = MPredicates.Ident.Phone,
                  rcpt     = Set( creds02.phone, smsRuPhoneNumber ),
                  messages = MIdMessageData(
                    sentAt = Instant.now(),
                    msgId  = sentStatus.smsId,
                    checkCode = smsCode
                  ) :: Nil
                )
              })
                .toList
            ),
            payload   = idTokPayload,
          )
        }

        idTokenJsonStr = Json.toJson( idToken ).toString()

        // Надо зашифровать id-токен, чтобы передать на руки юзеру для дальнейших шагов регистрации
        pgpKey <- pgpKeyFut

      } yield {
        // Зашифровать всё с помощью PGP.
        val baos = new ByteArrayOutputStream(1024)
        val cipherText = try {
          pgpUtil.encryptForSelf(
            data = IOUtils.toInputStream(idTokenJsonStr, StandardCharsets.UTF_8),
            key  = pgpKey,
            out  = baos
          )
          new String(baos.toByteArray)
        } finally {
          // Это не нужно, но пусть будет, на случай, если в светлом далёком будущем это вдруг изменится.
          baos.close()
        }

        val resp = MRegTokenResp(
          token = pgpUtil.minifyPgpMessage( cipherText )
        )

        Ok( Json.toJson(resp) )
      }

      /*for {
        // Проверить, что там с почтовым адресом?
        // Поиск юзеров с таким вот email:
        userAlreadyExists <- mNodes.dynExists {
          new MNodeSearchDfltImpl {
            override val nodeTypes = MNodeTypes.Person :: Nil
            override def limit = 2
            override val outEdges: Seq[Criteria] = {
              val cr = Criteria(
                nodeIds     = email1 :: Nil,
                predicates  = MPredicates.Ident.Email :: Nil,
              )
              cr :: Nil
            }
          }
        }

        // Разобраться, есть ли уже юзеры с таким email?
        // Тут есть несколько вариантов: юзер не существует, юзер уже существует.
        isReg = {
          LOGGER.trace(s"$logPrefix userAlreadyExists?$userAlreadyExists email=$email1")
          !userAlreadyExists
        }

        // Рандомная строка, выполняющая роль nonce + привязка письма к сессии текущего юзера.
        randomUuid = UUID.randomUUID()

        _ <- {
          implicit val ctx = implicitly[Context]
          mailer
            .instance
            .setRecipients( email1 )
            .setSubject {
              val prefixMsgCode =
                if (isReg) "reg.emailpw.email.subj"
                else MsgCodes.`Password.recovery`
              s"${ctx.messages(prefixMsgCode)} | ${MsgCodes.`Suggest.io`}"
            }
            .setHtml {
              val tplQs = MEmailRecoverQs(
                email         = email1,
                // Привязать сессию текущего юзера к письму:
                nonce         = randomUuid,
                checkSession  = true,
              )
              val tpl =
                if (isReg) emailRegMsgTpl
                else emailPwRecoverTpl
              val html = tpl.render(tplQs, ctx)
              htmlCompressUtil.html4email( html )
            }
            .send()
        }

      } yield {
        LOGGER.debug(s"$logPrefix Sent message to $email1 isReg?$isReg")
        // TODO Залить id в текущую сессию для привязки сессии к письму?

        NoContent
          .addingToSession(
            MSessionKeys.ExtLoginData.value -> randomUuid.toString
          )
      }*/
    }
  }


  /** Возврат из регистрации или восстановления пароля.
    *
    * @param qs Данные регистрации или восстановления пароля.
    * @return Экшен, рендерящий react-форму окончания регистрации.
    */
  def epwReturn(qs: MEmailRecoverQs) = csrf.Check {
    ???
  }

}

