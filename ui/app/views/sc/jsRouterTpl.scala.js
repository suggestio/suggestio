@()(implicit ctx: Context)

@* Код js-роутера, который раздается контроллером, либо включается в верстку соотв.страницы. *@

@import io.suggest.sc.ScConstants._
@import ctx.request

"use strict";

@play.api.routing.JavaScriptReverseRouter(JS_ROUTER_NAME)(
  routes.javascript.MarketShowcase.geoShowcase,
  routes.javascript.MarketShowcase.showcase,
  routes.javascript.MarketShowcase.findAds,
  routes.javascript.MarketShowcase.findNodes,
  routes.javascript.MarketShowcase.focusedAds
);

@* Когда скрипт загрузился и выполнился, будет вызвана функция инициализации с указанным именем, если задана. *@
if(typeof window.@JS_ROUTER_ASYNC_INIT_FNAME == 'function') {
  window.@(JS_ROUTER_ASYNC_INIT_FNAME)();
}
