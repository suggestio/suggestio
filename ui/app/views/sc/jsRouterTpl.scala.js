@()(implicit ctx: Context)

@* Код js-роутера, который раздается контроллером, либо включается в верстку соотв.страницы. *@

@import io.suggest.sc.ScConstants.JsRouter._
@import ctx.request

"use strict";

@play.api.routing.JavaScriptReverseRouter(NAME)(
  routes.javascript.Sc.geoShowcase,
  routes.javascript.Sc.showcase,
  routes.javascript.Sc.findAds,
  routes.javascript.Sc.findNodes,
  routes.javascript.Sc.focusedAds,
  routes.javascript.Sc.tagsSearch,
  routes.javascript.Sc.handleScError
);

@* Когда скрипт загрузился и выполнился, будет вызвана функция инициализации с указанным именем, если задана. *@
if(typeof window.@ASYNC_INIT_FNAME == 'function') {
  window.@(ASYNC_INIT_FNAME)();
}
