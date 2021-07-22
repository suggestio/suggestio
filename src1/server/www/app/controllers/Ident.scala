package controllers

import java.time.{Instant, ZoneOffset}
import java.util.UUID
import javax.inject.Inject
import io.suggest.auth.{AuthenticationException, AuthenticationResult}
import io.suggest.captcha.MCaptchaCheckReq
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.ctx.CtxData
import io.suggest.err.MCheckException
import io.suggest.es.model.{EsModel, MEsNestedSearch, MEsUuId}
import io.suggest.ext.svc.MExtService
import io.suggest.i18n.MsgCodes
import io.suggest.id.IdentConst
import io.suggest.id.login.MEpwLoginReq
import io.suggest.id.pwch.MPwChangeForm
import io.suggest.id.reg.{MCodeFormData, MCodeFormReq, MRegCreds0, MRegTokenResp}
import io.suggest.id.token.{MIdMsg, MIdToken, MIdTokenConstaints, MIdTokenDates, MIdTokenTypes}
import io.suggest.init.routed.MJsInitTargets
import io.suggest.mbill2.m.ott.{MOneTimeToken, MOneTimeTokens}
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicate, MPredicates}
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.meta.{MBasicMeta, MMeta, MPersonMeta}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.sec.util.{PgpUtil, ScryptUtil}
import io.suggest.session.{CustomTtl, LongTtl, MSessionKeys, ShortTtl, Ttl}
import io.suggest.spa.SioPages
import io.suggest.streams.JioStreamsUtil
import io.suggest.text.Validators
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.ueq.UnivEqUtil._
import models.mctx.Context
import models.usr._
import play.api.data.validation.{Constraints, Valid}
import util.acl._
import util.adn.NodesUtil
import util.captcha.CaptchaUtil
import util.ident.{IdTokenUtil, IdentUtil}
import util.mail.IMailerWrapper
import io.suggest.ueq.UnivEqUtilJvm._
import views.html.ident.reg.email.emailRegMsgTpl
import views.html.lk.login._
import japgolly.univeq._
import models.req.IReqHdr
import models.sms.{ISmsSendResult, MSmsSend}
import play.api.libs.json.Json
import play.api.mvc.Result
import util.sec.CspUtil
import util.sms.SmsSendUtil
import util.xplay.LangUtil

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Success

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 11:47
 * Description: Контроллер обычного логина в систему.
 * Обычно логинятся через email/телефон и пароль.
 * Но есть и поддержка логина через внешнего провайдера.
 */
