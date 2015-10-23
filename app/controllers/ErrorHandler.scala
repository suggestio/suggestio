package controllers

import _root_.util.acl.RichRequestHeader
import _root_.util.jsa.init.JsInitTargetsDfltT
import com.google.inject.{Singleton, Provider, Inject}
import models.{ContextT, Context, Context2Factory}
import play.api.http.DefaultHttpErrorHandler
import play.api._
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc._
import play.api.routing.Router
import scala.concurrent._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.10.15 19:00
 * Description: Контроллер, обрабатывающий ошибки других контроллеров.
 * Пришел на смену убогих методов в GlobalSettings.
 * @see [[https://www.playframework.com/documentation/2.4.x/ScalaErrorHandling#Extending-the-default-error-handler]]
 */
@Singleton
class ErrorHandler @Inject() (
  env                             : Environment,
  config                          : Configuration,
  sourceMapper                    : OptionalSourceMapper,
  router                          : Provider[Router],
  override val _contextFactory    : Context2Factory,
  override val messagesApi        : MessagesApi,
  implicit val ex                 : ExecutionContext
)
  extends DefaultHttpErrorHandler(env, config, sourceMapper, router)
  with I18nSupport
  with JsInitTargetsDfltT
  with ContextT
{

  /** Кешируем значение сравнения текущего режима приложения с Mode.Prod. */
  private val _isProd = env.mode == Mode.Prod

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    if (_isProd) {
      http404Fut(request)
    } else {
      super.onNotFound(request, message)
    }
  }

  /** Рендер 404-результата. */
  def http404ctx(implicit ctx: Context): Result = {
    Results.NotFound( views.html.static.http404Tpl() )
  }

  /** Асинхронный рендер 404-результата для сырого запроса. */
  def http404Fut(implicit request: RequestHeader): Future[Result] = {
    RichRequestHeader(request) map { implicit rrh =>
      http404ctx
    }
  }

}
