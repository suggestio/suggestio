package controllers

import _root_.util.jsa.init.ITargetsEmpty
import javax.inject.{Inject, Provider, Singleton}
import io.suggest.sec.util.SessionUtil
import io.suggest.www.m.mctx.CtxData
import models.mctx.{Context, Context2Factory}
import models.req.{IReqHdr, MReqHdr, MSioUsers}
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.i18n.{I18nSupport, MessagesApi}
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
  contextFactory                  : Context2Factory,
  mSioUsers                       : MSioUsers,
  sessionUtil                     : SessionUtil,
  override val messagesApi        : MessagesApi,
  implicit val ex                 : ExecutionContext
)
  extends DefaultHttpErrorHandler(env, config, sourceMapper, router)
  with I18nSupport
  with ITargetsEmpty
{

  implicit private def getContext2(implicit rrh: IReqHdr): Context = {
    contextFactory.create(rrh, implicitly[play.api.i18n.Messages], CtxData.empty)
  }

  /** Кешируем значение сравнения текущего режима приложения с Mode.Prod. */
  private val _isProd = env.mode == Mode.Prod

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    if (_isProd) {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)
      val reqHdr = MReqHdr(request, user)
      http404Fut(reqHdr)
    } else {
      super.onNotFound(request, message)
    }
  }

  /** Асинхронный рендер 404-результата для сырого запроса. */
  def http404Fut(implicit req: IReqHdr): Future[Result] = {
    val resp = http404ctx
    Future.successful(resp)
  }

  /** Рендер 404-результата. */
  def http404ctx(implicit ctx: Context): Result = {
    Results.NotFound( views.html.static.http404Tpl() )
  }

}


/** DI-доступ к инстансу контроллеру обработчика ошибок. */
trait IErrorHandler {
  def errorHandler    : ErrorHandler
}
