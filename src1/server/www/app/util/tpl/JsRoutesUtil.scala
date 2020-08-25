package util.tpl

import controllers.routes
import play.api.routing.JavaScriptReverseRoute

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.08.2020 16:46
  * Description: Сюда скидывается различная утиль для сборки шаблонных js-роутеров.
  */
final class JsRoutesUtil {

  /** JS-роуты для сборки роутеров под логин-форму.
    * Вынесено из шаблона, т.к. с некоторых пор роуты нужны ещё в выдаче. */
  def loginRoutes(): LazyList[JavaScriptReverseRoute] = {
    routes.javascript.Ident.epw2LoginSubmit #::
    routes.javascript.Ident.idViaProvider #::
    routes.javascript.Ident.loginFormPage #::
    routes.javascript.Ident.regStep0Submit #::
    routes.javascript.Ident.epw2RegSubmit #::
    routes.javascript.Ident.smsCodeCheck #::
    routes.javascript.Ident.regFinalSubmit #::
    routes.javascript.Captcha.getCaptcha #::
    routes.javascript.Static.privacyPolicy #::
    LazyList.empty
  }


  /** JS-роуты формы управления узлами, пошаренные с выдачей. */
  def lkNodesFormRoutes(): LazyList[JavaScriptReverseRoute] = {
    routes.javascript.LkNodes.subTree #::
    routes.javascript.LkNodes.createSubNodeSubmit #::
    routes.javascript.LkNodes.setNodeEnabled #::
    routes.javascript.LkNodes.deleteNode #::
    routes.javascript.LkNodes.editNode #::
    routes.javascript.LkNodes.setAdv #::
    routes.javascript.LkNodes.setTfDaily #::
    routes.javascript.LkNodes.setAdvShowOpened #::
    routes.javascript.LkNodes.setAlwaysOutlined #::
    routes.javascript.LkAds.adsPage #::
    controllers.sc.routes.javascript.ScSite.geoSite #::
    LazyList.empty
  }

}
