package util.ident.esia

import java.nio.charset.StandardCharsets
import java.security.cert.X509Certificate
import java.security.{PrivateKey, PublicKey, Security, Signature}
import java.time.{Instant, OffsetDateTime, ZoneId}
import java.util.{Base64, UUID}

import controllers.routes
import io.jsonwebtoken.{Claims, Jwts}
import io.jsonwebtoken.impl.FixedClock
import io.suggest.auth.{AuthenticationMethod, AuthenticationResult, OAuth2Info, UserProfile}
import io.suggest.ext.svc.MExtServices
import io.suggest.primo.{MTestProdMode, MTestProdModes}
import io.suggest.proto.http.HttpConst
import io.suggest.sec.m.MKeyStore
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.ueq.UnivEqUtil._
import javax.inject.Inject
import util.ident.IExtLoginAdp
import java.{util => ju}

import io.suggest.err.ErrorConstants
import io.suggest.session.MSessionKeys

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import japgolly.univeq._
import models.mctx.ContextUtil
import models.req.MLoginViaReq
import models.usr.esia._
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.{CMSProcessableByteArray, CMSSignedData, CMSSignedDataGenerator}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.{JcaContentSignerBuilder, JcaDigestCalculatorProviderBuilder}
import play.api.Configuration
import play.api.libs.json.JsError
import play.api.libs.ws.{EmptyBody, WSClient}
import play.api.mvc.{AnyContent, QueryStringBindable, Results, Session}

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.03.19 18:42
  * Description: В гос.услугах для логина используется велосипед, именуемый "OpenID Connect 1.0",
  * не особо дружащий с одноимённым протоколом.
  */
