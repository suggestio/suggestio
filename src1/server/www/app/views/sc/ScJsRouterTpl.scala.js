@this(jsRoutesUtil: util.tpl.JsRoutesUtil)
@()(implicit ctx: Context)

@* Код js-роутера, который раздается контроллером, либо включается в верстку соотв.страницы. *@

@import io.suggest.sc.ScConstants.JsRouter._
@import io.suggest.sc.ScConstants.SC3_JS_API_GLOBAL_NAME
@import ctx.request
@import views.js.stuff.jsRevRouterTpl
@import io.suggest.routes.JsRoutesConst

@* Помимо обычных sc-роут, ещё добавляются роуты формы логина, чтобы юзер мог логинится/регаться прямо из выдачи,
   что необходимо в случае мобильного приложения. *@
@jsRevRouterTpl( JsRoutesConst.GLOBAL_NAME, cdn = true )(
  jsRoutesUtil.sc(): _*
)

@* When script is loaded, lets notify showcase about readyness. *@
if(
  (typeof @(SC3_JS_API_GLOBAL_NAME) == 'object') &&
  (typeof @(SC3_JS_API_GLOBAL_NAME).@(ASYNC_INIT_FNAME) == 'function')
) {
  @(SC3_JS_API_GLOBAL_NAME).@(ASYNC_INIT_FNAME)();
}
