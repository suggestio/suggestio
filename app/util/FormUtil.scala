package util

import play.api.data.Forms._
import java.net.URL
import io.suggest.util.UrlUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.13 19:01
 * Description: набор хелперов для форм.
 */
object FormUtil {

  val strIdentityF = {s:String => s}

  private val allowedProtocolRePattern = "^(?i)https?^".r.pattern

  // Маппер form-поля URL
  val urlMapper = nonEmptyText(minLength = 8)
    .transform(_.trim, strIdentityF)
    .verifying("mappers.url.invalid_url", { urlStr =>
      try {
        new URL(urlStr)
        true

      } catch {
        case ex:Throwable => false
      }
    })
    .transform(new URL(_), {url:URL => url.toExternalForm})

  // Проверить ссылку на возможность добавления сайта в индексацию.
  val urlAllowedMapper = urlMapper
    .verifying("mappers.url.only_http_https_allowed", { url =>
      allowedProtocolRePattern.matcher(url.getProtocol).matches()
    })
    .verifying("mappers.url.hostname_prohibited", { url =>
      UrlUtil.isHostnameValid(url.getHost)
    })

}
