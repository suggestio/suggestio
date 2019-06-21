package controllers

import java.time.Instant
import java.util.UUID

import javax.inject.{Inject, Singleton}
import controllers.ident._
import io.suggest.ctx.CtxData
import io.suggest.es.model.EsModel
import io.suggest.i18n.MsgCodes
import io.suggest.id.IdentConst
import io.suggest.id.login.{ILoginFormPages, MEpwLoginReq}
import io.suggest.id.reg.{MCodeFormData, MCodeFormReq, MRegCaptchaReq, MRegTokenResp}
import io.suggest.id.token.{MIdMsg, MIdToken, MIdTokenConstaints, MIdTokenDates, MIdTokenTypes}
import io.suggest.init.routed.MJsInitTargets
import io.suggest.mbill2.m.ott.MOneTimeTokens
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.MNodes
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
import models.im.MCaptchaSecret
import models.sms.{ISmsSendResult, MSmsSend}
import play.api.libs.json.Json
import play.api.mvc.Result
import util.sec.CspUtil
import util.sms.SmsSendUtil

import scala.collection.immutable.ListMap
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Success, Try}

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
        .json[MRegCaptchaReq]
        // Начать с синхронной валидации присланных данных. Делаем это прямо тут, хотя по логике оно должно жить в теле экшена.
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
    } { implicit request =>
      LOGGER.trace(s"$logPrefix email=${request.body.creds0.email} capTyped=${request.body.captcha.typed}")

      implicit lazy val ctx = implicitly[Context]

      for {
        // Проверить капчу, сразу отметив её в базе, как использованную:
        captchaVldRes <- captchaUtil.validateAndMarkAsUsed( request.body.captcha )

        pgpKeyFut = pgpUtil.getLocalStorKey()

        // Убедится, что капча проверена успешно:
        (_, mott0) = captchaVldRes
          .left.map { emsg =>
            throw new IllegalArgumentException(emsg)
          }
          .right.get

        creds02 = request.body.creds0.copy(
          email = request.body.creds0
            .email
            .trim
            .toLowerCase(),
          // TODO Задействовать libphonenumber или что-то этакое.
          phone = request.body.creds0.phone.trim,
        )

        // Отправить смс с кодом проверки.
        smsCode = _genSmsCode()

        // Запуск отправки смс-кода в фоне.
        smsSendResFut = _sendIdCodeSms(
          phoneNumber = creds02.phone,
          smsCode     = smsCode,
          ctx         = ctx
        )

        idTokPayload = Json.toJson( creds02 )

        smsSendRes <- smsSendResFut

        idToken = {
          MIdToken(
            typ    = MIdTokenTypes.IdentVerify,
            idMsgs = _sendSmsRes2IdMsgs(
              phoneNumber = creds02.phone,
              smsCode     = smsCode,
              now         = now,
              smsSendRes  = smsSendRes,
            ),
            payload   = idTokPayload,
            // Токен только для анонимуса.
            constraints = MIdTokenConstaints(
              personIdsC = Some( Set.empty ),
              // TODO sessionId задать здесь, чтобы привязать токен к сессии.
            ),
            dates = MIdTokenDates(
              ttlSeconds = IdentConst.Reg.ID_TOKEN_VALIDITY_SECONDS,
            )
          )
        }

        idTokenJsonStr = Json.toJson( idToken ).toString()

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
        val cipherText = JioStreamsUtil.stringIo[String]( idToken.ott.toString, 512 )( pgpUtil.encryptForSelf(_, pgpKey, _) )

        _tokenResp(
          token = pgpUtil.minifyPgpMessage( cipherText )
        )
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

    bruteForceProtect( _theAction )
  }


  /** Экшен проверки смс-кода, получает токен с выверенной капчей и запросом смс-кода,
    * возвращает токен с проверенным кодом или ошибку.
    *
    * @return JSON с токеном проверки.
    */
  def smsCodeCheck() = csrf.Check {
    val now = Instant.now()
    lazy val logPrefix = s"smsCodeCheck()#${now.toEpochMilli}:"
    lazy val pgpKeyFut = pgpUtil.getLocalStorKey()

    val _theAction = isAnon().async {
      parse
        .json[MCodeFormReq]
        .validate { smsReq =>
          Either.cond(
            test  = smsReq.formData match {
              case MCodeFormData(Some(smsCode), false) =>
                Validators.isSmsCodeValid( smsCode )
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
            val ottIdStr = JioStreamsUtil.stringIo[String]( smsReq.token )( pgpUtil.decryptFromSelf(_, pgpKey, _) )
            val ottId = UUID.fromString( ottIdStr )
            LOGGER.debug(s"$logPrefix Found token ID#$ottId")
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
    } { implicit request =>
      // Проверить токен по базе.
      val (smsReq, ottId) = request.body

      implicit lazy val ctx = implicitly[Context]

      // TODO Наверное, надо организовать DB-транзакцию, в которой и проводить все проверки с обновлением токена.
      val dbActionTxn = for {
        // Достать токен по id из базы биллиннга:
        mottOpt0 <- mOneTimeTokens.getById( ottId ).forUpdate
        mott0 = mottOpt0.get
        if !(mott0.dateEnd isBefore now)

        idTokenCipherText = mott0.info.get

        // Ключ уже получен на стадии BodyParser'а, поэтому паузы тут особо не будет.
        pgpKey <- DBIO.from( pgpKeyFut )

        // Расшифровать id-токен
        idToken0 = {
          val idTokenStr = JioStreamsUtil.stringIo[String]( idTokenCipherText, 1024 )( pgpUtil.decryptFromSelf(_, pgpKey, _) )
          Json
            .parse(idTokenStr)
            .as[MIdToken]
        }

        // Убедиться, что тип токена соответствует ожиданиям ident-токена.
        if (idToken0.typ ==* MIdTokenTypes.IdentVerify) &&
           !(idToken0.dates.bestBefore isBefore now) &&
           idTokenUtil.isConstaintsMeetRequest(idToken0) &&
           // капча должна быть уже выверена:
           idToken0.idMsgs.exists { idMsg =>
             (idMsg.rcptType ==* MPredicates.JdContent.Image) &&
             idMsg.validated.nonEmpty
           }

        // Собрать данные по отправленным смс-кам из токена.
        smsRcptType = MPredicates.Ident.Phone
        smsMsgs = idToken0.idMsgs
          .filter(_.rcptType ==* smsRcptType)

        if {
          val r = smsMsgs.nonEmpty
          if (!r) LOGGER.error(s"$logPrefix IdToken does NOT contain sms-messages.")
          r
        }

        // Запретить дальнейшую обработку, если было слишком много ошибок.
        if {
          val tooManyErrors = !smsMsgs.exists( _.errorsCount > 10 )
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
                  ctx         = ctx
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

}

