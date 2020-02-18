@()(implicit ctx: Context)

@* Код js-роутера, который раздается контроллером, либо включается в верстку соотв.страницы. *@

@import io.suggest.sc.ScConstants.JsRouter._
@import ctx.request
@import views.js.stuff.jsRevRouterTpl

@jsRevRouterTpl(NAME, cdn = true)(
  routes.javascript.Sc.pubApi,
  routes.javascript.Sc.handleScError,
  routes.javascript.Static.advRcvrsMapJson,

  routes.javascript.Ident.loginFormPage,
  routes.javascript.Ident.rdrUserSomewhere,
  routes.javascript.LkAds.adsPage,
  routes.javascript.LkAdEdit.editAd,

  routes.javascript.ScApp.appDownloadInfo,
  routes.javascript.ScApp.iosInstallManifest,
)

@* Когда скрипт загрузился и выполнился, будет вызвана функция инициализации с указанным именем, если задана. *@
if(typeof window.@ASYNC_INIT_FNAME == 'function') {
  window.@(ASYNC_INIT_FNAME)();
}
