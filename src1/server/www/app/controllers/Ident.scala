package controllers

import java.time.{Instant, ZoneOffset}
import java.util.UUID

import javax.inject.{Inject, Singleton}
import controllers.ident._
import io.suggest.captcha.MCaptchaCheckReq
import io.suggest.common.empty.OptionUtil
import io.suggest.ctx.CtxData
import io.suggest.es.model.{EsModel, IMust}
import io.suggest.i18n.MsgCodes
import io.suggest.id.IdentConst
import io.suggest.id.login.{ILoginFormPages, MEpwLoginReq}
import io.suggest.id.reg.{MCodeFormData, MCodeFormReq, MRegCreds0, MRegTokenResp}
import io.suggest.id.token.{MIdMsg, MIdToken, MIdTokenConstaints, MIdTokenDates, MIdTokenTypes}
import io.suggest.init.routed.MJsInitTargets
import io.suggest.mbill2.m.ott.{MOneTimeToken, MOneTimeTokens}
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicate, MPredicates}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{MNode, MNodeType, MNodeTypes, MNodes}
import io.suggest.sec.csp.Csp
import io.suggest.sec.util.{PgpUtil, ScryptUtil}
import io.suggest.session.{LongTtl, MSessionKeys, ShortTtl, Ttl}
import io.suggest.streams.JioStreamsUtil
import io.suggest.text.Validators
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.ueq.UnivEqUtil._
import models.mctx.Context
import models.mproj.ICommonDi
import models.usr._
import play.api.data.validation.{Constraints, Valid}
import util.acl._
import util.adn.NodesUtil
import util.captcha.CaptchaUtil
import util.ident.{IdTokenUtil, IdentUtil}
import util.mail.IMailerWrapper
import io.suggest.ueq.UnivEqUtilJvm._
import views.html.ident._
import views.html.ident.login.epw._loginColumnTpl
import views.html.ident.reg.email.{_regColumnTpl, emailRegMsgTpl}
import views.html.ident.recover.emailPwRecoverTpl
import views.html.lk.login._
import japgolly.univeq._
import models.req.IReqHdr
import models.sms.{ISmsSendResult, MSmsSend}
import play.api.libs.json.Json
import play.api.mvc.Result
import util.sec.CspUtil
import util.sms.SmsSendUtil

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

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
                        mOneTimeTokens                    : MOneTimeTokens,
                        idTokenUtil                       : IdTokenUtil,
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
  import slick.profile.api._

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
          personsFound <- mNodes.findUsersByEmailPhoneWithPw( request.body.name )

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


  private def _genSmsCode() = captchaUtil.createCaptchaDigits(5)


  /** Отправка смс с кодом проверки. */
  private def _sendIdCodeSms(phoneNumber: String, smsCode: String, ctx: Context) = {
    smsSendUtil.smsSend(
      MSmsSend(
        msgs = Map(
          phoneNumber -> (ctx.messages( MsgCodes.`Registration.code`, smsCode ) :: Nil)
        ),
        ttl       = Some( 10.minutes ),
        translit  = false,
      )
    )
  }


  /** Конвертация отправленных смс'ок в id-msgs. */
  private def _sendSmsRes2IdMsgs(phoneNumber: String, smsCode: String, now: Instant, smsSendRes: Seq[ISmsSendResult]) = {
    (for {
      r <- smsSendRes.iterator
      (smsRuPhoneNumber, sentStatus) <- r.smsInfo.headOption
      //if sentStatus.isOk  // Плевать на ошибку, возможно какой-то спам-фильтр сработал или что-то ещё.
    } yield {
      if (!sentStatus.isOk)
        LOGGER.warn(s"_sendSmsRes2IdMsgs($phoneNumber): Looks like, sms to $smsRuPhoneNumber was NOT sent.")
      MIdMsg(
        rcptType  = MPredicates.Ident.Phone,
        rcpt      = Set( phoneNumber, smsRuPhoneNumber ),
        sentAt    = now,
        msgId     = sentStatus.smsId,
        checkCode = smsCode,
      )
    })
      .toList
  }


  private def _tokenResp(token: String) = {
    val respBody = MRegTokenResp(
      token = token
    )
    Ok( Json.toJson(respBody) )
  }


  /** Сабмит данных нулевого шага для перехода к капче.
    * Реквизиты регистрации заворачиваются в токен, шифруются и отправляются клиенту.
    *
    * @return Ответ в виде токена, с помощью которого можно организовать шаг капчи.
    */
  def regStep0Submit = csrf.Check {
    bruteForceProtect {
      val now = Instant.now()
      lazy val logPrefix = s"epw2RegSubmit()#${now.toEpochMilli}:"
      isAnon().async {
        parse
          .json[MRegCreds0]
          // Начать с синхронной валидации присланных данных. Делаем это прямо тут, хотя по логике оно должно жить в теле экшена.
          .validate { creds0 =>
            // Проверить валидность email:
            val vldRes = Constraints.emailAddress.apply( creds0.email )
            Either.cond(
              test  = vldRes ==* Valid,
              right = creds0,
              left  = {
                LOGGER.warn(s"$logPrefix Invalid email address:\n email = ${creds0.email}\n vld res = $vldRes")
                NotAcceptable("email.invalid")
              }
            )
            // Проверить валидность номера телефона.
            .filterOrElse(
              {creds1 =>
                Validators.isPhoneValid( creds1.phone )
              },
              {
                LOGGER.warn(s"$logPrefix Invalid phone number: ${creds0.phone}")
                NotAcceptable("phone.invalid")
              }
            )
          }
      } { implicit request =>
        // Сгенерить токен с данными регистрации. С помощью этого токена можно будет запросить капчу.
        val idToken = MIdToken(
          typ     = MIdTokenTypes.IdentVerify,
          idMsgs  = Nil,
          dates   = MIdTokenDates(
            ttlSeconds = 15,
          ),
          payload = Json.toJson {
            request.body.copy(
              // Нормализованные обязательно.
              email = Validators.normalizeEmail( request.body.email ),
              // TODO Задействовать libphonenumber или что-то этакое.
              phone = Validators.normalizePhoneNumber( request.body.phone ),
            )
            request.body
          },
          constraints = MIdTokenConstaints(
            personIdsC = Some( request.user.personIdOpt.toSet ),
          ),
        )
        val idTokenCipheredFut = idTokenUtil.encrypt( idToken )
        LOGGER.trace(s"$logPrefix Ad-hoc idToken#${idToken.ottId} created=${idToken.dates.created.atOffset(ZoneOffset.UTC)}\n $idToken")

        for {
          tokenEncrypted <- idTokenCipheredFut
        } yield {
          _tokenResp( tokenEncrypted )
        }
      }
    }
  }


  /** Экшен сабмита react-формы регистрации по паролю: шаг капчи.
    * В теле - JSON.
    *
    * @return 200 OK + JSON, или ошибка.
    */
  def epw2RegSubmit() = csrf.Check {
    val now = Instant.now()
    lazy val logPrefix = s"epw2RegSubmit()#${now.toEpochMilli}:"

    val _theAction = isAnon().async {
      parse
        .json[MCaptchaCheckReq]
    } { implicit request =>
      LOGGER.trace(s"$logPrefix capTyped=${request.body.typed}")

      implicit lazy val ctx = implicitly[Context]

      for {
        // Проверить капчу, сразу отметив её в базе, как использованную:
        captchaVldRes <- captchaUtil.validateAndMarkAsUsed( request.body )

        pgpKeyFut = pgpUtil.getLocalStorKey()

        // Убедится, что капча проверена успешно:
        (idToken0, mott0) = captchaVldRes
          .left.map { emsg =>
            throw new IllegalArgumentException(emsg)
          }
          .right.get

        // Распарсить введённые юзером реквизиты регистрации.
        creds02 = idToken0.payload.as[MRegCreds0]

        // Подготовиться к отправки смс с кодом проверки.
        smsCode = _genSmsCode()

        // Запуск отправки смс-кода в фоне.
        smsSendResFut = _sendIdCodeSms(
          phoneNumber = creds02.phone,
          smsCode     = smsCode,
          ctx         = ctx
        )

        smsSendRes <- smsSendResFut

        smsIdMsgs = _sendSmsRes2IdMsgs(
          phoneNumber = creds02.phone,
          smsCode     = smsCode,
          now         = now,
          smsSendRes  = smsSendRes,
        )

        // Сборка инстанса id-токена.
        idToken2 = (
          MIdToken.idMsgs.modify(smsIdMsgs ++ _) andThen
          MIdToken.dates.modify { dates0 =>
            dates0.copy(
              ttlSeconds = 10.minutes.toSeconds.toInt,
              modified   = Some( now ),
            )
          }
        )(idToken0)

        idTokenJsonStr = Json.toJson( idToken2 ).toString()

        // Обновление токен
        mott2 = mott0.copy(
          dateEnd = now plusSeconds IdentConst.Reg.ID_TOKEN_VALIDITY_SECONDS,
          info    = Some(idTokenJsonStr),
        )

        // 2019.06.20 Из-за переусложнения схемы токена, решено просто сохранять его в ott.info, а при проверке смс-кода оттуда же брать и там же обновлять.
        // На клиенте, после прохождения капчи, держать только "указатель" на токен - защищённый/зашифрованный id-токена в базе.
        _ <- slick.db.run {
          val dbAction = for {
            countUpdated <- mOneTimeTokens.updateExisting( mott2 )
            if countUpdated ==* 1
          } yield {
            None
          }
          dbAction.transactionally
        }

        // Надо зашифровать id-токен, чтобы передать на руки юзеру для дальнейших шагов регистрации
        pgpKey <- pgpKeyFut

      } yield {
        // Зашифровать и подписать id сохранённого токена:
        val cipherText = JioStreamsUtil.stringIo[String]( idToken2.ottId.toString, 512 )( pgpUtil.encryptForSelf(_, pgpKey, _) )

        _tokenResp(
          token = pgpUtil.minifyPgpMessage( cipherText )
        )
      }
    }

    bruteForceProtect( _theAction )
  }


  /** Парсер для MCodeForm. Используется при вводе смс-кода и при вводе пароля. */
  private def _parseCodeFormReq(isCodeValid: String => Boolean) = {
    val pgpKeyFut = pgpUtil.getLocalStorKey()
    lazy val logPrefix = s"_parseIdTokenPtr#${System.currentTimeMillis()}:"
    parse
      .json[MCodeFormReq]
      .validate { smsReq =>
        Either.cond(
          test  = smsReq.formData match {
            case MCodeFormData(Some(smsCode), false) =>
              isCodeValid( smsCode )
            case MCodeFormData(None, true) =>
              true
            case _ =>
              false
          },
          right = smsReq,
          left  = {
            LOGGER.debug(s"$logPrefix Invalid smsCode format: ${smsReq.formData}")
            NotAcceptable("sms.code.invalid")
          }
        )
      }
      // И расшифровать присланные данные токена:
      .validateM { smsReq =>
        val fut0 = for (pgpKey <- pgpKeyFut) yield {
          val pgpMsg = pgpUtil.unminifyPgpMessage( smsReq.token )
          val ottIdStr = JioStreamsUtil.stringIo[String]( pgpMsg, outputSizeInit = 256 )( pgpUtil.decryptFromSelf(_, pgpKey, _) )
          val ottId = UUID.fromString( ottIdStr )
          LOGGER.trace(s"$logPrefix Found tokenId#$ottId in request")
          (smsReq, ottId)
        }
        fut0.transform { tryRes =>
          val resE = tryRes
            .toEither
            .left.map { ex =>
              LOGGER.error(s"$logPrefix Unabled to decrypt/parse token", ex)
              NotAcceptable("invalid")
            }
          Success( resE )
        }
      }
  }


  /** На основе указателя ottId, получить из БД и распарсить idToken (выставив forUpdate).
    *
    * @param ottId id токена.
    * @param now текущее время запроса.
    * @param req Текущий http-реквест.
    * @return Прочитанные токены.
    */
  private def _idTokenPtrCaptchaOk(ottId: UUID, now: Instant)(implicit req: IReqHdr): DBIOAction[(MOneTimeToken, MIdToken), NoStream, Effect.Read] = {
    lazy val logPrefix = s"_idTokenPtrCaptchaOk()#${now.toEpochMilli}:"
    for {
      // Достать токен по id из базы биллиннга:
      mottOpt0 <- mOneTimeTokens.getById( ottId ).forUpdate
      if {
        val r = mottOpt0.nonEmpty
        if (!r) LOGGER.warn(s"$logPrefix Cannot find token#$ottId in db.")
        r
      }
      mott0 = mottOpt0.get

      // Расшифровать id-токен
      idToken0 = Json
        .parse( mott0.info.get )
        .as[MIdToken]

      // Убедиться, что тип токена соответствует ожиданиям ident-токена.
      if {
        val expected = MIdTokenTypes.IdentVerify
        val r = (idToken0.typ ==* expected)
        if (!r) LOGGER.warn(s"$logPrefix Invalid token type#${idToken0.typ}, but $expected expected.")
        r
      } && {
        val r = idTokenUtil.isConstaintsMeetRequest( idToken0, now )
        if (!r) LOGGER.warn(s"$logPrefix IdToken stored is unrelated to current request session.")
        r
      } && {
        // капча должна быть уже выверена:
        val r = idToken0.idMsgs.exists { idMsg =>
          (idMsg.rcptType ==* MPredicates.JdContent.Image) &&
          idMsg.validated.nonEmpty
        }
        if (!r) LOGGER.warn(s"$logPrefix Captcha IdMsg not exist or invalid.\n ${idToken0.idMsgs.mkString(",\n ")}")
        r
      }
    } yield {
      (mott0, idToken0)
    }
  }


  /** Экшен проверки смс-кода, получает токен с выверенной капчей и запросом смс-кода,
    * возвращает токен с проверенным кодом или ошибку.
    *
    * @return JSON с токеном проверки.
    */
  def smsCodeCheck() = csrf.Check {
    val _theAction = isAnon().async {
      _parseCodeFormReq(Validators.isSmsCodeValid)
    } { implicit request =>
      val now = Instant.now()
      lazy val logPrefix = s"smsCodeCheck()#${now.toEpochMilli}:"
      // Проверить токен по базе.
      val (smsReq, ottId) = request.body

      // TODO Наверное, надо организовать DB-транзакцию, в которой и проводить все проверки с обновлением токена.
      val dbActionTxn = for {
        // Достать токен по id из базы биллиннга:
        (mott0, idToken0) <- _idTokenPtrCaptchaOk( ottId, now )

        // Собрать данные по отправленным смс-кам из токена.
        smsMsgs = idToken0.idMsgs
          .filter(_.rcptType ==* MPredicates.Ident.Phone)

        if {
          val r = smsMsgs.nonEmpty
          if (!r) LOGGER.error(s"$logPrefix IdToken does NOT contain sms-messages.")
          r
        } && {
          // Запретить дальнейшую обработку, если было слишком много ошибок.
          val tooManyErrors = smsMsgs.exists( _.errorsCount > 10 )
          if (tooManyErrors) LOGGER.warn(s"$logPrefix Suppressed bruteforce: too many errors were:\n ${smsMsgs.mkString(",\n ")}")
          !tooManyErrors
        }

        // Дальше кусок непосредственно перемалывания смс-формы.
        // Найти текущее состояние проверяемого смс-кода :
        httpResp <- {
          val someNow = Some(now)
          val frFut: Future[(MIdToken => MIdToken, Result)] = smsReq.formData match {

            // Юзер ввёл код и отправил его на проверку.
            case MCodeFormData(Some(codeTyped), _) =>
              val fr = smsMsgs
                .find(_.checkCode ==* codeTyped)
                .fold [(MIdToken => MIdToken, Result)] {
                  // Не совпал код из смс. Выставить ошибку в id-token для смс.
                  LOGGER.debug(s"$logPrefix Invalid sms-code typed by user: $codeTyped expected, but should be ${smsMsgs.iterator.map(_.checkCode).mkString(", ")}")
                  val f = MIdToken.idMsgs.modify { idMsgs0 =>
                    for (msg0 <- idMsgs0) yield {
                      msg0.rcptType match {
                        case MPredicates.Ident.Phone if msg0.validated.isEmpty =>
                          MIdMsg.errorsCount
                            .modify(_ + 1)( msg0 )
                        case _ => msg0
                      }
                    }
                  }
                  val r = NotAcceptable("error.invalid")
                  (f, r)
                } { smsFound =>
                  // Смс-код совпал с ожидаемым. Обновить id-токен, вернуть положительный ответ.
                  LOGGER.debug(s"$logPrefix SMS code '$codeTyped' matched ok: $smsFound")
                  val f = MIdToken.idMsgs.modify { idMsgs0 =>
                    for (msg0 <- idMsgs0) yield {
                      if (msg0 ===* smsFound) {
                        // Это смс было выверено, код совпал. Обновить состояние.
                        MIdMsg.validated.set( Some(now) )(msg0)
                      } else msg0
                    }
                  }
                  val r = _tokenResp(
                    token = smsReq.token
                  )
                  (f, r)
                }

              Future.successful(fr)

            // Команда к повторной отправке смс.
            case MCodeFormData(_, true) =>
              if (smsMsgs.length >= 3)
                throw new IllegalArgumentException("error.too.many")
              // Надо узнать, достаточно ли времени прошло с момента прошлой отсылки смс.
              val lastSmsMsg = smsMsgs.maxBy(_.sentAt)
              val canReSendAt = lastSmsMsg.sentAt plusSeconds IdentConst.Reg.SMS_CAN_RE_SEND_AFTER_SECONDS
              if ( canReSendAt isBefore now )
                throw new IllegalArgumentException("error.too.fast")

              // Можно переслать смску.
              val smsCode = smsMsgs
                .iterator
                .map(_.checkCode)
                .buffered
                .headOption
                .getOrElse( _genSmsCode() )

              val phoneNumber = smsMsgs
                .iterator
                .flatMap(_.rcpt)
                .next()

              // Запуск повторной отправки смс.
              for {
                sendRes <- _sendIdCodeSms(
                  phoneNumber = phoneNumber,
                  smsCode     = smsCode,
                  ctx         = implicitly[Context]
                )
              } yield {
                // Нужно собрать обновлённый токен на основе результатов отправки:
                val newIdMsgs = _sendSmsRes2IdMsgs(
                  phoneNumber = phoneNumber,
                  smsCode     = smsCode,
                  now         = now,
                  smsSendRes  = sendRes,
                )
                val f = MIdToken.idMsgs.modify( newIdMsgs ++ _)
                val r = _tokenResp( smsReq.token )
                (f, r)
              }

            case other =>
              // Should never happen: непровалидированные данные попали в обработку.
              throw new IllegalStateException(s"$logPrefix Invalid sms-req data, should be already filtered out in BodyParser: $other")
          }

          // накатить изменения и вернуть результат работы
          for {
            (idTokenMod, result) <- DBIO.from( frFut )

            // Собрать обновлённый токен: увеличить счётчик смс-ошибок, обновить дату последней модификации.
            idToken2 = (
              idTokenMod andThen
              MIdToken.dates
                .composeLens( MIdTokenDates.modified )
                .set( someNow )
            )( idToken0 )

            // Сохранить в базу обновлённый токен.
            countUpdated <- mOneTimeTokens.updateExisting {
              mott0.copy(
                dateEnd = now plusSeconds IdentConst.Reg.ID_TOKEN_VALIDITY_SECONDS,
                info    = Some( Json.toJson(idToken2).toString() )
              )
            }
            if countUpdated ==* 1
          } yield {
            result
          }
        }

      } yield {
        httpResp
      }

      slick.db
        .run( dbActionTxn.transactionally )
        .recover { case ex: Throwable =>
          LOGGER.error(s"$logPrefix Failed to check/validate sms-code.", ex)
          NotAcceptable("error.invalid")
        }
    }

    bruteForceProtect( _theAction )
  }


  /** Финал регистрации: юзер подтвердил галочки, юзер ввёл пароль.
    *
    * @return Экшен, возвращающий редирект.
    */
  def regFinalSubmit = csrf.Check {
    bruteForceProtect {
      isAnon().async {
        _parseCodeFormReq( Validators.isPasswordValid )
      } { implicit request =>
        val now = Instant.now()
        val (cfReq, ottId) = request.body
        val password = cfReq.formData.code.get
        lazy val logPrefix = s"regFinalSubmit($ottId)#${now.toEpochMilli}:"
        implicit lazy val ctx = implicitly[Context]

        val dbAction = for {
          // Достать токен по id из базы биллиннга:
          (mott0, idToken0) <- _idTokenPtrCaptchaOk( ottId, now )

          idPreds = (MPredicates.Ident.Phone :: MPredicates.JdContent.Image :: Nil) : List[MPredicate]

          // Убедится, что смс и капча успешно выверены.
          if {
            idPreds.forall { mpred =>
              idToken0.idMsgs.exists { idMsg =>
                val r = (idMsg.rcptType ==* mpred) && idMsg.validated.nonEmpty
                if (r) LOGGER.trace(s"$logPrefix Found validated $idMsg")
                r
              }
            }
          }

          personIdRcptType = MPredicates.Ident.Id

          if {
            // И ещё нет токена сохранённого пароля.
            !idToken0.idMsgs.exists { idMsg =>
              val r = idMsg.rcptType ==* personIdRcptType
              if (r) LOGGER.trace(s"$logPrefix Password idMsg already exist. Token already used: $idMsg")
              r
            }
          }

          // Достать реквизиты юзера с нулевого шага:
          regCreds = idToken0.payload.as[MRegCreds0]

          // Поискать уже существующего юзера в узлах:
          existingPersonNodeOpt <- DBIO.from {
            for {
              existingPersonNodes <- mNodes.dynSearch {
                new MNodeSearchDfltImpl {
                  override def nodeTypes = MNodeTypes.Person :: Nil
                  override def outEdges: Seq[Criteria] = {
                    val cr = Criteria(
                      nodeIds    = regCreds.phone :: Nil,
                      predicates = MPredicates.Ident.Phone :: Nil,
                    )
                    cr :: Nil
                  }
                  // Надо падать на ситуации, когда есть несколько узлов с одним номером телефона.
                  override def limit = 2
                }
              }
            } yield {
              // если для одного номера телефона - несколько номеров, то ситуация неопределена.
              if (existingPersonNodes.lengthCompare(1) > 0) {
                LOGGER.error(s"$logPrefix Two or more existing nodes[${existingPersonNodes.iterator.flatMap(_.id).mkString(",")}] found for phoneIdent[${regCreds.phone}]. Should never happend. Please resolve db consistency by hands.")
                // TODO node-merge Надо запустить авто-объединение узлов и обновление зависимых узлов.
                throw new IllegalStateException("DB consistency in doubt: duplicate phone number. Please check.")
              } else {
                existingPersonNodes.headOption
              }
            }
          }

          // Узел юзера. Может быть, он уже существует.
          personNodeSaved <- {
            // Эдж пароля:
            val passwordEdge = MEdge(
              predicate = MPredicates.Ident.Password,
              info = MEdgeInfo(
                textNi = Some( scryptUtil.mkHash( password ) ),
              )
            )
            val emailEdge = MEdge(
              predicate = MPredicates.Ident.Email,
              nodeIds = Set( regCreds.email ),
              info = MEdgeInfo(
                flag = Some(false),
              )
            )
            existingPersonNodeOpt.fold [DBIOAction[MNode, NoStream, Effect]] {
              // Как и ожидалось, такого юзера нет в базе. Собираем нового юзера:
              LOGGER.debug(s"$logPrefix For phone[${regCreds.phone}] no user exists. Creating new...")
              // Собрать узел для нового юзера.
              val mperson0 = MNode(
                common = MNodeCommon(
                  ntype       = MNodeTypes.Person,
                  isDependent = false,
                  isEnabled   = true,
                ),
                meta = MMeta(
                  basic = MBasicMeta(
                    techName = Some(
                      idToken0.idMsgs
                        .iterator
                        .filter { m =>
                          idPreds contains[MPredicate] m.rcptType
                        }
                        .flatMap(_.rcpt)
                        .mkString(" | ")
                    )
                  )
                ),
                edges = MNodeEdges(
                  out = MNodeEdges.edgesToMap(
                    passwordEdge,
                    // Номер телефона.
                    MEdge(
                      predicate = MPredicates.Ident.Phone,
                      nodeIds   = Set( regCreds.phone ),
                      info = MEdgeInfo(
                        flag = Some(true),
                      )
                    ),
                    // Электронная почта.
                    emailEdge,
                  )
                ),
              )
              // Создать узел для юзера:
              for {
                savedId <- DBIO.from {
                  mNodes.save( mperson0 )
                }
              } yield {
                LOGGER.info(s"$logPrefix Created new user#$savedId for cred[$regCreds]")
                MNode.id
                  .set( Some(savedId) )(mperson0)
              }

            } { personNode0 =>
              // Уже существует узел. Организовать сброс пароля.
              val nodeId = personNode0.id.get
              LOGGER.info(s"$logPrefix Found already existing user#$nodeId for phone[${regCreds.phone}]")
              val mnode1 = MNode.edges
                .composeLens(MNodeEdges.out)
                .modify { edges0 =>
                  // Убрать гарантировано ненужные эджи:
                  val edges1 = edges0
                    .iterator
                    .filter { e0 =>
                      e0.predicate match {
                        // Эджи пароля удаляем безусловно
                        case MPredicates.Ident.Password =>
                          LOGGER.trace(s"$logPrefix Drop edge Password $e0 of node#$nodeId\n $e0")
                          false
                        // Эджи почты: неподтвердённые - удалить.
                        case MPredicates.Ident.Email if !e0.info.flag.contains(true) =>
                          LOGGER.trace(s"$logPrefix Drop edge Email#${e0.nodeIds.mkString(",")} with flag=false on node#$nodeId\n $e0")
                          false
                        // Остальные эджи - пускай живут.
                        case _ => true
                      }
                    }
                    .toStream
                  // Отработать email-эдж. Если он уже есть для текущей почты, то оставить как есть. Иначе - создать новый, неподтверждённый эдж.
                  val edges2 = edges1
                    .find { e =>
                      (e.predicate ==* MPredicates.Ident.Email) &&
                      (e.nodeIds contains regCreds.email)
                    }
                    .fold {
                      // Создать email-эдж
                      LOGGER.trace(s"$logPrefix Inserting Email-edge#${regCreds.email} on node#$nodeId\n $emailEdge")
                      emailEdge #:: edges1
                    } { emailExistsEdge =>
                      LOGGER.trace(s"$logPrefix Keep edge Email#${regCreds.email} as-is on node#$nodeId\n $emailExistsEdge")
                      edges1
                    }

                  MNodeEdges.edgesToMap1( passwordEdge #:: edges2 )
                }( personNode0 )

              DBIO.from {
                for (_ <- mNodes.save( mnode1 ))
                yield MNode.versionOpt.modify { vOpt => Some(vOpt.fold(1L)(_ + 1L)) }(mnode1)
              }
            }
          }

          personId = personNodeSaved.id.get

          // Всё ок. Финализировать idToken:
          idToken2 = {
            LOGGER.trace(s"$logPrefix personId#$personId for $regCreds")
            (
              MIdToken.idMsgs.modify { idMsgs0 =>
                MIdMsg(
                  rcptType  = personIdRcptType,
                  checkCode = "",
                  validated = Some(now),
                  rcpt      = Set( personId ),
                ) :: idMsgs0
              } andThen
              MIdToken.dates
                .composeLens( MIdTokenDates.modified )
                .set( Some(now) )
            )(idToken0)
          }

          // Собрать и сохранить обновлённый ott-токен в базу:
          mott2 = mott0.copy(
            dateEnd = now plusSeconds 30.minutes.toSeconds,
            info    = Some( Json.toJson(idToken2).toString() ),
          )

          countUpdated <- mOneTimeTokens.updateExisting( mott2 )
          if {
            val r = countUpdated ==* 1
            if (!r) {
              LOGGER.error(s"$logPrefix Failed to update ott#${mott2.id}, countUpdated=${countUpdated}")
              mNodes
                .deleteById( personId )
                .onComplete { tryRes =>
                  LOGGER.info(s"$logPrefix Deleted user#$personId due to previous errors.\n r = $tryRes")
                }
            }
            r
          }

        } yield {
          LOGGER.debug(s"$logPrefix Updated ott#${mott2.id} with finalized id-token.")
          (personNodeSaved, regCreds, existingPersonNodeOpt.nonEmpty)
        }

        // Запустить транзацию обновления состояния хранимого токена и создания узла юзера.
        for {
          // Запустить транзакцию по базе с созданием узла.
          (personNode, regCreds, isUserPreviouslyExisted) <- slick.db.run {
            dbAction.transactionally
          }
          personId = personNode.id.get

          // Рега? или восстановление забытого пароля?
          isReg = {
            LOGGER.trace(s"$logPrefix userAlreadyExists?$isUserPreviouslyExisted email=${regCreds.email}")
            !isUserPreviouslyExisted
          }

          // Отправить email, если в email-эдже flag=false для текущего email.
          sendEmailActMsgFut = if (
            personNode.edges
              .withNodePred(regCreds.email, MPredicates.Ident.Email)
              .exists(_.info.flag contains true)
          ) {
            // email уже провалидирован в узле - email отправлять не требуется.
            LOGGER.trace(s"$logPrefix Email[${regCreds.email}] already validated for node#$personId, skipping email-activation msg.")
            Future.successful( None )

          } else {
            LOGGER.debug(s"$logPrefix Sending email-activation msg[${regCreds.email}] for node#$personId ...")
            mailer
              .instance
              .setRecipients( regCreds.email )
              .setSubject {
                val prefixMsgCode =
                  if (isReg) "reg.emailpw.email.subj"
                  else MsgCodes.`Password.recovery`
                s"${ctx.messages(prefixMsgCode)} | ${MsgCodes.`Suggest.io`}"
              }
              .setHtml {
                val tplQs = MEmailRecoverQs(
                  email         = regCreds.email,
                  // Привязать сессию текущего юзера к письму:
                  //nonce         = randomUuid,
                  checkSession  = true,
                )
                val tpl =
                  if (isReg) emailRegMsgTpl
                  else emailPwRecoverTpl
                val html = tpl.render(tplQs, ctx)
                htmlCompressUtil.html4email( html )
              }
              .send()
              .recover { case ex: Throwable =>
                // Подавить ошибку, т.к. телефон всё равно уже провалидирован.
                LOGGER.error(s"$logPrefix Unable to send email to ${regCreds.email}", ex)
                // TODO Удалить email из узла? Или вообще туда почту не сохранять?
                None
              }
          }

          // Поискать adn-узлы в подчинении у юзера.
          userAdnNodesIds <- if (isReg) {
            LOGGER.trace(s"$logPrefix Fresh-registered user does not have adn-nodes attached. Will create new ADN-node.")
            Future.successful( List.empty[String] )
          } else {
            mNodes.dynSearchIds {
              new MNodeSearchDfltImpl {
                override val nodeTypes = MNodeTypes.AdnNode :: Nil
                override val outEdges: Seq[Criteria] = {
                  val cr = Criteria(
                    nodeIds = personId :: Nil,
                    predicates = MPredicates.OwnedBy :: Nil
                  )
                  cr :: Nil
                }
                // Не важно, сколько точно узлов. Надо понять лишь: 0, 1 или более, чтобы сформулировать правильный редирект.
                override def limit = 2
              }
            }
          }

          // Создать узел-магазин начальный, если у юзера нет узлов
          adnNodeCreatedOpt <- if (userAdnNodesIds.isEmpty) {
            for {
              adnNode <- nodesUtil.createUserNode(
                name      = ctx.messages( MsgCodes.`My.node` ),
                personId  = personId,
              )
            } yield {
              LOGGER.info(s"$logPrefix Created ADN-node#${adnNode.id.get} for user#$personId")
              Some(adnNode)
            }
          } else {
            LOGGER.debug(s"$logPrefix Will NOT create new ADN nodes for user#$personId, because user already have ADN-nodes##[${userAdnNodesIds.mkString(", ")},...]")
            Future.successful( None )
          }
          adnNodeCreatedIdOpt = adnNodeCreatedOpt.flatMap(_.id)

          // TODO Хз, надо ли дожидаться отправки email здесь... Пока дожидаемся для отладки и субъективного контроля работоспособности.
          _ <- sendEmailActMsgFut

        } yield {
          // Залогинить юзера, вернуть адрес для редирект
          LOGGER.info(s"$logPrefix Successfully registered/restored user#$personId with adn-node#${adnNodeCreatedIdOpt.orNull}.")

          // Надо сформировать ссылку для редиректа юзера на основе его ADN-узла (узлов).
          val rdrTo = adnNodeCreatedIdOpt
            .orElse {
              OptionUtil.maybe( userAdnNodesIds.lengthCompare(1) ==* 0 ) {
                userAdnNodesIds.head
              }
            }
            .map { toNodeId =>
              // Редирект на единственный узел (созданный или существующий).
              routes.LkAds.adsPage(toNodeId :: Nil)
            }
            .getOrElse {
              // 2 или более узлов. Отправить юзера в lkList. (Или даже 0, внезапно: восстановление пароля без ранее созданного узла).
              routes.MarketLkAdn.lkList()
            }

          _tokenResp( rdrTo.url )
            .addingToSession(
              MSessionKeys.PersonId.value -> personId,
            )
        }
      }
    }
  }

}

