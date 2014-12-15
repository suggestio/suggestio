package util.cdn

import play.api.mvc.{Result, RequestHeader, Filter}
import util.PlayMacroLogsImpl
import play.api.Play.{current, configuration}

import scala.concurrent.Future
import play.api.http.HeaderNames._
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.10.14 14:55
 * Description: Дамп forwarded-хидеров для отладки интеграции с CDN.
 */
object XffUtil {

  val DUMP_FWD_HEADERS: Boolean = configuration.getBoolean("xff.dump.headers.enabled") getOrElse false

  lazy val DUMP_HEADER_NAMES: Seq[String] = {
    configuration.getStringList("xff.dump.headers.names")
      .map(_.toSeq)
      .getOrElse { Seq(X_FORWARDED_FOR, "X-Client-Ip", "X-Real-Ip", X_FORWARDED_PROTO, X_FORWARDED_HOST) }
  }

}


import XffUtil._


object DumpXffHeaders extends Filter with PlayMacroLogsImpl {

  import LOGGER._

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    val resultFut = f(rh)
    // Параллельно начинаем дампить хидеры в лог.
    if (DUMP_FWD_HEADERS) {
      val fwdHdrsIter = rh.headers
        .toMap
        .iterator
        .filter {
          case (k, vs)  =>  vs.nonEmpty && DUMP_HEADER_NAMES.exists { _ equalsIgnoreCase k }
        }
      if (fwdHdrsIter.nonEmpty) {
        val sb = new StringBuilder("Fwd headers for ")
        sb.append(rh.remoteAddress).append("(play-guessed remote address)")
          .append(":\n")
        fwdHdrsIter.foreach { case (k, vs) =>
          sb.append(' ')
            .append(k)
            .append(": ")
          vs.foreach { v =>
            sb.append(v).append(", ")
          }
          sb.setLength(sb.length - 2)
          sb.append('\n')
        }
        sb.setLength(sb.length - 1)
        trace(sb.toString())
      }
    }
    resultFut
  }

}
