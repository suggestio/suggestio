package controllers

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.{Instant, ZoneOffset}
import java.util.UUID

import javax.inject.{Inject, Singleton}
import controllers.ident._
import io.suggest.ctx.CtxData
import io.suggest.es.model.EsModel
import io.suggest.i18n.MsgCodes
import io.suggest.id.login.{ILoginFormPages, MEpwLoginReq}
import io.suggest.id.reg.MEpwRegReq
import io.suggest.init.routed.MJsInitTargets
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.sec.csp.Csp
import io.suggest.sec.util.{PgpUtil, ScryptUtil}
import io.suggest.session.{LongTtl, MSessionKeys, ShortTtl, Ttl}
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
import views.html.lk.login._
import japgolly.univeq._
import models.im.MCaptchaSecret
import org.apache.commons.io.IOUtils
import play.api.libs.json.Json
import util.sec.CspUtil
import views.html.ident.recover.emailPwRecoverTpl

import scala.concurrent.Future
import scala.util.{Failure, Success}

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
    val ownSecretKeyFut = pgpUtil.getLocalStorKey()
    val now = Instant.now()
    lazy val logPrefix = s"epw2RegSubmit()#${now.toEpochMilli}:"

    isAnon().async {
      parse
        .json[MEpwRegReq]
        // Начать с синхронной валидации данных:
        .validate { epwReg =>
          // Проверить валидность email:
          Either.cond(
            test = {
              val vldRes = Constraints.emailAddress.apply( epwReg.email )
              val r = vldRes ==* Valid
              if (!r) LOGGER.warn(s"$logPrefix Invalid email address:\n email = ${epwReg.email}\n vld res = $vldRes")
              r
            },
            right = epwReg,
            left  = NotAcceptable("email.invalid")
          )
          // Поверхностно проверить валидность поля капчи (почистить, проверить длину):
          .filterOrElse(
            {epwReg2 =>
              epwReg2.captchaTyped.length ==* captchaUtil.DIGITS_CAPTCHA_LEN
            },
            {
              LOGGER.warn(s"$logPrefix Captcha len invalid: ${epwReg.captchaTyped} expected=${captchaUtil.DIGITS_CAPTCHA_LEN} irl=${epwReg.captchaTyped.length}")
              NotAcceptable("captcha.invalid")
            }
          )
        }
        // Отработать проверку капчи:
        // - проверить соответствие присланной капчи с pgp-шифротекстом.
        // - проверить токен по базе, и сохранить в БД использованный токен.
        .validateM { epwReg =>
          val pgpSecret = pgpUtil.unminifyPgpMessage( epwReg.captchaSecret )
          for {
            // Получить pgp-ключ на руки:
            ownSecretKey <- ownSecretKeyFut

            // Быстрые синхронные проверки секрета капчи:
            captchaSecretEith = {
              val baos = new ByteArrayOutputStream( 512 )
              val captchaSecretJsonE = try {
                pgpUtil.decryptFromSelf(
                  data = IOUtils.toInputStream( pgpSecret, StandardCharsets.UTF_8 ),
                  key  = ownSecretKey,
                  out  = baos,
                )
                Right( new String( baos.toByteArray ) )
              } catch {
                case ex: Throwable =>
                  // Не удалось расшифровать pgp-сообщение.
                  LOGGER.warn(s"$logPrefix Unable to parse pgp secret:\n $pgpSecret", ex)
                  Left( BadRequest("Invalid request body") )
              } finally {
                baos.close()
              }

              captchaSecretJsonE
                // Расшифровать присланный шифротекст капчи.
                .right.flatMap { captchaSecretJson =>
                  Json
                    .parse( captchaSecretJson )
                    .validate[MCaptchaSecret]
                    .asEither
                    .left.map { jsonParseErrors =>
                      // Should never happen: непарсится расшифрованный текст
                      LOGGER.error(s"$logPrefix Failed to parse decrypted JSON:\n $captchaSecretJson\n ${jsonParseErrors.mkString("\n ")}")
                      InternalServerError
                    }
                }
                // Сверить TTL секрета капчи:
                .filterOrElse(
                  {captchaSecret =>
                    val dateEnd = captchaUtil.captchaTtl( captchaSecret.dateCreated )
                    val r = dateEnd isAfter now
                    if (!r) LOGGER.warn(s"$logPrefix Expired captcha#${captchaSecret.captchaUid}, dateEnd=${dateEnd.atOffset(ZoneOffset.UTC)} now=${now.atOffset(ZoneOffset.UTC)}")
                    r
                  },
                  NotAcceptable( "captcha.invalid" )
                )
                // Проверить соответствие введённой капчи и реальной.
                .filterOrElse(
                  {captchaSecret =>
                    // Оставить только символы
                    val captchaTypedNorm = epwReg.captchaTyped
                      .replaceAll("\\s+", "")
                    // По идее, в капче только цифры, но вдруг это изменится - сравниваем через equalsIgnoreCase
                    val r = captchaSecret.captchaText equalsIgnoreCase captchaTypedNorm
                    if (r) LOGGER.trace(s"Captcha matched ok = $captchaTypedNorm")
                    else   LOGGER.warn(s"Captcha mismatch, typed=${captchaTypedNorm} expected=${captchaSecret.captchaText}")
                    r
                  },
                  NotAcceptable("captcha.invalid")
                )
            }

            // Капча проверена, но возможно, что она уже бывшая в употреблении. Надо провалидировать id капчи по базе токенов.
            isOk <- captchaSecretEith.fold(
              {_ =>
                Future.successful(captchaSecretEith)
              },
              {capthaSecret =>
                slick.db.run {
                  captchaUtil
                    .ensureCaptchaOtt( capthaSecret )
                }
                  .transform {
                    case Failure(ex) =>
                      LOGGER.warn(s"$logPrefix Captcha already used")
                      LOGGER.trace("captcha verify error:", ex)
                      Success( Left( ExpectationFailed("captcha.expired") ) )
                    case Success(_) =>
                      LOGGER.trace(s"$logPrefix Captcha marked as USED")
                      Success( captchaSecretEith )
                  }
              }
            )

          } yield {
            LOGGER.trace(s"$logPrefix Captcha checks done.")
            for (captchaSecret <- isOk.right) yield
              (epwReg, captchaSecret)
          }
        }

    } { implicit request =>
      val (epwReg, captchaSecret) = request.body
      LOGGER.trace(s"$logPrefix email=${epwReg.email} capTyped=${epwReg.captchaTyped}")

      // Проверки выполнены.
      val email1 = epwReg.email
        .trim
        .toLowerCase()

      // Проверить, что там с почтовым адресом?
      for {

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
      }
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

