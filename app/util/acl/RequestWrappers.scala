package util.acl

import models.req.{RequestHeaderWrap, ExtReqHdr, SioReqMd}
import util.acl.PersonWrapper._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

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
      new RequestHeaderWrap with RichRequestHeader {
        override def request      = rh
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





case class RequestHeaderAsRequest(request: RequestHeader) extends Request[Nothing] with RequestHeaderWrap {
  override def body: Nothing = {
    throw new UnsupportedOperationException("This is request headers wrapper. Body never awailable here.")
  }
}


