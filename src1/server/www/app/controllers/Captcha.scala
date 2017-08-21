package controllers

import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.imageio.ImageIO
import javax.inject.Singleton

import com.google.code.kaptcha.util.Config
import javax.inject.Inject

import com.google.code.kaptcha.Producer
import com.google.code.kaptcha.impl.DefaultKaptcha
import io.suggest.text.util.TextUtil
import io.suggest.util.logs.{IMacroLogs, MacroLogsImpl}
import models.mproj.ICommonDi
import play.api.data.Form
import play.api.mvc._
import util.captcha.{CaptchaUtil, ICaptchaUtilDi}
import util.captcha.CaptchaUtil._

import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.14 9:57
 * Description: Контроллер и сопутствующая утиль для капчеванию пользователей.
 */
@Singleton
class Captcha @Inject() (
                          override val captchaUtil: CaptchaUtil,
                          override val mCommonDi  : ICommonDi
                        )
  extends SioControllerImpl
  with KaptchaGenerator
  with MacroLogsImpl


/** Абстрактный кусок контроллера для генерации капч с помощью какой-то неопределённой библиотеки. */
trait CaptchaGeneratorBase
  extends BaseController
  with IMacroLogs
  with ICaptchaUtilDi
{

  def createCaptchaText: String

  /** Генерация текста цифровой капчи (n цифр от 0 до 9). */
  def createCaptchaDigits: String = {
    val rnd = new Random()
    val l = captchaUtil.DIGITS_CAPTCHA_LEN
    val sb = new StringBuilder(l)
    for(_ <- 1 to l) {
      sb append rnd.nextInt(10)
    }
    sb.toString()
  }

  def createCaptchaImg(ctext: String): Array[Byte]

  protected def _getCaptchaImg(captchaId: String, ctext: String, cookiePath: String)(implicit request: RequestHeader): Result = {
    val ctextCrypt = captchaUtil.cipherer.encryptPrintable(
      ctext,
      ivMaterial = captchaUtil.ivMaterial(captchaId)
    )
    Ok(createCaptchaImg(ctext))
      .as("image/" + captchaUtil.CAPTCHA_FMT_LC)
      .withHeaders(
        EXPIRES       -> "0",
        PRAGMA        -> "no-cache",
        CACHE_CONTROL -> "no-store, no-cache, must-revalidate"
      )
      .withCookies(Cookie(
        name      = captchaUtil.cookieName(captchaId),
        value     = ctextCrypt,
        maxAge    = Some( captchaUtil.COOKIE_MAXAGE_SECONDS ),
        httpOnly  = true,
        secure    = captchaUtil.COOKIE_FLAG_SECURE,
        path      = cookiePath
      ))
  }

  /**
   * Вернуть картинку капчи, выставив зашифрованный ответ на капчу в куки.
   * @param captchaId id капчи, генерится в шаблоне формы. Используется для генерации имени кукиса с ответом.
   * @return image/png
   */
  def getCaptcha(captchaId: String, cookiePath: String) = Action { implicit request =>
    val ctext = createCaptchaText
    LOGGER.trace(s"getCaptcha($captchaId): ctext -> $ctext")
    _getCaptchaImg(captchaId, ctext = ctext, cookiePath = cookiePath)
  }

  /**
   * Вернуть картинку капчи, которая состоит только из цифр.
   * Такую картинку будет проще ввести на мобильном устройстве.
   * @param captchaId id капчи.
   * @return image/png
   */
  def getDigitalCaptcha(captchaId: String, cookiePath: String) = Action { implicit request =>
    val ctext = createCaptchaDigits
    LOGGER.trace(s"getDigitalCaptcha($captchaId): ctext -> $ctext")
    _getCaptchaImg(captchaId, ctext = ctext, cookiePath = cookiePath)
  }

}


/** Генератор капч с помощью библиотеки com.google.code.kaptcha. */
trait KaptchaGenerator extends CaptchaGeneratorBase {

  protected val CAPTCHA_PRODUCER: Producer = {
    val prod = new DefaultKaptcha
    prod.setConfig(new Config(new Properties()))
    prod
  }

  def createCaptchaText: String = CAPTCHA_PRODUCER.createText()

  def createCaptchaImg(ctext: String): Array[Byte] = {
    val img = CAPTCHA_PRODUCER.createImage(ctext)
    val baos = new ByteArrayOutputStream(8192)
    if (!ImageIO.write(img, captchaUtil.CAPTCHA_FMT_LC, baos)) {
      throw new IllegalStateException("ImageIO writer returned false. No writer for captcha image or format " + captchaUtil.CAPTCHA_FMT_LC)
    }
    baos.toByteArray
  }

}


/** Проверка капчи, миксуемая в трейт для проверки введённой капчи. */
trait CaptchaValidator extends IMacroLogs with ICaptchaUtilDi {

  /** Проверить капчу, присланную в форме. Вызывается перез Form.fold().
    * @param form Маппинг формы.
    * @return Форма, в которую может быть залита ошибка поля ввода капчи.
    */
  def checkCaptcha[T](form: Form[T])(implicit request: RequestHeader): Form[T] = {
    val okFormOpt = for {
      captchaId       <- form.data.get(CAPTCHA_ID_FN)
      captchaTyped    <- form.data.get(CAPTCHA_TYPED_FN)
      _cookieName     = captchaUtil.cookieName(captchaId)
      cookieRaw       <- request.cookies.get(_cookieName)
      if {
        lazy val logPrefix = s"checkCaptcha($CAPTCHA_ID_FN, $CAPTCHA_TYPED_FN):"
        try {
          val ctext = captchaUtil.cipherer.decryptPrintable(
            cookieRaw.value,
            ivMaterial = captchaUtil.ivMaterial(captchaId)
          )
          // Бывает юзер вводит английские буквы с помощью кириллицы. Исправляем это:
          // TODO Надо исправлять только буквы
          val captchaTyped2 = captchaTyped.trim.map { TextUtil.mischarFixEnAlpha }
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
      form
    }
    // Отработать отсутствие положительного результата парсинга капчи.
    okFormOpt.getOrElse {
      // Нет результата. Залить в форму ошибки.
      form.withError(CAPTCHA_TYPED_FN, "error.captcha")
    }
  }

  /**
   * Удалить капчу из кукисов.
   * @param form Форма.
   * @param response Http-ответ.
   * @return Модифицированный http-ответ.
   */
  def rmCaptcha(form: Form[_])(response: Result): Result = {
    form.data.get(CAPTCHA_ID_FN).fold(response) { captchaId =>
      response
        .discardingCookies(DiscardingCookie(
          name   = captchaUtil.cookieName(captchaId),
          secure = captchaUtil.COOKIE_FLAG_SECURE
        ))
    }
  }

}
