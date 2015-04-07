package util.xplay

import models.{ContextImpl, Context}
import play.api.mvc.{Results, RequestHeader, Result}
import play.api.i18n.Messages.Implicits._
import play.api.Play.current

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.04.15 9:36
 * Description: Заготовка для обработчика ошибок play.
 * По идее можно будет допилить его до реализации error handler'а и закинуть в play через Global.
 */
object SioHttpErrorHandler {

  /** Тело экшена, генерирующее страницу 404. Используется при минимальном окружении. */
  def http404AdHoc(implicit request: RequestHeader): Result = {
    http404ctx(ContextImpl())
  }

  def http404ctx(implicit ctx: Context): Result = {
    Results.NotFound( views.html.static.http404Tpl() )
  }

  /** Враппер, генерящий фьючерс с телом экшена http404(RequestHeader). */
  def http404Fut(implicit request: RequestHeader): Future[Result] = {
    Future successful http404AdHoc
  }

}
