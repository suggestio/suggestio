package models.req

import inet.ipaddr.IPAddressString

import java.net.InetAddress
import io.suggest.compress.{MCompressAlgo, MCompressAlgos}
import io.suggest.proto.http.HttpConst
import io.suggest.text.util.UrlUtil
import io.suggest.util.logs.MacroLogsImpl
import play.api.http.HeaderNames
import play.api.mvc.RequestHeader

import scala.util.Try

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 21:52
 * Description: Extended Request Header -- расширение API RequestHeader для нужд sio.
 */
object ExtReqHdr extends MacroLogsImpl {

  import LOGGER._

  /** Скомпиленный регэксп для сплиттинга значения X_FORWARDED_FOR. */
  val X_FW_FOR_SPLITTER_RE = ",\\s*".r

  val BACKEND_HOST_RE = "^backend\\.".r

  def lastForwarded(raw: String): String = {
    val splits = X_FW_FOR_SPLITTER_RE.split(raw)
    if (splits.length == 0)
      raw
    else
      splits.last
  }

  def firstForwarded(raw: String): String = {
    val splits = X_FW_FOR_SPLITTER_RE.split(raw)
    if (splits.length == 0)
      raw
    else
      splits.head
  }

  /** Последний из списка форвардов. Там обычно содержится оригинальный ip, если клиент не докинул туда свой. */
  def firstForwardedAddr(raw: String): String = {
    lazy val logPrefix = s"firstForwardedAddr($raw): "
    X_FW_FOR_SPLITTER_RE.split(raw)
      .iterator
      .filter { addr =>
        try {
          // TODO Доставать только ip адреса, отсеивать хлам.
          InetAddress.getByName(addr)   // TODO Избавится от InetAddress, просто матчить без использования сети/DNS.
          true
        } catch {
          case ex: Throwable =>
            warn(logPrefix + "Invalid forwarded address: " + addr)
            false
        }
      }
      .nextOption()
      .getOrElse {
        warn(logPrefix + "No more forwarding tokens. Returning raw value.")
        raw
      }
  }

}


import ExtReqHdr._


/** Расширение play RequestHeader полями и функциями S.io. */
trait ExtReqHdr extends RequestHeader {

  /**
    * Является ли передача данных к юзеру безопасной?
    *
    * В отличие от флага Request.secure, который просто проверяет https'ность связи с reverse-proxy,
    * данный флаг смотрит в ПЕРВУЮ очередь в заголовки, выставленные на стороне nginx во время проксирования.
    *
    * Изначально этот флаг оверрайдил флаг secure (который тоже был lazy val, что небезопасно),
    * но в play-2.6 этот жестокий беспредел решили прикрыть.
    */
  lazy val isTransferSecure: Boolean = {
    headers
      .get( HeaderNames.X_FORWARDED_PROTO )
      .filter(!_.isEmpty)
      .fold {
        secure
      } { protos =>
        firstForwarded(protos)
          .toLowerCase()
          .startsWith( HttpConst.Proto.HTTPS  )
      }
  }

  /** Что используется для связи? http или https. */
  lazy val myProto: String = {
    val P = HttpConst.Proto
    if (isTransferSecure) P.HTTPS else P.HTTP
  }

  /**
   * remote address может содержать несколько адресов через ", ". Например, если клиент послал своё
   * значение X_FORWARDED_FOR, то nginx допишет через запятую новый адрес и прокинет сюда.
   * Тут мы это исправляем, чтобы не было проблем в будущем.
   */
  lazy val remoteClientAddress: String = {
    headers
      .get(HeaderNames.X_FORWARDED_FOR)
      .map { firstForwardedAddr }
      .getOrElse { remoteAddress }
  }

  /** Parsed IP-address from XFF-header's beginning. */
  lazy val remoteClientIpTry = {
    val ipTry = Try( new IPAddressString( remoteClientAddress ).toAddress )
    for (ex <- ipTry.failed)
      LOGGER.error(s"Failed to parse ip addr[${remoteClientAddress}] as ip-address.", ex)
    ipTry
  }


  /** У нас тут своя методика определения хоста: используя X-Forwarded-Host. */
  override lazy val host: String = {
    // Попытаться извлечь запрошенный хостнейм из данных форварда.
    val xffHostPort = for {
      xfhHdr0 <- headers.get( HeaderNames.X_FORWARDED_HOST )
      xfhHdr  = xfhHdr0.trim
      if xfhHdr.nonEmpty
    } yield {
      val h = lastForwarded(xfhHdr)
      // Если входящий запрос на backend, то нужно отобразить его на www.
      BACKEND_HOST_RE
        .replaceFirstIn(h, "")
        .toLowerCase
    }

    // Если форвард не найден, то пытаемся доверять Host-заголовку.
    xffHostPort
      .orElse {
        headers
          .get( HeaderNames.HOST )
          .filter(!_.isEmpty)
      }
      .getOrElse {
        LOGGER.warn("host: unable to detect host for http request " + this)
        // TODO Тут константа, которая должна быть унесена куда-то...
        "suggest.io"
      }
  }

  override lazy val domain = UrlUtil.urlHostStripPort( host )

  /** Какие алгоритмы сжатия готов принять клиент в качестве Content-Encoding? */
  lazy val acceptCompressEncodings: Seq[MCompressAlgo] = {
    headers
      .get( HeaderNames.ACCEPT_ENCODING )
      .iterator
      .flatMap( _.split("[,\\s]+") )
      .flatMap( MCompressAlgos.withHttpContentEncoding )
      .toSeq
  }

}

