package util.captcha

import java.io.ByteArrayOutputStream
import java.util.Properties

import akka.util.ByteString
import com.google.code.kaptcha.Producer
import com.google.code.kaptcha.impl.DefaultKaptcha
import com.google.code.kaptcha.util.Config
import javax.inject.{Inject, Singleton}
import io.suggest.sec.util.CipherUtil
import io.suggest.text.util.TextUtil
import io.suggest.util.logs.MacroLogsImpl
import javax.imageio.ImageIO
import models.mproj.ICommonDi
import play.api.data.Form
import play.api.http.HeaderNames
import play.api.mvc._
import play.api.data.Forms._
import util.FormUtil._

import scala.util.Random

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
)
  extends MacroLogsImpl
{

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

  def checkCaptcha2(captchaId: String, captchaTyped: String)(implicit request: RequestHeader): Boolean =
    checkCaptcha2(captchaId -> captchaTyped)( ICaptchaGetter.PlainKvGetter, request )
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
  def createCaptchaDigits(): String = {
    val rnd = new Random()
    val l = DIGITS_CAPTCHA_LEN
    val sb = new StringBuilder(l)
    for(_ <- 1 to l) {
      sb append rnd.nextInt(10)
    }
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
    if ( !ImageIO.write(img, CAPTCHA_FMT_LC, baos) )
      throw new IllegalStateException("ImageIO writer returned false. No writer for captcha image or format " + CAPTCHA_FMT_LC)
    ByteString.fromArrayUnsafe( baos.toByteArray )
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
