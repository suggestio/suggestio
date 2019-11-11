package util.captcha

import java.io.ByteArrayOutputStream
import java.time.{Instant, ZoneId, ZoneOffset}
import java.util.{Properties, UUID}

import akka.util.ByteString
import com.google.code.kaptcha.Producer
import com.google.code.kaptcha.impl.DefaultKaptcha
import com.google.code.kaptcha.util.Config
import io.suggest.captcha.MCaptchaCheckReq
import io.suggest.id.token.{MIdMsg, MIdToken}
import io.suggest.mbill2.m.ott.{MOneTimeToken, MOneTimeTokens}
import io.suggest.mbill2.util.effect
import io.suggest.model.n2.edge.MPredicates
import io.suggest.playx.CacheApiUtil
import javax.inject.{Inject, Singleton}
import io.suggest.sec.util.{CipherUtil, PgpUtil}
import io.suggest.text.util.TextUtil
import io.suggest.util.logs.MacroLogsImpl
import javax.imageio.ImageIO
import models.mproj.ICommonDi
import play.api.data.Form
import play.api.http.HeaderNames
import play.api.mvc._
import play.api.data.Forms._
import play.api.libs.json.Json
import util.FormUtil._
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import models.req.IReqHdr
import util.ident.IdTokenUtil

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.06.14 18:18
 * Description: Утиль для работы с капчами. Разгаданные значения капч лежат зашифрованными в куке,
 * которая имеет короткий цикл жизни, и имя её -- некий рандомный id капчи, который передаётся вместе с сабмитом формы,
 * к которой относится эта капча. Т.е. рандомный id генерится с формой, но кука с капчей выставляется только при
 * получении картинки капчи.
 * При сабмите значение капчи расшифровывается и сравнивается с выхлопом юзера через простое API.
 */


/** Утиль для криптографии, используемой при stateless-капчевании. */
object CaptchaUtil {
  def CAPTCHA_ID_FN     = "captchaId"
  def CAPTCHA_TYPED_FN  = "captchaTyped"
}


