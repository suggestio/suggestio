@()(implicit ctx: Context)

@* Код js-роутера, который раздается контроллером, либо включается в верстку соотв.страницы. *@

@import io.suggest.sc.ScConstants.JsRouter._
@import ctx.request
@import views.js.stuff.jsRevRouterTpl

@jsRevRouterTpl(NAME)(
  routes.javascript.Sc.index,
  routes.javascript.Sc.findAds,
  routes.javascript.Sc.findNodes,
  routes.javascript.Sc.focusedAds,
  routes.javascript.Sc.tagsSearch,
  routes.javascript.Sc.handleScError,
  routes.javascript.Static.advRcvrsMap
)

@* Когда скрипт загрузился и выполнился, будет вызвана функция инициализации с указанным именем, если задана. *@
if(typeof window.@ASYNC_INIT_FNAME == 'function') {
  window.@(ASYNC_INIT_FNAME)();
}