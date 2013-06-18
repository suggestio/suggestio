package util

import play.api.data.Forms._
import java.net.URL

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.13 19:01
 * Description: набор хелперов для форм.
 */
object FormUtil {

  val strIdentityF = {s:String => s}

  val urlStrMapper = nonEmptyText(minLength = 8)
    .transform(_.trim, strIdentityF)
    .verifying("Invalid URL", { urlStr =>
      try {
        new URL(urlStr)
        true
      } catch {
        case ex:Throwable => false
      }
    })

}
