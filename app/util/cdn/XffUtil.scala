package util.cdn

import play.api.mvc.{Filter, Result, RequestHeader}
import util.PlayLazyMacroLogsImpl
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

object DumpXffHeaders {

  val IS_ENABLED = configuration.getBoolean("xff.dump.headers.enabled") getOrElse false

  /** Какие заголовки дампить? Если фильтр отключён, то эта настройка всё равно прочитается. */
  lazy val DUMP_HEADER_NAMES: Seq[String] = {
    configuration.getStringList("xff.dump.headers.names")
      .map(_.toSeq)
      .getOrElse { Seq(X_FORWARDED_FOR, "X-Client-Ip", "X-Real-Ip", X_FORWARDED_PROTO, X_FORWARDED_HOST) }
  }

}

class DumpXffHeaders extends Filter with PlayLazyMacroLogsImpl {

  override def apply(f: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    import DumpXffHeaders._
    val resultFut = f(rh)
    // Параллельно начинаем дампить хидеры в лог.
    if (IS_ENABLED) {
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
        LOGGER.trace(sb.toString())
      }
    }
    // Вернуть исходный результат.
    resultFut
  }

}