final class EsiaLoginUtil @Inject()(
                                     mKeyStore                : MKeyStore,
                                     configuration            : Configuration,
                                     contextUtil              : ContextUtil,
                                     wsClient                 : WSClient,
                                     implicit private val ec  : ExecutionContext,
                                   )
  extends IExtLoginAdp
  with MacroLogsImpl
{

  def KEYSTORE_ALIASES_PREFIX = "esia."
  def KEYSTORE_ALIAS =  KEYSTORE_ALIASES_PREFIX + "mykey"

  /** Алиас сертификата самой ЕСИА в keystore. */
  def esiaCertKeyStoreAlias(mode: MTestProdMode): String =
    KEYSTORE_ALIASES_PREFIX + mode.value


  protected case class EsiaConf(
                                 clientId   : String,
                                 mode       : MTestProdMode
                               )

  /** Сборка конфига ЕСИА. */
  lazy val CONFIG = {
    val ROOT_KEY = "esia"
    for {
      esiaConf <- {
        val r = configuration.getOptional[Configuration]( ROOT_KEY )
        if (r.isEmpty) LOGGER.info(s"$ROOT_KEY configuration missing.")
        r
      }

      clientId <- {
        val k = "client.id"
        val r = esiaConf.getOptional[String]( k )
        if (r.isEmpty) LOGGER.warn(s"ESIA $ROOT_KEY.$k missing in config - no client ID.")
        r
      }

      modeConfigKey = "mode"
      modeStr  <- {
        val r = esiaConf.getOptional[String]( modeConfigKey )
        if (r.isEmpty) LOGGER.warn(s"ESIA $ROOT_KEY.$modeConfigKey missing in config - no mode enabled.")
        r
      }

      mode     <- {
        val r = MTestProdModes.withValueOpt( modeStr )
        if (r.isEmpty) LOGGER.error(s"$ROOT_KEY.$modeConfigKey value invalid. Possible values are: [${MTestProdModes.values.iterator.map(_.value).mkString(", ")}]")
        r
      }

    } yield {
      EsiaConf(
        clientId  = clientId,
        mode      = mode,
      )
    }
  }


  def esiaUrlPrefixForMode(mode: MTestProdMode): String = {
    val root = mode match {
      case MTestProdModes.Testing     => "-portal1.test"
      case MTestProdModes.Production  => ""
    }
    "https://esia" + root + ".gosuslugi.ru/aas/oauth2/"
  }


  object Scopes {
    def OPENID = "openid"
    def EMAIL  = "email"
  }

  object EsiaOAuth2Resources {
    def LOGIN_FORM    = "ac"
    def ACCESS_TOKEN  = "te"
  }


  /** Запуск шага аутентификации.
    *
    * @param service Сервис.
    * @param req     HTTP-реквест.
    * @return Фьючерс с результатом.
    *         Если совсем неправильный вызов, то будет ошибка внутри Future.
    */
  override def authenticateFromRequest()(implicit req: MLoginViaReq[AnyContent]): Future[AuthenticationResult] = {
    // Если пришёл пустой GET, то надо отредиректить юзера в ЕСИА, собрав подписанную ссылку
    if (req.rawQueryString.isEmpty) {
      // Сюда перебросило юзера, чтобы отредиректить его в ЕСИА
      val res = rdrUserToEsia()
      Future.successful( res )

    } else {
      authFromReqWithData()
    }
  }

  def redirectUrl: String =
    contextUtil.LK_URL_PREFIX + routes.Ident.idViaProvider(MExtServices.GosUslugi).url

  /** Высылка юзера в направлении ЕСИА. */
  def rdrUserToEsia()(implicit req: MLoginViaReq[AnyContent]): AuthenticationResult.NavigationFlow = {
    val conf = CONFIG.get
    val qsArgs4sign = MEsiaSignContent(
      clientId  = conf.clientId,
      timestamp = _now,
      state     = UUID.randomUUID(),
      scope     = Scopes.OPENID,
    )

    val qsArgs = MEsiaStep1Qs(
      signContent  = qsArgs4sign,
      clientSecret = _toBase64UrlSafe( mkSign(qsArgs4sign).getEncoded ),
      responseType = MEsiaRespTypes.Code,
      redirectUri  = redirectUrl,
      accessType   = MEsiaAccessTypes.Online
    )

    val esiaQueryString = implicitly[QueryStringBindable[MEsiaStep1Qs]]
      .unbind("", qsArgs)

    // Сбор итоговой ссылки:
    val rdrUrl = esiaUrlPrefixForMode( conf.mode ) +
      EsiaOAuth2Resources.LOGIN_FORM +
      "?" + esiaQueryString

    val resp = Results.TemporaryRedirect( rdrUrl )
      // Добавить в сессию инфу об исходной ссылке для верификации на следующем шаге:
      .addingToSession( MSessionKeys.ExtLoginData.value -> qsArgs4sign.state.toString )

    AuthenticationResult.NavigationFlow( resp )
  }


  /** Возврат юзера из ЕСИА.
    * Надо узнать, чего юзер нам принёс. */
  def authFromReqWithData()(implicit req: MLoginViaReq[AnyContent]): Future[AuthenticationResult] = {
    lazy val logPrefix = s"authFromReqWithData()#${req.remoteClientAddress}#${System.currentTimeMillis()}:"

    // Внучную попытаться забиндить query-string
    val bindedOpt = for {
      // Убедится, что тут у нас юзер ожидаемый, а не подставной, и он осознанно проходит процедуру логина:
      stateStr0 <- req.session.get( MSessionKeys.ExtLoginData.value )

      // Забиндить qs:
      bindedE   <- implicitly[QueryStringBindable[MEsiaAuthReturnQs]].bind("", req.target.queryMap)
      binded    <- bindedE.toOption

      // Методичка требует сверять исходный state и полученный:
      state0    = UUID.fromString( stateStr0 )
      stateRet  <- binded.state
      if state0 ==* stateRet

    } yield {
      // Что-то забиндилось, надо разобраться что именно:
      binded
        .error
        .fold[Future[AuthenticationResult]] {
          // Нет ошибок - разобраться с авторизационным кодом.
          val authCode = binded.code.get
          handleReturnedAuthCode( authCode )

        } { errorCode =>
          LOGGER.debug(s"$logPrefix User#${req.user.personIdOpt.orNull} failed ESIA-login: $errorCode")
          val res = AuthenticationResult.Failed( errorCode )
          Future.successful( res )
        }
    }

    bindedOpt.getOrElse {
      LOGGER.warn(s"$logPrefix Undefined behaviour: nothing binded from qs.")
      Future.successful( AuthenticationResult.AccessDenied() )
    }
  }


  /** Юзер вернулся с кодом на руках.
    *
    * @param code Авторизационнй код, присланный ЕСИА.
    * @return Фьючерс с результатом проверки авторизационного кода.
    */
  def handleReturnedAuthCode(authCode: String)(implicit req: MLoginViaReq[AnyContent]): Future[AuthenticationResult] = {
    // TODO Для проверки кода авторизации, надо напрямую запросить access token у ESIA с помощью полученного кода.
    val conf = CONFIG.get

    val qsArgs4sign = MEsiaSignContent(
      clientId  = conf.clientId,
      timestamp = _now,
      state     = UUID.randomUUID(),
      scope     = Scopes.OPENID,
    )

    val qsArgs = MEsiaAcTokQs(
      signContent   = qsArgs4sign,
      clientSecret  = _toBase64UrlSafe( mkSign(qsArgs4sign).getEncoded ),
      redirectUri   = redirectUrl,
      authCode      = authCode,
      grantType     = MEsiaGrantTypes.AuthorizationCode,
      tokenType     = MEsiaTokenTypes.Bearer,
    )

    val acTokUrl = esiaUrlPrefixForMode( conf.mode ) +
      EsiaOAuth2Resources.ACCESS_TOKEN +
      "?" + implicitly[QueryStringBindable[MEsiaAcTokQs]].unbind("", qsArgs)

    for {
      resp <- wsClient
        .url( acTokUrl )
        .post( EmptyBody )
    } yield {
      lazy val logPrefix = s"handleReturnedAuthCode(#${authCode.hashCode}):"

      if (resp.status ==* HttpConst.Status.OK) {
        resp.json
          .validate[MEsiaAcTokResp]
          .filter( JsError("state req-resp mismatch") ) { resp =>
            // Малополезная проверка, но всё же...
            val r = resp.state.fold(true)(_ ==* qsArgs.signContent.state)
            if (!r)
              LOGGER.error(s"$logPrefix state value in request and response does not match:\n ${qsArgs.signContent.state} != ${resp.state}")
            r
          }
          .fold[AuthenticationResult](
            {jsErrors =>
              LOGGER.error(s"$logPrefix Failed to bind 200-OK ESIA resp:\n ${jsErrors.mkString("\n ")}")
              AuthenticationResult.Failed( "JSON parse error" )
            },
            {resp =>
              // Всё ок, токен на руках. Но надо извлечь subject id.
              val tokenStr = resp.idToken
                .orElse(resp.accessToken)
                .get
              getClaimsFromToken( tokenStr, qsArgs ).fold(
                {tokenEx =>
                  LOGGER.error(s"$logPrefix Failed to decode ESIA JWT-token", tokenEx)
                  AuthenticationResult.Failed( tokenEx.getMessage )
                },
                {claims =>
                  AuthenticationResult.Authenticated(
                    UserProfile(
                      providerId  = MExtServices.GosUslugi.value,
                      userId      = claims.getSubject,
                      authMethod  = AuthenticationMethod.OAuth2,
                      oAuth2Info  = for (acTok <- resp.accessToken) yield {
                        OAuth2Info(
                          accessToken   = acTok,
                          tokenType     = resp.tokenType.map(_.value),
                          expiresIn     = resp.expiresInSec,
                          refreshToken  = resp.refreshToken
                        )
                      }
                    )
                  )
                }
              )
            }
          )

      } else {
        LOGGER.warn(s"$logPrefix Failed to POST access_token: HTTP ${resp.status} ${resp.statusText}:\n ${resp.body}")
        AuthenticationResult.Failed( resp.body )
      }
    }
  }


  override def MAX_SESSION_TTL_SECONDS = 1.day.toSeconds

  override def clearSession(s: Session): Session = {
    val k = MSessionKeys.ExtLoginData.value
    if (s.data contains k) {
      s - k
    } else {
      s
    }
  }


  /** Генерация подписи.
    *
    * @param data Неподписанные данные.
    * @return Подписанные данные.
    *         .getEncoded() - получение всего результата detached-подписи.
    *         .getSignedContent().getContent() - извлечение контента (сертификата) под подписью
    */
  def mkSign(qs: MEsiaSignContent): CMSSignedData = {
    // Написано по мотивам https://stackoverflow.com/questions/16662408/correct-way-to-sign-and-verify-signature-using-bouncycastle
    // TODO Взять из конфига сертификат с ключом, сгенерить подпись для данных.
    // Сертификат уже сгенерен по рекомендациям, но не прописан в конфигах, не сохранен ни в каком keystore.
    val signedDataStr = s"${qs.scope}${MEsiaQs.TIMESTAMP_FORMAT.format( qs.timestamp )}${qs.clientId}${qs.state.toString}"

    // Сборка подписи для данных:
    // TODO Перейти на ГОСТ 34.10-2012 (или -2001)
    val sigAlgo = "SHA256WithRSA"
    val myPrivKey = mKeyStore.getKey[PrivateKey]( KEYSTORE_ALIAS ).get

    // Кэшируем крипто-провайдера для небольшого ускорения работы.
    val bcProv = Security.getProvider( BouncyCastleProvider.PROVIDER_NAME )

    val signature = Signature.getInstance( sigAlgo, bcProv )
    signature.initSign( myPrivKey )
    signature.update( signedDataStr.getBytes(StandardCharsets.UTF_8) )

    // PKCS#7 CMS-подпись требует сертификата внутри себя
    val msg = new CMSProcessableByteArray(signature.sign());
    val cert = mKeyStore.getCert[X509Certificate]( KEYSTORE_ALIAS ).get
    val certs = new JcaCertStore( (cert :: Nil).asJavaCollection )

    val cmsSigner = new JcaContentSignerBuilder( sigAlgo )  // TODO В оригинале здесь было "with" с маленькой буквы: SHA256withRSA
      .setProvider( bcProv )
      .build( myPrivKey )
    val digestCalc = new JcaDigestCalculatorProviderBuilder()
      .setProvider( bcProv )
      .build()
    val sigInfoGenerator = new JcaSignerInfoGeneratorBuilder(digestCalc)
      .build(cmsSigner, cert)

    val cmsSigGen = new CMSSignedDataGenerator
    cmsSigGen.addSignerInfoGenerator( sigInfoGenerator )
    cmsSigGen.addCertificates(certs)

    // Без инкапсуляции данных, требуется detached-signature.
    cmsSigGen.generate( msg, false )
  }


  private def _toBase64UrlSafe( bytes: Array[Byte] ): String = {
    Base64.getUrlEncoder
      .withoutPadding()
      .encodeToString( bytes )
  }

  private def _now = OffsetDateTime.now( ZoneId.of("Europe/Moscow") )


  /** Экстракция id юзера из access_token'а, присланного ЕСИА.
    *
    * @param tokQsArgs Аргументы qs при запросе токена.
    * @see [[https://ru.stackoverflow.com/a/785425]] Пример кода (java).
    */
  def getClaimsFromToken(token: String, tokQsArgs: MEsiaAcTokQs): Try[Claims] = {
    val conf = CONFIG.get
    lazy val logPrefix = s"getClaimsFromToken()#${System.currentTimeMillis()}:"

    val certKeyAlias = esiaCertKeyStoreAlias( conf.mode )
    val esiaPubKeyOpt = mKeyStore.getKey[PublicKey]( certKeyAlias )
    if (esiaPubKeyOpt.isEmpty)
      throw new IllegalStateException(s"$logPrefix ESIA public key'$certKeyAlias' missing for mode=${conf.mode}. Do import key into keystore.")

    val esiaPubKey = mKeyStore.getKey[PublicKey]( esiaCertKeyStoreAlias(conf.mode) ).get

    Try {
      val jwt = Jwts
        .parser()
        .setSigningKey( esiaPubKey )
        // Выставляем время запроса:
        .setClock(
          new FixedClock(
            ju.Date.from(
              tokQsArgs.signContent.timestamp.toInstant
            )
          )
        )
        .setAllowedClockSkewSeconds( 60 )
        .parseClaimsJws( token )

      val claims = jwt.getBody

      // Токены subject id бывают такие:
      // - "sub" - идентификатор субъекта (вроде бы - число, но следует учитывать и строку).
      // - "urn:esia:sbj"."urn:esia:sbj:oid" (число) - для id-token
      // - "urn:esia:sbj_id" (число) - для биометрического id-токена и для access-токена
      // Можно использовать urn:esia:sbj:nam (строка вида OID.34234324) для идентификатора юзера.
      //
      // - urn:esia:sbj -> urn:esia:sbj:is_tru - is trusted - учетная запись пользователя подтверждена.
      //                                         Параметр отсутствует, если учетная запись не подтверждена.
      // TODO Надо решить, ограничивать ли пользователей по is_tru? И если да, то на каком этапе? В начале пока можно забить.

      // Приложение В.6.4 требует проверять маркер идентификации:
      // 1. Проверка идентификатора (мнемоники) ЕСИА, содержащейся в маркере идентификации.
      val issuer = claims.getIssuer
      ErrorConstants.assertArg(
        // TODO Учитывать test/prod
        issuer matches "^https?://esia.gosuslugi.ru/$",
        s"$logPrefix Token issuer mismatch: unexpected '$issuer'"
      )

      // 2. Проверка идентификатора (мнемоники) системы-клиента, т.е. именно система-клиент должна быть указана в качестве адресата маркера идентификации.
      val claimedAudience = claims.getAudience
      ErrorConstants.assertArg(
        claimedAudience ==* conf.clientId,
        s"$logPrefix Audience clientId mismatch:\n claimed = $claimedAudience\n expected = ${conf.clientId}"
      )

      // 3. Проверка подписи маркера идентификации (с использованием указанного в маркере алгоритма).

      // 4. Текущее время должно быть не позднее, чем время прекращения срока действия маркера идентификации.
      val notBefore = claims.getNotBefore
      val notBeforeInstant = notBefore.toInstant
      val nowInstant = Instant.now()
      ErrorConstants.assertArg(
        !(nowInstant isAfter notBeforeInstant),
        s"$logPrefix notBefore claim is outdated:\n NotBefore = $notBefore ($notBeforeInstant)\n now = $nowInstant"
      )

      claims
    }
  }

}
