package util.cdn

import akka.stream.Materializer
import javax.inject.Inject

import io.suggest.util.logs.MacroLogsImpl
import play.api.Configuration
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.Future
import play.api.http.HeaderNames._
import util.acl.AclUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.10.14 14:55
 * Description: Дамп forwarded-хидеров для отладки интеграции с CDN.
 * 2015.jun.18: Параметр конфига xff.dump.headers.enabled удалён. Теперь проверяется
 * активность trace-логгинга в данном модуле.
 */

class DumpXffHeaders @Inject() (
  configuration             : Configuration,
  aclUtil                   : AclUtil,
  override implicit val mat : Materializer
)
  extends Filter
    with MacroLogsImpl
{

  /** Какие заголовки дампить? Если фильтр отключён, то эта настройка всё равно прочитается. */
  lazy val DUMP_HEADER_NAMES: Seq[String] = {
    configuration.getOptional[Seq[String]]("xff.dump.headers.names")
      .map(_.toSeq)
      .getOrElse { Seq(HOST, X_FORWARDED_FOR, "X-Client-Ip", "X-Real-Ip", X_FORWARDED_PROTO, X_FORWARDED_HOST) }
  }

  /** Применить фильтр для обработки одного запроса. */
  override def apply(f: (RequestHeader) => Future[Result])(rh0: RequestHeader): Future[Result] = {
    val rh = aclUtil.reqHdrFromRequestHdr( rh0 )
    val resultFut = f(rh)

    // Параллельно начинаем дампить хидеры в лог.
    if (LOGGER.underlying.isTraceEnabled) {
      val sb = new StringBuilder("Fwd headers for ")
        .append(rh.method)
        .append(' ')
        .append(rh.uri)
        .append(" <- ")
        .append(rh.remoteClientAddress)
        .append(" secure=").append(rh.isTransferSecure).append(":\n")

      rh.headers
        .toMap
        .iterator
        .filter {
          case (k, vs)  =>
            vs.nonEmpty && DUMP_HEADER_NAMES.exists { _ equalsIgnoreCase k }
        }
        .foreach { case (k, vs) =>
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

      LOGGER.trace( sb.toString() )
    }

    // Вернуть исходный результат.
    resultFut
  }

}
