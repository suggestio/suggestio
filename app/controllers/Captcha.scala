package controllers

import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.imageio.ImageIO
import com.google.code.kaptcha.util.Config
import com.typesafe.scalalogging.slf4j
import io.suggest.util.TextUtil
import play.api.Play.{current, configuration}
import com.google.code.kaptcha.Producer
import com.google.code.kaptcha.impl.DefaultKaptcha
import play.api.data.Form
import play.api.mvc._
import util.{PlayMacroLogsImpl, PlayLazyMacroLogsImpl}
import util.captcha.CipherUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.14 9:57
 * Description: Контроллер и сопутствующая утиль для капчеванию пользователей.
 */
object Captcha extends KaptchaGenerator with PlayMacroLogsImpl {
}


/** Абстрактный кусок контроллера для генерации капч с помощью какой-то неопределённой библиотеки. */
trait CaptchaGeneratorBase extends Controller {

  def LOGGER : slf4j.Logger

  val CAPTCHA_FMT_LC = "png"

  def createCaptchaText: String
  def createCaptchaImg(ctext: String): Array[Byte]

  val COOKIE_MAXAGE_SECONDS = configuration.getInt("captcha.cookie.maxAge.seconds") getOrElse 1800
  val COOKIE_FLAG_SECURE = configuration.getBoolean("session.secure") getOrElse false

  /**
   * Вернуть картинку капчи, выставив зашифрованный ответ на капчу в куки.
   * @param captchaId id капчи, генерится в шаблоне формы. Используется для генерации имени кукиса с ответом.
   * @return image
   */
  def getCaptcha(captchaId: String) = Action { implicit request =>
    val ctext = createCaptchaText
    LOGGER.trace(s"getCaptcha($captchaId): ctext -> $ctext")
    val ctextCrypt = CipherUtil.encryptPrintable(ctext, ivMaterial = ivMaterial(captchaId))
    Ok(createCaptchaImg(ctext))
      .withHeaders(
        CONTENT_TYPE  -> ("image/" + CAPTCHA_FMT_LC),
        EXPIRES       -> "0",
        PRAGMA        -> "no-cache",
        CACHE_CONTROL -> "no-store, no-cache, must-revalidate"
      )
      .withCookies(Cookie(
        name = cookieName(captchaId),
        value = ctextCrypt,
        maxAge = Some(COOKIE_MAXAGE_SECONDS),
        httpOnly = true,
        secure = COOKIE_FLAG_SECURE
      ))
  }


  def cookieName(captchaId: String) = "cha." + captchaId

  def ivMaterial(captchaId: String)(implicit request: RequestHeader): Array[Byte] = {
    request.headers
      .get(USER_AGENT)
      .fold(captchaId) { _ + captchaId }
      .getBytes
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
    if (!ImageIO.write(img, CAPTCHA_FMT_LC, baos)) {
      throw new IllegalStateException("ImageIO writer returned false. No writer for captcha image or format " + CAPTCHA_FMT_LC)
    }
    baos.toByteArray
  }

}


/** Проверка капчи, миксуемая в трейт для проверки введённой капчи. */
trait CaptchaValidator {
  def LOGGER : slf4j.Logger

  /** Проверить капчу, присланную в форме. Вызывается перез Form.fold().
    * @param form Маппинг формы.
    * @param captchaIdFn Название hidden-поля формы, где содержится id капчи.
    * @param captchaValueFn Название поля формы, куда юзер должен был ввести значение капчи.
    * @return Форма, в которую может быть залита ошибка поля ввода капчи.
    */
  def checkCaptcha[T](form: Form[T], captchaIdFn: String, captchaValueFn: String)(implicit request: RequestHeader): Form[T] = {
    val maybeCookieOk = form.data.get(captchaIdFn) flatMap { captchaId =>
      form.data.get(captchaValueFn) flatMap { captchaTyped =>
        val cookieName = Captcha.cookieName(captchaId)
        request.cookies.get(cookieName)
          .filter { cookie =>
            try {
              val ivMaterial = Captcha.ivMaterial(captchaId)
              val ctext = CipherUtil.decryptPrintable(cookie.value, ivMaterial = ivMaterial)
              // Бывает юзер вводит английские буквы с помощью кириллицы. Исправляем это:
              // TODO Надо исправлять только буквы
              //val captchaTyped2 = captchaTyped.trim.map { TextUtil.mischarFixEn }
              // TODO Допускать неточное совпадение капчи?
              val result = ctext equalsIgnoreCase captchaTyped
              if (!result)
                LOGGER.trace(s"checkCaptcha($captchaIdFn, $captchaValueFn): Invalid captcha typed. expected = $ctext, typed = $captchaTyped")
              result
            } catch {
              case ex: Exception =>
                LOGGER.warn(s"checkCaptcha($captchaIdFn, $captchaValueFn): Failed", ex)
                false
            }
        }
      }
    }
    maybeCookieOk.fold {
      // Нет результата. Залить в форму ошибки.
      form.withError(captchaValueFn, "error.captcha")
    } { _ =>
      // Есть что-то в результате. Значит капчка пропарсилась.
      form
    }
  }

  /**
   * Удалить капчу из кукисов.
   * @param form Форма.
   * @param captchaIdFn Название поля с id капчи.
   * @param response Http-ответ.
   * @return Модифицированный http-ответ.
   */
  def rmCaptcha(form: Form[_], captchaIdFn: String, response: Result) = {
    form.data.get(captchaIdFn).fold(response) { captchaId =>
      val cookieName = Captcha.cookieName(captchaId)
      response
        .discardingCookies(DiscardingCookie(name = cookieName, secure = Captcha.COOKIE_FLAG_SECURE))
    }
  }
}
