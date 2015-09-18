@()(implicit ctx: Context)

@* Код js-роутера, который раздается контроллером, либо включается в верстку соотв.страницы. *@

@import io.suggest.sc.ScConstants.JsRouter._
@import ctx.request

"use strict";

@play.api.routing.JavaScriptReverseRouter(NAME)(
  routes.javascript.MarketShowcase.geoShowcase,
  routes.javascript.MarketShowcase.showcase,
  routes.javascript.MarketShowcase.findAds,
  routes.javascript.MarketShowcase.findNodes,
  routes.javascript.MarketShowcase.focusedAds,
  routes.javascript.MarketShowcase.tagsSearch
);

@* Когда скрипт загрузился и выполнился, будет вызвана функция инициализации с указанным именем, если задана. *@
if(typeof window.@ASYNC_INIT_FNAME == 'function') {
  window.@(ASYNC_INIT_FNAME)();
}
