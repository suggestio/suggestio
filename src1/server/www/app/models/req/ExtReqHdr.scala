package models.req

import java.net.InetAddress

import io.suggest.util.logs.MacroLogsImpl
import models.msc.ScJsState
import play.api.http.HeaderNames
import play.api.mvc.RequestHeader
import play.core.parsers.FormUrlEncodedParser

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
      .toStream
      .headOption
      .getOrElse {
        warn(logPrefix + "No more forwarding tokens. Returning raw value.")
        raw
      }
  }

}


import ExtReqHdr._


/** Расширение play RequestHeader полями и функциями S.io. */
trait ExtReqHdr extends RequestHeader {

  /** Сюда перенесён код из Context.isSecure для возможности проброса логики внутрь play. */
  abstract override lazy val secure: Boolean = {
    headers
      .get( HeaderNames.X_FORWARDED_PROTO )
      .filter(!_.isEmpty)
      .fold (super.secure) { protos =>
        firstForwarded(protos)
          .toLowerCase()
          .startsWith( "https" )
      }
  }

  /** Что используется для связи? http или https. */
  lazy val myProto: String = {
    if (secure) "https" else "http"
  }

  /**
   * remote address может содержать несколько адресов через ", ". Например, если клиент послал своё
   * значение X_FORWARDED_FOR, то nginx допишет через запятую новый адрес и прокинет сюда.
   * Тут мы это исправляем, чтобы не было проблем в будущем.
   */
  abstract override lazy val remoteAddress: String = {
    headers
      .get(HeaderNames.X_FORWARDED_FOR)
      .map { firstForwardedAddr }
      .getOrElse { super.remoteAddress }
  }

  /** Кравлеры при индексации !#-страниц используют ссылки, содержащие что-то типа "?_escaped_fragment_=...". */
  lazy val ajaxEscapedFragment: Option[String] = {
    queryString
      .get("_escaped_fragment_")
      .flatMap(_.headOption)
      // TODO Нужно делать URL unescape тут?
  }

  /** Переданное js-состояние скрипта выдачи, закинутое в ajax escaped_fragment. */
  lazy val ajaxJsScState: Option[ScJsState] = {
    ajaxEscapedFragment
      .flatMap { raw =>
        try {
          val r = FormUrlEncodedParser.parseNotPreservingOrder(raw)
          Some(r)
        } catch {
          case ex: Exception =>
            LOGGER.debug("Failed to parse ajax-escaped fragment.", ex)
            None
        }
      }
      .flatMap { aefMap =>
        val qsb = ScJsState.qsbStandalone
        qsb.bind("", aefMap)
      }
      .flatMap {
        case Right(res) =>
          Some(res)
        case Left(errMsg) =>
          LOGGER.warn(s"_geoSiteResult(): Failed to bind ajax escaped_fragment '$ajaxEscapedFragment' from '$remoteAddress': $errMsg")
          None
      }
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
        .replaceFirstIn(h, "www.")
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
        "suggest.io"
      }
  }

}

