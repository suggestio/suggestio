@this(jsRoutesUtil: util.tpl.JsRoutesUtil)
@()(implicit ctx: Context)

@* Код js-роутера, который раздается контроллером, либо включается в верстку соотв.страницы. *@

@import io.suggest.sc.ScConstants.JsRouter._
@import ctx.request
@import views.js.stuff.jsRevRouterTpl
@import io.suggest.routes.JsRoutesConst

@* Помимо обычных sc-роут, ещё добавляются роуты формы логина, чтобы юзер мог логинится/регаться прямо из выдачи,
   что необходимо в случае мобильного приложения. *@
@jsRevRouterTpl( JsRoutesConst.GLOBAL_NAME, cdn = true )(
  jsRoutesUtil.sc(): _*
)

@* Когда скрипт загрузился и выполнился, будет вызвана функция инициализации с указанным именем, если задана. *@
if(typeof window.@ASYNC_INIT_FNAME == 'function') {
  window.@(ASYNC_INIT_FNAME)();
}
