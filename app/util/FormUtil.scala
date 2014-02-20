package util

import play.api.data.Forms._
import java.net.URL
import io.suggest.util.UrlUtil
import gnu.inet.encoding.IDNA
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.13 19:01
 * Description: набор хелперов для форм.
 */
object FormUtil {

  val strIdentityF = {s:String => s}
  val strTrimF = {s:String => s.trim}
  val strTrimLowerF = {s:String => s.trim.toLowerCase}

  private val allowedProtocolRePattern = "(?i)https?".r

  def isValidUrl(urlStr: String): Boolean = {
    try {
      new URL(urlStr)
      true

    } catch {
      case ex:Throwable => false
    }
  }


  // Трансформеры для optional-списков.
  def optList2ListF[T] = { optList: Option[List[T]] => optList getOrElse Nil }
  def list2OptListF[T] = { l:List[T] =>  if (l.isEmpty) None else Some(l) }

  /** Маппер form-поля URL в строку URL */
  val urlStrMapper = nonEmptyText(minLength = 8)
    .transform(_.trim, strIdentityF)
    .verifying("mappers.url.invalid_url", isValidUrl(_))

  /** Маппер form-поля с ссылкой в java.net.URL. */
  val urlMapper = urlStrMapper
    .transform(new URL(_), {url:URL => url.toExternalForm})

  /** Проверить ссылку на возможность добавления сайта в индексацию. */
  val urlAllowedMapper = urlMapper
    .verifying("mappers.url.only_http_https_allowed", { url =>
      allowedProtocolRePattern.pattern.matcher(url.getProtocol).matches()
    })
    .verifying("mappers.url.hostname_prohibited", { url =>
      UrlUtil.isHostnameValid(url.getHost)
    })


  // Маппер домена. Формат ввода тут пока не проверяется.
  val domainMapper = nonEmptyText(minLength = 4, maxLength = 128)
    .transform(strTrimLowerF, strIdentityF)
    .verifying("mappers.url.hostname_prohibited", UrlUtil.isHostnameValid(_))

  // Маппер домена с конвертором в dkey.
  val domain2dkeyMapper = domainMapper
    .transform(UrlUtil.normalizeHostname, {dkey:String => IDNA.toUnicode(dkey)})

  // Маппер для float-значений.
  val floatRe = "[0-9]{1,7}([,.][0-9]{1,2})?".r
  val float = nonEmptyText(maxLength = 15)
    .verifying(floatRe.pattern.matcher(_).matches())
    .transform(_.toFloat, {f: Float => f.toString})
}
