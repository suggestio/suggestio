package controllers

import io.suggest.captcha.{CaptchaConstants, MCaptchaCookiePath, MCaptchaCookiePaths}
import io.suggest.playx.CacheApiUtil
import io.suggest.sec.util.Csrf
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import play.api.inject.Injector
import util.acl.{BruteForceProtect, IsIdTokenValid, MaybeAuth}
import util.captcha.CaptchaUtil
import util.ident.IdTokenUtil

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.2020 17:24
  * Description: Контроллер для капчи.
  */
class Captcha @Inject()(
                         injector       : Injector,
                         csrf           : Csrf,
                         sioCtlApi      : SioControllerApi,
                       )
  extends MacroLogsImpl
{

  import sioCtlApi._


  private lazy val maybeAuth = injector.instanceOf[MaybeAuth]
  private lazy val bruteForceProtect = injector.instanceOf[BruteForceProtect]
  private lazy val cacheApiUtil = injector.instanceOf[CacheApiUtil]
  private lazy val captchaUtil = injector.instanceOf[CaptchaUtil]
  private lazy val idTokenUtil = injector.instanceOf[IdTokenUtil]
  private lazy val isIdTokenValid = injector.instanceOf[IsIdTokenValid]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  /**
    * Вернуть картинку капчи.
    * Такую картинку будет проще ввести на мобильном устройстве.
    * Для защиты от hot-link'а и создания паразитной нагрузки/трафика - используется CSRF и кэш.
    *
    * @param captchaId id капчи.
    * @return image/png
    */
  def getCaptchaImg(captchaId: String, mCookiePath: MCaptchaCookiePath) = csrf.Check {
    maybeAuth().async { implicit request =>
      lazy val logPrefix = s"getCaptchaImg($captchaId)#${System.currentTimeMillis()}:"
      LOGGER.trace(s"$logPrefix for ${request.remoteClientAddress}/u#${request.user.personIdOpt.orNull}")
      for {

        // Рендер одной картинки создаёт некоторую нагрузку, поэтому для защиты от DoS-атаки через перегрузку сервера, используем кэш рендера:
        (ctext, imgBytes) <- cacheApiUtil.getOrElseFut("captcha.img", expiration = 3.seconds) {
          // Пусть будут только цифры. На телефоне удобнее цифры набирать, а остальным -- равные права.
          val ctext1 = captchaUtil.createCaptchaDigits()
          LOGGER.trace(s"$logPrefix ctext=$ctext1")
          val imgBytes1 = captchaUtil.createCaptchaImg( ctext1 )
          val res = (ctext1, imgBytes1)
          Future.successful( res )
        }

      } yield {
        // Путь до кукиса:
        val cookiePath = mCookiePath match {
          case MCaptchaCookiePaths.EpwReg =>
            routes.Ident.epw2RegSubmit().url
        }
        Ok( imgBytes )
          .as( captchaUtil.CAPTCHA_IMG_MIME )
          .withHeaders(
            EXPIRES       -> "0",
            PRAGMA        -> "no-cache",
            CACHE_CONTROL -> "no-store, no-cache, must-revalidate"
          )
          .withCookies(
            captchaUtil.mkCookie(captchaId, ctext, cookiePath)
          )
      }
    }
  }


  /** Старый getCaptchaImg уязвим для повторного использования cookie из-за stateless-сессий,
    * а зашифрованный ответ на капчи - недостаточно хорошо защищён в долгосрочной перспективе.
    * + Имеет велосипед с cookie и cookiePath.
    *
    * Поэтому решено, что надо сделать по уму:
    * - PGP-сообщение вместо шифрования
    * - Кастомный хидер вместо cookie, для передачи pgp-шифротекста. Явный сабмит шифра капчи на сервер.
    * - Для одноразовости капч используется sql-табличка one_time_tokens, куда попадают уже использованные капчи.
    * - Для id капч используется UUID, который и заносится в sql-таблице.
    *
    * @return Картинка + http-хидеры с id и ответом на капчу.
    */
  def getCaptcha(idTokenCiphered: String) = csrf.Check {
    bruteForceProtect {
      isIdTokenValid(idTokenCiphered)(isIdTokenValid.isFreshToken).async { implicit request =>
        val logPrefix = s"getCaptchaImg2()#${System.currentTimeMillis()}:"
        LOGGER.trace(s"$logPrefix for ${request.remoteClientAddress}, personId#${request.user.personIdOpt.orNull}")

        // Кэш для защиты от перегрузки слишкой активной генерацией капч: ограничить макс.кол-во параллельных капч.
        val captchaFut = captchaUtil.mkCaptcha( request.idToken.ottId )

        // Шифруем правильный ответ на капчу:
        val captchaSecretPgpMinFut = captchaFut.flatMap { case (ctext, _) =>
          val idToken2 = captchaUtil.storeCaptchaSecret( captchaText = ctext, request.idToken )
          idTokenUtil.encrypt( idToken2 )
        }

        // Отправить http-ответ юзеру, запихнув pgp-шифр в заголовок ответа:
        for {
          (_, captchaImgBytes)     <- captchaFut
          captchaSecretPgpMin      <- captchaSecretPgpMinFut
        } yield {
          Ok( captchaImgBytes )
            .as( captchaUtil.CAPTCHA_IMG_MIME )
            .withHeaders(
              EXPIRES       -> "0",
              PRAGMA        -> "no-cache",
              CACHE_CONTROL -> "no-store, no-cache, must-revalidate",
              CaptchaConstants.CAPTCHA_SECRET_HTTP_HDR_NAME -> captchaSecretPgpMin,
            )
        }
      }
    }
  }

}
