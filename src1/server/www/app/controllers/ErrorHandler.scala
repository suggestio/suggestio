package controllers

import _root_.util.jsa.init.ITargetsEmpty
import javax.inject.{Inject, Provider, Singleton}
import io.suggest.ctx.CtxData
import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import models.mctx.{Context, Context2Factory}
import models.req.IReqHdr
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_FOUND}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.routing.Router
import util.acl.AclUtil

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
final class ErrorHandler @Inject() (
                                     env                             : Environment,
                                     config                          : Configuration,
                                     sourceMapper                    : OptionalSourceMapper,
                                     router                          : Provider[Router],
                                     contextFactory                  : Context2Factory,
                                     aclUtil                         : AclUtil,
                                     override val messagesApi        : MessagesApi,
                                     implicit private val ec         : ExecutionContext,
                                   )
  extends DefaultHttpErrorHandler(env, config, sourceMapper, router)
  with I18nSupport
  with ITargetsEmpty
{

  def getContext2(implicit rrh: IReqHdr): Context = {
    contextFactory.create(rrh, implicitly, CtxData.empty)
  }

  /** Кешируем значение сравнения текущего режима приложения с Mode.Prod. */
  private val _isProd = env.mode == Mode.Prod

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    if (_isProd && message.isEmpty) {
      CustomResponces(request).notFound()
    } else {
      super.onNotFound(request, message)
    }
  }


  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val isAcceptingHtml = request.headers
      .get( HttpConst.Headers.ACCEPT )
      .fold(false)( _ contains MimeConst.TEXT_HTML )

    if (isAcceptingHtml) {
      // Разрешить ответы на 5хх ошибки, которые зачем-то теперь запрещены в DefaultHttpErrorHandler.
      statusCode match {
        case BAD_REQUEST      => onBadRequest(request, message)
        case FORBIDDEN        => onForbidden(request, message)
        case NOT_FOUND        => onNotFound(request, message)
        case _                => onOtherClientError(request, statusCode, message)
      }

    } else {
      // TODO Отработать Accept: JSON
      // Нет возможности возвращать HTML, отвечает plain text'ом
      var respBody = s"HTTP $statusCode"

      if (message.nonEmpty) {
        respBody = s"$respBody: $message"
      }

      val res = Results.Status(statusCode)(respBody)
      Future.successful(res)
    }
  }

  /** Контейнер кастомных sio-ответы на запросы. Используются на замену дефолту. */
  class CustomResponces(val ctx: Context) {

    /** Рендер 404-результата. */
    def notFound(): Future[Result] = {
      // TODO Добавить поддержку рендера message.
      val resp = Results.NotFound( views.html.static.http404Tpl()(ctx) )
      Future.successful( resp )
    }

  }

  object CustomResponces {
    def apply(rh: RequestHeader): CustomResponces =
      apply( aclUtil.reqHdrFromRequestHdr(rh) )
    def apply(req: IReqHdr): CustomResponces =
      apply( getContext2(req) )
    def apply(ctx: Context): CustomResponces =
      new CustomResponces( ctx )
  }

}


/** DI-доступ к инстансу контроллеру обработчика ошибок. */
trait IErrorHandler {
  def errorHandler    : ErrorHandler
}
