package util

import play.api.data.Forms._
import java.net.URL
import io.suggest.util.UrlUtil
import gnu.inet.encoding.IDNA
import HtmlSanitizer._
import views.html.helper.FieldConstructor
import views.html.market.lk._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.13 19:01
 * Description: набор хелперов для форм.
 */
object FormUtil {

  val strIdentityF = {s:String => s}
  val strTrimF = {s: String => s.trim }
  val strTrimSanitizeF = {s:String =>
    stripAllPolicy.sanitize(s).trim
  }
  val strTrimBrOnlyF = {s: String =>
    brOnlyPolicy.sanitize(s).trim
  }
  val strTrimSanitizeLowerF = strTrimSanitizeF andThen {_.toLowerCase}
  val strFmtTrimF = {s: String =>
    textFmtPolicy.sanitize(s).trim
  }

  private val allowedProtocolRE = "(?i)https?".r

  def isValidUrl(urlStr: String): Boolean = {
    try {
      new URL(urlStr)
      true

    } catch {
      case ex:Throwable => false
    }
  }

  /** Маппинг для номера этажа в ТЦ. */
  val martFloorM = number(min = -10, max = 200)

  /** Маппинг для секции в ТЦ. */
  val martSectionM = number(min=0, max=200000)

  val nameM = nonEmptyText(maxLength = 64)
    .transform(strTrimSanitizeF, strIdentityF)
  def shopNameM = nameM
  def martNameM = nameM
  def companyNameM = nameM

  val publishedTextM = text(maxLength = 2048)
    .transform(strFmtTrimF, strIdentityF)
  val publishedTextOptM = optional(publishedTextM)

  val townM = nonEmptyText(maxLength = 32)
    .transform(strTrimSanitizeF, strIdentityF)

  val addressM = nonEmptyText(minLength = 10, maxLength = 128)
    .transform(strTrimSanitizeF, strIdentityF)

  def userCatIdM = esIdM

  // TODO Нужен нормальный валидатор телефонов.
  val phoneM = nonEmptyText(minLength = 5, maxLength = 16)

  def martAddressM = addressM

  // Трансформеры для optional-списков.
  def optList2ListF[T] = { optList: Option[List[T]] => optList getOrElse Nil }
  def list2OptListF[T] = { l:List[T] =>  if (l.isEmpty) None else Some(l) }

  /** Маппер form-поля URL в строку URL */
  val urlStrMapper = nonEmptyText(minLength = 8)
    .transform(strTrimF, strIdentityF)
    .verifying("mappers.url.invalid_url", isValidUrl(_))

  /** Маппер form-поля с ссылкой в java.net.URL. */
  val urlMapper = urlStrMapper
    .transform(new URL(_), {url:URL => url.toExternalForm})

  /** Проверить ссылку на возможность добавления сайта в индексацию. */
  val urlAllowedMapper = urlMapper
    .verifying("mappers.url.only_http_https_allowed", { url =>
      allowedProtocolRE.pattern.matcher(url.getProtocol).matches()
    })
    .verifying("mappers.url.hostname_prohibited", { url =>
      UrlUtil.isHostnameValid(url.getHost)
    })


  // Маппер домена. Формат ввода тут пока не проверяется.
  val domainMapper = nonEmptyText(minLength = 4, maxLength = 128)
    .transform(strTrimSanitizeLowerF, strIdentityF)
    .verifying("mappers.url.hostname_prohibited", UrlUtil.isHostnameValid(_))

  // Маппер домена с конвертором в dkey.
  val domain2dkeyMapper = domainMapper
    .transform(UrlUtil.normalizeHostname, {dkey:String => IDNA.toUnicode(dkey)})

  // Маппер для float-значений.
  val floatRe = "[0-9]{1,7}([,.][0-9]{1,2})?".r
  val float = nonEmptyText(maxLength = 15)
    .verifying(floatRe.pattern.matcher(_).matches())
    .transform(_.toFloat, {f: Float => f.toString})

  /** id'шники в ES-моделях генерятся силами ES. Тут маппер для полей, содержащих ES-id. */
  val esIdM = nonEmptyText(minLength=6, maxLength=64)
    .transform(strTrimSanitizeF, strIdentityF)
}


object FormHelpers {

  implicit val myFields = FieldConstructor(lkFieldConstructor.f)

}

