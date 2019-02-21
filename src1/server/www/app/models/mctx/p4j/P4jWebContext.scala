package models.mctx.p4j

import com.google.inject.assistedinject.Assisted
import javax.inject.Inject
import models.req.IReq
import org.pac4j.play.PlayWebContext
import org.pac4j.play.store.PlaySessionStore
import play.api.mvc.AnyContent

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.02.19 18:35
  * Description: Поддержка pac4j вместо штатного pac4j-play костыля для интеграции гос.услуг.
  *
  * Самописный код нужен для интеграции WebContext с [[models.mctx.Context]] и прочего.
  */
final class P4jWebContext @Inject() (
                                      @Assisted mreq        : IReq[AnyContent],
                                      sessionStore          : PlaySessionStore,
                                    )
  extends PlayWebContext(mreq, mreq.body, sessionStore)
{

  override def getServerName: String =
    mreq.domain

  override def getScheme: String =
    mreq.myProto

  override def getServerPort: Int = {
    val hostNameParts = mreq.host.split(':')
    if (hostNameParts.length > 1)
      hostNameParts(1).toInt
    else if (mreq.isTransferSecure)
      443
    else
      80
  }

  override def isSecure: Boolean =
    mreq.isTransferSecure

  override def getRemoteAddr: String =
    mreq.remoteClientAddress

  // Запрет mutable-сборки ответа.
  // Стандартная логика обработки не используется по очевидным причинам, и эти методы тоже не должны никогда дёргаться.
  private def _unsupported = throw new UnsupportedOperationException("Mutable response impossible.")
  override def setResponseStatus(code: Int): Unit = _unsupported
  override def setResponseHeader(name: String, value: String): Unit = _unsupported
  override def setRequestAttribute(name: String, value: Any): Unit = _unsupported
  override def setResponseContentType(content: String): Unit = _unsupported

}


/** Guice-factory для сборки инстансов [[P4jWebContext]]. */
trait P4jWebContextFactory {
  def fromRequest()(implicit request: IReq[AnyContent]): P4jWebContext
}
