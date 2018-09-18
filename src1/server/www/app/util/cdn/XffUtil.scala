package util.cdn

import javax.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import play.api.Configuration
import play.api.mvc._

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
  aclUtil                   : AclUtil
)
  extends EssentialFilter
  with MacroLogsImpl
{

  /** Какие заголовки дампить? Если фильтр отключён, то эта настройка всё равно прочитается. */
  lazy val DUMP_HEADER_NAMES: Seq[String] = {
    configuration.getOptional[Seq[String]]("xff.dump.headers.names")
      .map(_.toSeq)
      .getOrElse {
        HOST ::
          X_FORWARDED_FOR ::
          "X-Client-Ip" ::
          "X-Real-Ip" ::
          X_FORWARDED_PROTO ::
          X_FORWARDED_HOST ::
          Nil
      }
  }

  override def apply(next: EssentialAction): EssentialAction = {
    EssentialAction { rh0 =>
      val rh = aclUtil.reqHdrFromRequestHdr( rh0 )
      val resultFut = next(rh)

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

      // Вернуть исходный акк.
      resultFut
    }
  }

}
