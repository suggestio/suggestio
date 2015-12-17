package models.req

import java.net.InetAddress

import models.msc.ScJsState
import play.api.http.HeaderNames
import play.api.mvc.RequestHeader
import play.core.parsers.FormUrlEncodedParser
import util.PlayMacroLogsImpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 21:52
 * Description: Extended Request Header -- расширение API RequestHeader для нужд sio.
 */
object ExtReqHdr extends PlayMacroLogsImpl {

  import LOGGER._

  /** Скомпиленный регэксп для сплиттинга значения X_FORWARDED_FOR. */
  val X_FW_FOR_SPLITTER_RE = ",\\s*".r


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
    val splitsIter = X_FW_FOR_SPLITTER_RE.split(raw)
      .iterator
      .filter { addr =>
        try {
          // TODO Доставать только ip адреса, отсеивать хлам.
          InetAddress.getByName(addr)
          true
        } catch {
          case ex: Throwable =>
            warn(logPrefix + "Invalid forwarded address: " + addr)
            false
        }
      }
    if (splitsIter.hasNext) {
      splitsIter.next()
    } else {
      warn(logPrefix + "No more forwarding tokens. Returning raw value.")
      raw
    }
  }

}


import ExtReqHdr._


/** Расширение play RequestHeader полями и функциями S.io. */
trait ExtReqHdr extends RequestHeader {

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

}

