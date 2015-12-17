package util.acl

import java.net.InetAddress

import models.msc.ScJsState
import models.req.SioReqMd
import play.api.http.HeaderNames
import play.core.parsers.FormUrlEncodedParser
import util.PlayMacroLogsImpl
import util.acl.PersonWrapper._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}


object SioWrappedRequest {
  implicit def request2sio[A](request: Request[A]): SioWrappedRequest[A] = {
    new SioWrappedRequest[A](request)
  }
}

/** Экранизация WrappedRequest[A] с примесями нужд s.io. */
class SioWrappedRequest[A](request: Request[A]) extends WrappedRequest(request) with SioRequestHeader

/** Абстрактный реквест, в рамках которого содержится инфа о текущем sio-юзере. */
abstract class AbstractRequestWithPwOpt[A](request: Request[A])
  extends SioWrappedRequest(request)
  with RichRequestHeader


object SioRequestHeader extends PlayMacroLogsImpl {

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

  implicit def request2sio[A](request: Request[A]): SioRequestHeader = {
    SioWrappedRequest.request2sio(request)
  }
}


object RichRequestHeader {

  def apply(rh: RequestHeader)(implicit ec: ExecutionContext): Future[RichRequestHeader] = {
    val _pwOpt = PersonWrapper.getFromRequest(rh)
    SioReqMd.fromPwOpt(_pwOpt).map { srm =>
      new RequestHeaderWrapper with RichRequestHeader {
        override def underlying   = rh
        override def pwOpt        = _pwOpt
        override def sioReqMd     = srm
      }
    }
  }
}

/** Интерфейс полей из [[AbstractRequestWithPwOpt]]. */
trait RichRequestHeader extends RequestHeader {
  /** Данные о юзере. */
  def pwOpt: PwOpt_t
  /** Дополнительные метаданные для рендера ответа. */
  def sioReqMd: SioReqMd
  /** Является ли текущий юзер суперюзером? */
  def isSuperuser = PersonWrapper isSuperuser pwOpt
  /** Залогинен ли текущий юзер? */
  def isAuth = pwOpt.isDefined
}


/** Расширение play RequestHeader полями и функциями S.io. */
trait SioRequestHeader extends RequestHeader {
  import SioRequestHeader._

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


/**
 * Wrapped-реквест для передачи pwOpt.
 * @param pwOpt Опциональный PersonWrapper.
 * @param request Исходнный реквест.
 * @tparam A Подтип реквеста.
 */
case class RequestWithPwOpt[A](pwOpt: PwOpt_t, request: Request[A], sioReqMd: SioReqMd)
  extends AbstractRequestWithPwOpt(request)


/** Админство магазина. */
abstract class AbstractRequestForShopAdm[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def shopId: String
}



/** Враппер над RequestHeader. */
trait RequestHeaderWrapper extends RequestHeader {
  def underlying: RequestHeader
  override def id             = underlying.id
  override def secure         = underlying.secure
  override def uri            = underlying.uri
  override def remoteAddress  = underlying.remoteAddress
  override def queryString    = underlying.queryString
  override def method         = underlying.method
  override def headers        = underlying.headers
  override def path           = underlying.path
  override def version        = underlying.version
  override def tags           = underlying.tags
}

case class RequestHeaderAsRequest(underlying: RequestHeader) extends Request[Nothing] with RequestHeaderWrapper {
  override def body: Nothing = {
    throw new UnsupportedOperationException("This is request headers wrapper. Body never awailable here.")
  }
}


