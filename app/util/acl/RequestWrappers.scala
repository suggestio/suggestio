package util.acl

import models.req.{ExtReqHdr, SioReqMd}
import util.acl.PersonWrapper._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}


object SioWrappedRequest {
  def request2sio[A](request: Request[A]): SioWrappedRequest[A] = {
    new SioWrappedRequest[A](request)
  }
}

/** Экранизация WrappedRequest[A] с примесями нужд s.io. */
class SioWrappedRequest[A](request: Request[A])
  extends WrappedRequest(request)
  with ExtReqHdr

/** Абстрактный реквест, в рамках которого содержится инфа о текущем sio-юзере. */
abstract class AbstractRequestWithPwOpt[A](request: Request[A])
  extends SioWrappedRequest(request)
  with RichRequestHeader



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
  def isSuperuser = PersonWrapper.isSuperuser(pwOpt)
  /** Залогинен ли текущий юзер? */
  def isAuth = pwOpt.isDefined
}



/**
 * Wrapped-реквест для передачи pwOpt.
 * @param pwOpt Опциональный PersonWrapper.
 * @param request Исходнный реквест.
 * @tparam A Подтип реквеста.
 */
case class RequestWithPwOpt[A](pwOpt: PwOpt_t, request: Request[A], sioReqMd: SioReqMd)
  extends AbstractRequestWithPwOpt(request)



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