/** Инжектируемая часть капча-утили. */
@Singleton
final class CaptchaUtil @Inject() (
                                    mOneTimeTokens              : MOneTimeTokens,
                                    cacheApiUtil                : CacheApiUtil,
                                    pgpUtil                     : PgpUtil,
                                    idTokenUtil                 : IdTokenUtil,
                                    protected val cipherUtil    : CipherUtil,
                                    val mCommonDi               : ICommonDi,
                                  )
  extends MacroLogsImpl
{

  import mCommonDi.{ec, slick}

  def CAPTCHA_IMG_FMT_LC = "png"
  def CAPTCHA_IMG_MIME = "image/" + CAPTCHA_IMG_FMT_LC

  /** Кол-во цифр в цифровой капче (длина строки капчи). */
  def DIGITS_CAPTCHA_LEN = 5

  def COOKIE_MAXAGE_SECONDS = 1800
  lazy val COOKIE_FLAG_SECURE = mCommonDi.current.injector.instanceOf[SessionCookieBaker].secure

  /** Маппер формы для hidden поля, содержащего id капчи. */
  def captchaIdM = nonEmptyText(maxLength = 16)
    .transform(strTrimSanitizeF, strIdentityF)

  /** Маппер формы для поля, в которое юзер вписывает текст с картинки. */
  def captchaTypedM = nonEmptyText(maxLength = 16)
    .transform(strTrimF, strIdentityF)

  /** Сброка инстанса шифровальной/дешифровальной машины. */
  def cipherer = cipherUtil.Cipherer(
    // TODO Sec Запилить получение IV и ключа по рандомным строкам из конфига.
    IV_MATERIAL_DFLT = Array[Byte](-112, 114, -62, 99, -19, -86, 118, -42, 77, -103, 33, -30, -91, 104, 18, -105,
      101, -39, 4, -41, 24, -79, 58, 58, -7, -119, -68, -42, -102, 53, -104, -33),
    SECRET_KEY = Array[Byte](-22, 52, -78, -47, -46, 44, -3, 116, -8, -2, -96, -98, 48, 102, -117, -43,
      -59, -23, 75, 59, -101, 21, -26, 51, -102, -76, 22, 43, -94, -43, 111, 51)
  )


  def cookieName(captchaId: String) = "cha." + captchaId

  def ivMaterial(captchaId: String)(implicit request: RequestHeader): Array[Byte] = {
    request.headers
      .get(HeaderNames.USER_AGENT)
      .fold(captchaId) { _ + captchaId }
      .getBytes
  }


  /**
   * Удалить капчу из кукисов.
   * @param form Форма.
   * @param response Http-ответ.
   * @return Модифицированный http-ответ.
   */
  def rmCaptcha(form: Form[_])(response: Result): Result = {
    form.data.get( CaptchaUtil.CAPTCHA_ID_FN).fold(response) { captchaId =>
      response
        .discardingCookies(DiscardingCookie(
          name   = cookieName(captchaId),
          secure = COOKIE_FLAG_SECURE
        ))
    }
  }

  def checkCaptcha2[T](t: T)(implicit capGet: ICaptchaGetter[T], request: RequestHeader): Boolean = {
    val okFormOpt = for {
      captchaId       <- capGet.getCaptchaId(t)
      captchaTyped    <- capGet.getCaptchaTyped(t)
      _cookieName     = cookieName(captchaId)
      cookieRaw       <- request.cookies.get(_cookieName)
      if {
        lazy val logPrefix = s"checkCaptcha2(${CaptchaUtil.CAPTCHA_ID_FN}, ${CaptchaUtil.CAPTCHA_TYPED_FN}):"
        try {
          val ctext = cipherer.decryptPrintable(
            cookieRaw.value,
            ivMaterial = ivMaterial(captchaId)
          )
          // Бывает юзер вводит английские буквы с помощью кириллицы. Исправляем это:
          // TODO Надо исправлять только буквы
          val captchaTyped2 = captchaTyped.trim
            .map { TextUtil.mischarFixEnAlpha }
          // TODO Допускать неточное совпадение капчи?
          val result = ctext equalsIgnoreCase captchaTyped2
          if (!result)
            LOGGER.trace(s"$logPrefix Invalid captcha typed. expected = $ctext, typed = $captchaTyped2")
          result
        } catch {
          case ex: Exception =>
            LOGGER.warn(s"$logPrefix Failed", ex)
            false
        }
      }
    } yield {
      // Есть что-то в результате. Значит капчка пропарсилась.
      true
    }
    // Отработать отсутствие положительного результата парсинга капчи.
    okFormOpt.getOrElse {
      // Нет результата. Залить в форму ошибки.
      //form.withError( CaptchaUtil.CAPTCHA_TYPED_FN, "error.captcha")
      false
    }
  }

  /** Проверить капчу, присланную в форме. Вызывается перез Form.fold().
    * @param form Маппинг формы.
    * @return Форма, в которую может быть залита ошибка поля ввода капчи.
    */
  def checkCaptcha[T](form: Form[T])(implicit request: RequestHeader): Form[T] = {
    // TODO Это старое API. Удалить вместе с древним Ident'ом.
    if (checkCaptcha2( form )) form
    else form.withError( CaptchaUtil.CAPTCHA_TYPED_FN, "error.captcha")
  }


  /** Генерация текста цифровой капчи (n цифр от 0 до 9). */
  def createCaptchaDigits(l: Int = DIGITS_CAPTCHA_LEN): String = {
    val rnd = new Random()
    val sb = new StringBuilder(l)
    for(_ <- 1 to l)
      sb append rnd.nextInt(10)
    sb.toString()
  }


  def mkCookie(captchaId: String, rawCaptchaText: String, cookiePath: String)(implicit rh: RequestHeader): Cookie = {
    Cookie(
      name      = cookieName(captchaId),
      value     = cipherer.encryptPrintable(
        rawCaptchaText,
        ivMaterial = ivMaterial(captchaId)
      ),
      maxAge    = Some( COOKIE_MAXAGE_SECONDS ),
      httpOnly  = true,
      secure    = COOKIE_FLAG_SECURE,
      path      = cookiePath,
    )
  }


  def getCaptchaProducer(): Producer = {
    val prod = new DefaultKaptcha
    prod.setConfig(new Config(new Properties()))
    prod
  }

  //def createCaptchaText: String =
  //  getCaptchaProducer().createText()

  def createCaptchaImg(ctext: String): ByteString = {
    val img = getCaptchaProducer()
      .createImage(ctext)
    val baos = new ByteArrayOutputStream(8192)
    if ( !ImageIO.write(img, CAPTCHA_IMG_FMT_LC, baos) )
      throw new IllegalStateException("ImageIO writer returned false. No writer for captcha image or format " + CAPTCHA_IMG_FMT_LC)
    ByteString.fromArrayUnsafe( baos.toByteArray )
  }


  import mCommonDi.slick.profile.api._

  /** Выставить id капчи в базе, как уже использованную капчу.
    *
    * @param idToken id-токен капчи.
    * @return Результат обработки.
    */
  def ensureCaptchaOtt(idToken: MIdToken, idMsg: MIdMsg, now: Instant = Instant.now()): DBIOAction[(MIdToken, MOneTimeToken), NoStream, effect.RWT] = {
    lazy val logPrefix = s"ensureCaptchaOtt(${idToken.ottId}):"
    val dbAction = for {
      // Поискать в базе токен с текущем id капчи.
      ottExist <- mOneTimeTokens
        .query
        .filter { ott =>
          ott.id === idToken.ottId
        }
        .take(1)
        .forUpdate
        .result
        .headOption

      // Если токен уже присутствует в ott, то значит повторное использование уже использованной капчи.
      if {
        for (ott0 <- ottExist)
          LOGGER.warn(s"$logPrefix Token already exists in db since ${ott0.dateCreated.atOffset(ZoneOffset.UTC)}")
        ottExist.isEmpty
      }

      // Обновлённый idMsg:
      idMsg2 = MIdMsg.validated.set( Some(now) )(idMsg)

      // Обновлённый токен:
      idToken2 = (
        MIdToken.idMsgs.modify { idMsgs0 =>
          idMsg2 :: idMsgs0.filterNot(_.rcptType ==* MPredicates.JdContent.Image)
        } andThen
        MIdToken.dates.modify { dates0 =>
          dates0.copy(
            ttlSeconds = 5.minutes.toSeconds.toInt,
            modified   = Some( now ),
          )
        }
      )(idToken)

      // Инзертить новый токен в бд
      ott <- {
        val ott = MOneTimeToken(
          id          = idToken2.ottId,
          dateCreated = idToken2.dates.modifiedOrCreated,
          dateEnd     = idToken2.dates.bestBefore,
          info        = Some( Json.toJson(idToken2).toString() ),
        )
        LOGGER.trace(s"$logPrefix Saving OTT: $ott\n dateEnd = ${ott.dateEnd.atZone(ZoneId.systemDefault())}")
        mOneTimeTokens.insertOne( ott )
      }

    } yield {
      LOGGER.trace(s"$logPrefix Saved token ok.")
      idToken2 -> ott
    }
    dbAction.transactionally
  }


  def captchaTtl(dateCreated: Instant): Instant =
    dateCreated plusSeconds COOKIE_MAXAGE_SECONDS


  /** Генерация id капчи. */
  def mkCaptchaUid() = UUID.randomUUID()

  /** Генерация новой капчи для uid.
    *
    * @param captchaUid id капчи.
    * @return Ответ на капчу и png-картинка капчи.
    */
  def mkCaptcha(captchaUid: UUID): Future[(String, ByteString)] = {
    cacheApiUtil.getOrElseFut(s"${captchaUid.getLeastSignificantBits % 8}.captcha.img", expiration = 2.seconds) {
      val ctext1 = createCaptchaDigits()
      val imgBytes1 = createCaptchaImg( ctext1 )
      val res = (ctext1, imgBytes1)
      Future.successful( res )
    }
  }


  /** Собрать секрет капчи.
    *
    * @param captchaText Текст капчи.
    * @return Фьючерс с секретом капчи.
    */
  def storeCaptchaSecret(captchaText: String, idToken: MIdToken): MIdToken = {
    val captchaSecret = MIdMsg(
      rcptType  = MPredicates.JdContent.Image,
      checkCode = captchaText,
    )
    (
      MIdToken.idMsgs.modify(captchaSecret :: _) andThen
      MIdToken.dates.modify { dates0 =>
        dates0.copy(
          ttlSeconds = 10.minutes.toSeconds.toInt,
          modified   = Some( Instant.now() )
        )
      }
    )(idToken)
  }


  def _captchaInvalidMsg = "captcha.invalid"

  /** Валидация капчи на основе секрета
    * - проверить соответствие присланной капчи с pgp-шифротекстом.
    * - проверить токен по базе, и сохранить в БД использованный токен.
    *
    * @param captcha Данные капчи, присланные с клиента.
    * @return Фьючерс с результатом валидации:
    *         Left() - Код ошибки.
    *         Right() - Выверенный секрет.
    */
  def validateAndMarkAsUsed(captcha: MCaptchaCheckReq)(implicit request: IReqHdr): Future[Either[String, (MIdToken, MOneTimeToken)]] = {
    val now = Instant.now()
    lazy val logPrefix = s"epw2RegSubmit()#${now.toEpochMilli}:"

    if (captcha.typed.length !=* DIGITS_CAPTCHA_LEN) {
      LOGGER.warn(s"$logPrefix Captcha len invalid: ${captcha.typed} expected=$DIGITS_CAPTCHA_LEN irl=${captcha.typed.length}")
      Future.successful( Left(_captchaInvalidMsg) )

    } else {
      for {

        // Быстрые синхронные проверки секрета капчи:
        captchaDecodeRes <- {
          idTokenUtil
            .decrypt( captcha.secret )
            .transform { tryRes =>
              Success( tryRes.toEither )
            }
        }

        idTokenE = captchaDecodeRes
          .left.map { ex =>
            // Не удалось расшифровать pgp-сообщение.
            LOGGER.warn(s"$logPrefix Unable to parse captcha idToken", ex)
            "Invalid request body"
          }
          .filterOrElse(
            {idToken =>
              val r = idTokenUtil.isConstaintsMeetRequest( idToken, now )
              if (!r) LOGGER.warn(s"$logPrefix Expired or unrelated idToken#${idToken.ottId}")
              r
            },
            _captchaInvalidMsg
          )
          .map { idToken =>
            val captchaIdMsg = idToken.idMsgs
              .find(_.rcptType ==* MPredicates.JdContent.Image)
              .get
            (idToken, captchaIdMsg)
          }
          // Проверить соответствие введённой капчи и реальной.
          .filterOrElse(
            { case (_, captchaIdMsg) =>
              // Оставить только символы
              val captchaTypedNorm = captcha.typed
                .replaceAll("\\s+", "")

              // По идее, в капче только цифры, но вдруг это изменится - сравниваем через equalsIgnoreCase
              val r = captchaIdMsg.checkCode equalsIgnoreCase captchaTypedNorm

              if (r) LOGGER.trace(s"Captcha matched ok = $captchaTypedNorm")
              else   LOGGER.warn(s"Captcha mismatch, typed=${captchaTypedNorm} expected=${captchaIdMsg.checkCode}")

              r
            },
            _captchaInvalidMsg
          )

        // Капча проверена, но возможно, что она уже бывшая в употреблении. Надо провалидировать id капчи по базе токенов.
        isOk <- idTokenE.fold(
          {emsg =>
            Future.successful( Left(emsg) )
          },
          {case (idToken, idMsg) =>
            slick.db.run {
              ensureCaptchaOtt( idToken, idMsg, now )
            }
              .transform {
                case Failure(ex) =>
                  LOGGER.warn(s"$logPrefix Captcha already used")
                  LOGGER.trace("captcha verify error:", ex)
                  Success( Left( "captcha.expired" ) )
                case Success(r2 @ (_, mott)) =>
                  LOGGER.trace(s"$logPrefix Captcha marked as USED. mott#${mott.id}")
                  Success( Right(r2) )
              }
          }
        )

      } yield {
        LOGGER.trace(s"$logPrefix Captcha checks done, result =\n $isOk")
        isOk
      }
    }
  }


  // "Разогреть" подсистему генерации капчи, чтобы не было пауз при первом реальном вызове.
  Future {
    createCaptchaImg( createCaptchaDigits() )
  }

}


/** Интерфейс для доступа к инжектируемому через DI инстансу [[CaptchaUtil]]. */
trait ICaptchaUtilDi {
  def captchaUtil: CaptchaUtil
}


sealed trait ICaptchaGetter[T] {
  def getCaptchaId(t: T): Option[String]
  def getCaptchaTyped(t: T): Option[String]
}
object ICaptchaGetter {

  /** Доступ к form-запросу. */
  implicit def FormCaptchaGetter[T]: ICaptchaGetter[Form[T]] = {
    new ICaptchaGetter[Form[T]] {
      override def getCaptchaId(t: Form[T]): Option[String] =
        t.data.get( CaptchaUtil.CAPTCHA_ID_FN )
      override def getCaptchaTyped(t: Form[T]): Option[String] =
        t.data.get( CaptchaUtil.CAPTCHA_TYPED_FN )
    }
  }

  /** Доступ к kv-описанию. */
  implicit object PlainKvGetter extends ICaptchaGetter[(String, String)] {
    override def getCaptchaId(t: (String, String)): Option[String] =
      Some( t._1 )
    override def getCaptchaTyped(t: (String, String)): Option[String] =
      Some( t._2 )
  }

}
