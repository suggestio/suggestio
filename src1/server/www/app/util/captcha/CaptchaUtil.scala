package util.captcha

import javax.inject.{Inject, Singleton}

import io.suggest.sec.util.CipherUtil
import models.mproj.ICommonDi
import play.api.http.HeaderNames
import play.api.mvc.{RequestHeader, SessionCookieBaker}
import play.api.data.Forms._
import util.FormUtil._

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
class CaptchaUtil @Inject() (
  protected val cipherUtil    : CipherUtil,
  mCommonDi                   : ICommonDi
) {

  import mCommonDi.configuration

  def CAPTCHA_FMT_LC = "png"

  /** Кол-во цифр в цифровой капче (длина строки капчи). */
  def DIGITS_CAPTCHA_LEN = 5

  val COOKIE_MAXAGE_SECONDS = configuration.getOptional[Int]("captcha.cookie.maxAge.seconds").getOrElse(1800)
  val COOKIE_FLAG_SECURE = configuration.getOptional[Boolean]("captcha.cookie.secure")
    .getOrElse( mCommonDi.current.injector.instanceOf[SessionCookieBaker].secure )

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

}


/** Интерфейс для доступа к инжектируемому через DI инстансу [[CaptchaUtil]]. */
trait ICaptchaUtilDi {
  def captchaUtil: CaptchaUtil
}