final class Ident @Inject() (
                              sioControllerApi     : SioControllerApi,
                            )
  extends MacroLogsImpl
{

  import sioControllerApi._
  import mCommonDi.{ec, csrf, slick, htmlCompressUtil}
  import mCommonDi.current.injector

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mPersonIdentModel = injector.instanceOf[MPersonIdentModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val mailer = injector.instanceOf[IMailerWrapper]
  private lazy val identUtil = injector.instanceOf[IdentUtil]
  private lazy val nodesUtil = injector.instanceOf[NodesUtil]
  private lazy val captchaUtil = injector.instanceOf[CaptchaUtil]
  private lazy val canConfirmEmailPwReg = injector.instanceOf[CanConfirmEmailPwReg]
  private lazy val bruteForceProtect = injector.instanceOf[BruteForceProtect]
  private lazy val scryptUtil = injector.instanceOf[ScryptUtil]
  private lazy val canConfirmIdpReg = injector.instanceOf[CanConfirmIdpReg]
  private lazy val langUtil = injector.instanceOf[LangUtil]
  private lazy val pgpUtil = injector.instanceOf[PgpUtil]
  private lazy val cspUtil = injector.instanceOf[CspUtil]
  private lazy val smsSendUtil = injector.instanceOf[SmsSendUtil]
  private lazy val mOneTimeTokens = injector.instanceOf[MOneTimeTokens]
  private lazy val idTokenUtil = injector.instanceOf[IdTokenUtil]
  private lazy val isAdnNodeAdminOptOrAuth = injector.instanceOf[IsAdnNodeAdminOptOrAuth]
  private lazy val isAnon = injector.instanceOf[IsAnon]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val ignoreAuth = injector.instanceOf[IgnoreAuth]
  private lazy val canLoginVia = injector.instanceOf[CanLoginVia]
  private lazy val lkLoginTpl = injector.instanceOf[LkLoginTpl]

  import esModel.api._
  import mPersonIdentModel.api._
  import slick.profile.api._


  /**
   * Юзер разлогинивается. Выпилить из сессии данные о его логине.
   * @return Редирект на главную, ибо анонимусу идти больше некуда.
   */
  def logout() = csrf.Check {
    // TODO Добавить поддержку помечания истёкших сессий через ottID внутри сессии.
    Action { implicit request =>
      val toPage = models.MAIN_PAGE_CALL

      // Если выставлен X-Requested-With, то редирект возвращать не надо:
      val resp = if (request.headers.get(X_REQUESTED_WITH).isEmpty) {
        Redirect( toPage )
      } else {
        Ok( toPage.toString )
      }

      resp.removingFromSession( MSessionKeys.removeOnLogout.map(_.value): _* )
    }
  }


  /** Отредиректить юзера куда-нибудь. */
  def rdrUserSomewhere() = isAuth().async { implicit request =>
    identUtil.redirectUserSomewhere(request.user.personIdOpt.get)
  }



  // -----------------------------------------------------------------------------------
  // - Login v2 ------------------------------------------------------------------------
  // -----------------------------------------------------------------------------------

  /** Страница с отдельной формой логина по имени-паролю.
    * @param lfp Парсится на стороне сервера.
    * @return Страница с логином.
    */
  def loginFormPage(lfp: SioPages.Login) = csrf.AddToken {
    isAnon().async { implicit request =>
      import cspUtil.Implicits._

      implicit val ctxData = CtxData(
        jsInitTargets = MJsInitTargets.LoginForm :: Nil
      )
      Ok( lkLoginTpl() )
        .withCspHeader( cspUtil.CustomPolicies.Captcha )
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
              val ex = new IllegalStateException(s"$logPrefix Many nodes with same username: internal data-defect or vulnerability: [${personsFound.iterator.flatMap(_.id).mkString(", ")}] Giving up.")
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

                val rdrPathFut = getRdrUrl(r) {
                  identUtil.redirectCallUserSomewhere(personId)
                }
                // Реализация длинной сессии при наличии флага rememberMe.
                val ttl: Ttl =
                  if (request.body.isForeignPc) ShortTtl
                  else LongTtl

                val addToSession = ttl.addToSessionAcc(
                  (MSessionKeys.PersonId.value -> personId) ::
                  Nil
                )

                // Выставить язык, сохраненный ранее в MPerson
                val langOpt = langUtil.getLangFrom( Some(mperson) )
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
        msgs = Map.empty + (
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
        rcpt      = Set.empty + phoneNumber + smsRuPhoneNumber,
        sentAt    = now,
        msgId     = sentStatus.smsId,
        checkCode = smsCode,
      )
    })
      .toList
  }


  private def _tokenResp(respBody: MRegTokenResp): Result = {
    Ok( Json.toJson(respBody) )
      .withHeaders(
        EXPIRES       -> "0",
        PRAGMA        -> "no-cache",
        CACHE_CONTROL -> "no-store, no-cache, no-store",
      )
  }
  private def _tokenResp(token: String): Result =
    _tokenResp( MRegTokenResp(token) )


  /** Сабмит данных нулевого шага для перехода к капче.
    * Реквизиты регистрации заворачиваются в токен, шифруются и отправляются клиенту.
    *
    * @return Ответ в виде токена, с помощью которого можно организовать шаг капчи.
    */
  def regStep0Submit() = csrf.Check {
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
                // TODO Объединить с normalize phone number, т.к. в обоих случаях происходит просто отсев нецифровых символов.
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
      parse.json[MCaptchaCheckReq]
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
          .getOrElse(throw new NoSuchElementException("captcha invalid"))

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
                .nextOption()
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
                val f = MIdToken.idMsgs.modify( newIdMsgs ++ _ )
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
  def regFinalSubmit() = csrf.Check {
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

          idPreds: List[MPredicate] =
            MPredicates.Ident.Phone ::
            MPredicates.JdContent.Image ::
            Nil

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
                new MNodeSearch {
                  override val nodeTypes = MNodeTypes.Person :: Nil
                  override val outEdges: MEsNestedSearch[Criteria] = {
                    val cr = Criteria(
                      nodeIds    = regCreds.phone :: Nil,
                      predicates = MPredicates.Ident.Phone :: Nil,
                    )
                    MEsNestedSearch.plain( cr )
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
              nodeIds = Set.empty + regCreds.email,
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
                      nodeIds   = Set.empty + regCreds.phone,
                      info = MEdgeInfo(
                        flag = OptionUtil.SomeBool.someTrue,
                      )
                    ),
                    // Электронная почта.
                    emailEdge,
                  )
                ),
              )
              // Создать узел для юзера:
              for {
                docMetaSaved <- DBIO.from {
                  mNodes.save( mperson0 )
                }
              } yield {
                LOGGER.info(s"$logPrefix Created new user#${docMetaSaved.id.orNull} for cred[$regCreds]")
                mNodes.withDocMeta( mperson0, docMetaSaved )
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
                    .to( LazyList )

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
                mNodes.saveReturning( mnode1 )
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
                  rcpt      = Set.empty + personId,
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
                  checkSession  = true,
                  // id узла юзера
                  nodeId        = personNode.id,
                )
                val html = emailRegMsgTpl(tplQs)(ctx)
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
              new MNodeSearch {
                override val nodeTypes = MNodeTypes.AdnNode :: Nil
                override val outEdges: MEsNestedSearch[Criteria] = {
                  val cr = Criteria(
                    nodeIds = personId :: Nil,
                    predicates = MPredicates.OwnedBy :: Nil
                  )
                  MEsNestedSearch.plain( cr )
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
            .fold {
              // 2 или более узлов. Отправить юзера в lkList. (Или даже 0, внезапно: восстановление пароля без ранее созданного узла).
              routes.MarketLkAdn.lkList()
            } { toNodeId =>
              // Редирект на единственный узел (созданный или существующий).
              routes.LkAds.adsPage(toNodeId :: Nil)
            }

          _tokenResp( rdrTo.url )
            .addingToSession(
              MSessionKeys.PersonId.value -> personId,
            )
        }
      }
    }
  }


  // Смена пароля v2 через react-форму.

  /** Экшен формы смены пароля для залогиненного юзера.
    *
    * @param onNodeIdOptU id узла, если задан.
    * @return Экшен, возвращающий html-страницу с react-формой смены пароля.
    */
  def pwChangeForm(onNodeIdOptU: Option[MEsUuId]) = csrf.AddToken {
    val onNodeIdOpt = onNodeIdOptU.toStringOpt
    isAdnNodeAdminOptOrAuth( onNodeIdOpt, U.Lk ).async { implicit request =>
      // Закинуть данные инициализации в корневой шаблон:
      for {
        ctxData0 <- request.user.lkCtxDataFut
      } yield {
        implicit val ctxData1 = CtxData.jsInitTargets
          .modify( MJsInitTargets.PwChange :: _ )(ctxData0)
        implicit val ctx = implicitly[Context]

        Ok( LkPwChangeTpl( request.mnodeOpt )(ctx) )
      }
    }
  }


  /** На будущее, если необходимо явно перехэшировать пароль, который вроде бы не изменился. */
  private def _isForceReHashPassword(e: MEdge): Boolean = {
    // Если изменится формат пароля с исходного scrypt на что-либо ещё, то тут можно организовать проверку.
    false
  }


  /** Сабмит формы изменения пароля.
    *
    * @return 200, когда всё ок.
    */
  def pwChangeSubmit() = csrf.Check {
    bruteForceProtect {
      isAuth().async(
        parse
          .json[MPwChangeForm]
          // Убедиться, что присланы корректные пароли.
          .validate { form0 =>
            Either.cond(
              test  = Validators.isPasswordValid( form0.pwNew ),
              left  = {
                val msgCode = MsgCodes.`Inacceptable.password.format`
                val ex = MCheckException(
                  msgCode,
                  fields = MPwChangeForm.Fields.PW_NEW_FN :: Nil,
                )
                NotAcceptable( Json.toJson(ex) )
              },
              right = form0
            )
          }
      ) { implicit request =>
        lazy val logPrefix = s"pwChangeSubmit(u#${request.user.personIdOpt.orNull})#${System.currentTimeMillis()}:"

        implicit lazy val ctx = implicitly[Context]

        // 2018-02-27 Менять пароль может только юзер, уже имеющий логин. Остальные - идут на госуслуги.
        val resOkFut = for {
          personNode0 <- request.user.personNodeFut

          p = MPredicates.Ident

          pwEdges = personNode0.edges
            .withPredicateIter( p.Password )
            .toList

          oldPwEdgeOpt = pwEdges.find { e =>
            e.info.textNi
              .exists( scryptUtil.checkHash(request.body.pwOld, _) )
          }

          if {
            val r = oldPwEdgeOpt.nonEmpty
            if (!r) {
              LOGGER.warn(s"$logPrefix Current password does not match to pw.typed by user.")
              val msgCode = MsgCodes.`Invalid.password`
              throw new MCheckException(
                getMessage       = msgCode,
                fields           = MPwChangeForm.Fields.PW_OLD_FN :: Nil,
                localizedMessage = Some( ctx.messages(msgCode) )
              )
            }
            r
          }

          oldPwEdge = oldPwEdgeOpt.get

          // Залить обновлённые эджи в узел юзера:
          _ <- {
            if (
              (request.body.pwOld ==* request.body.pwNew) ||
              _isForceReHashPassword(oldPwEdge)
            ) {
              // Пароль не изменился - ничего пересохранять и надо.
              LOGGER.info(s"$logPrefix Password not changed - keeping node as-is.")
              Future.successful( personNode0 )
            } else {
              LOGGER.trace(s"$logPrefix Found matching password-edge, will update:\n $oldPwEdge")
              val pwNewHashSome = Some( scryptUtil.mkHash( request.body.pwNew ) )

              val pwEdgeModF = MEdge.info
                .composeLens( MEdgeInfo.textNi )
                .set( pwNewHashSome )

              val mnodeModF = MNode.edges
                .composeLens( MNodeEdges.out )
                .modify { edges0 =>
                  for (e <- edges0) yield {
                    // Заменяем пароль в эдже старого пароля.
                    // TODO Возможно, надо сохранять эдж старого пароля с flag=false, а при логине по старому паролю сообщать, что использованный старый пароль был ранее изменён?
                    // Сравниваем по == вместо eq, на случай теоретического конфликта версий в tryUpdate()
                    if (e ==* oldPwEdge) pwEdgeModF(e)
                    else e
                  }
                }

              mNodes.tryUpdate(personNode0)( mnodeModF )
            }
          }

        } yield {
          // Вернуть ответ о том, что всё ок.
          LOGGER.info(s"$logPrefix User successfully changed password.")
          NoContent
        }

        resOkFut.recoverWith {
          case ex: Throwable =>
            ex match {
              case mce: MCheckException =>
                NotAcceptable( Json.toJson(mce) )
              case _ =>
                val msg = s"$logPrefix Failed to update password for user#${request.user.personIdOpt.orNull}"
                if (ex.isInstanceOf[NoSuchElementException]) LOGGER.warn(msg)
                else LOGGER.error(msg, ex)

                ExpectationFailed
            }
        }
      }
    }
  }


  /** Юзер возвращается по ссылке из письма. Отрендерить страницу завершения регистрации. */
  def emailReturn(qs: MEmailRecoverQs) = {
    canConfirmEmailPwReg(qs).async { implicit request =>
      // ActionBuilder уже выверил всё. Нужно показать юзеру страницу с формой ввода пароля, названия узла и т.д.
      // TODO Выставить email как подтверждённый, сохранить ott(qs.nonce) и отредиректить юзера куда-нибудь с flashing()

      val now = Instant.now()
      lazy val logPrefix = s"emailReturn(${qs.email})#${now.toEpochMilli}:"
      LOGGER.trace(s"$logPrefix $qs")

      val saveFut = slick.db.run {
        val dbAction = for {
          mott <- mOneTimeTokens.insertOne {
            MOneTimeToken(
              id          = qs.nonce,
              dateCreated = now,
              dateEnd     = now plusSeconds canConfirmEmailPwReg.MAX_AGE.toSeconds
            )
          }

          // Всё заинзертилось - обновляем узел в рамках текущей транзакции.
          _ <- DBIO.from {
            mNodes.tryUpdate(request.mnode) {
              // Найти email-эдж и выставить flag=true
              MNode.edges
                .composeLens( MNodeEdges.out )
                .modify { edges0 =>
                  // Найти существующий email-эдж, если он есть (а это не обязательно)
                  var hasChangedEdge = false
                  val edges2 = for {
                    e <- edges0.to(LazyList)
                  } yield {
                    if (
                      (e.predicate ==* MPredicates.Ident.Email) &&
                      (e.nodeIds contains qs.email) &&
                      !(e.info.flag contains true)
                    ) {
                      hasChangedEdge = true
                      (MEdge.info composeLens MEdgeInfo.flag set OptionUtil.SomeBool.someTrue)(e)
                    } else {
                      e
                    }
                  }
                  if (!hasChangedEdge) {
                    LOGGER.trace(s"$logPrefix Will add new email[${qs.email}] edge to node#${qs.nodeId.orNull}")
                    val ee = MEdge(
                      predicate = MPredicates.Ident.Email,
                      nodeIds = Set.empty + qs.email,
                      info = MEdgeInfo(
                        flag = OptionUtil.SomeBool.someTrue,
                      )
                    )
                    ee #:: edges2
                  } else {
                    edges2
                  }
                }
            }
          }


        } yield {
          LOGGER.debug(s"$logPrefix Created ott#${mott.id}, updated node#${request.mnode.idOrNull}")
          mott
        }
        dbAction.transactionally
      }

      for {
        _ <- saveFut

        res0 <- request.user.personIdOpt.fold [Future[Result]] {
          // Юзер текуший не залогинен - отредиректить его на логин-форму.
          Future.successful( Redirect(routes.Ident.loginFormPage()) )
        }( identUtil.redirectUserSomewhere )
      } yield {
        res0
          .flashing( FLASH.SUCCESS -> MsgCodes.`Changes.saved` )
      }
    }
  }


  // -------------------------------------------------------------------------------------------------------------------
  // ExternalLogin - логин через внешнего провайдера.
  // -------------------------------------------------------------------------------------------------------------------

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
  protected def handleAuth1(extService: MExtService, redirectTo: Option[String]) = canLoginVia(extService).async { implicit request =>
    lazy val logPrefix = s"handleAuth1($extService):"
    request.apiAdp.authenticateFromRequest().flatMap {

      case _: AuthenticationResult.AccessDenied =>
        Redirect( routes.Ident.loginFormPage() )
          .flashing(FLASH.ERROR -> "login.accessDenied")

      case failed: AuthenticationResult.Failed =>
        LOGGER.error(s"$logPrefix authentication failed, reason: ${failed.error}")
        throw AuthenticationException()

      case flow: AuthenticationResult.NavigationFlow => Future.successful {
        val r0 = flow.result
        redirectTo.fold( r0 ) { url =>
          r0.addingToSession(MSessionKeys.ExtLoginData.value -> url)
        }
      }

      case authenticated: AuthenticationResult.Authenticated =>
        import esModel.api._
        import mPersonIdentModel.api._

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
                      nodeIds   = Set.empty + profile.userId,
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

              LOGGER.debug(s"$logPrefix Registering new user via service#${extService}:\n $profile ...")
              mNodes.saveReturning(mperson0)

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
            val rdrUrlFut = _urlFromSession(request.session, personId)
            for (url <- rdrUrlFut) yield
              Redirect(url)
          }

          // Сборка новой сессии: чистка исходника, добавление новых ключей, относящихся к идентификации.
          session1 = {
            val addToSession0 = (MSessionKeys.PersonId.value -> personId) :: Nil
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
          langOpt = langUtil.getLangFrom( Some(mperson2) )
        } yield {
          langUtil.setLangCookie( rdr2, langOpt )
        }

    }.recover {
      case e =>
        LOGGER.error("Unable to log user in. An exception was thrown", e)
        Redirect( routes.Ident.loginFormPage() )
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
  private def _urlFromSession(ses: play.api.mvc.Session, personId: String): Future[String] = {
    FutureUtil.opt2future( ses.get(MSessionKeys.ExtLoginData.value) ) {
      identUtil.redirectCallUserSomewhere(personId)
        .map(_.url)
    }
  }


  /**
    * Юзер, залогинившийся через провайдера, хочет создать ноду.
    * @return Страницу с колонкой подтверждения реги.
    */
  def idpConfirm() = {
    canConfirmIdpReg().async { implicit request =>
      // Развернуть узел для юзера, отобразить страницу успехоты.
      implicit val ctx = implicitly[Context]
      // TODO Здесь нужно создать шаг галочек: перс.данные, соглашение. Можно пропихнуть id-токен в lk-login-форму, чтобы только галочки отобразились.
      for {
        mnode <- nodesUtil.createUserNode(
          name     = ctx.messages( MsgCodes.`My.node` ),
          personId = request.user.personIdOpt.get
        )
      } yield {
        // Отредиректить на созданный узел юзера.
        Redirect( routes.LkAdnEdit.editNodePage( mnode.id.get ) )
          .flashing( FLASH.SUCCESS -> MsgCodes.`New.shop.created.fill.info` )
      }
    }
  }


  /** Поддержка push-уведомления со стороны внешнего сервиса. */
  def extServicePushPost(extService: MExtService) = _extServicePushGet(extService)
  def extServicePushGet(extService: MExtService) = _extServicePushGet(extService)
  private def _extServicePushGet(extService: MExtService) = ignoreAuth() { implicit request =>
    LOGGER.error(s"extServicePush(${extService}): Not implemented.\n HTTP ${request.method} ${request.host} ${request.uri}\n ${request.body}")
    NotImplemented("Not implemented yet, sorry!")
  }

}

