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
  (
    routes.javascript.Sc.pubApi #::
    routes.javascript.RemoteLogs.receive #::
    routes.javascript.Static.advRcvrsMapJson #::

    routes.javascript.Ident.loginFormPage #::
    routes.javascript.Ident.rdrUserSomewhere #::
    routes.javascript.LkAds.adsPage #::
    routes.javascript.LkAdEdit.editAd #::

    routes.javascript.ScApp.appDownloadInfo #::
    routes.javascript.ScApp.iosInstallManifest #::

    jsRoutesUtil.loginRoutes
  ): _*
)

@* Когда скрипт загрузился и выполнился, будет вызвана функция инициализации с указанным именем, если задана. *@
if(typeof window.@ASYNC_INIT_FNAME == 'function') {
  window.@(ASYNC_INIT_FNAME)();
}
