package io.suggest.id.login

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.id.login.m.LoginShowHide
import io.suggest.init.routed.InitRouter
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.doc.DocumentVm
import io.suggest.ReactCommonModule
import io.suggest.i18n.MCommonReactCtx
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 18:54
  * Description: Аддон для init-router'а для инициализации формы логина.
  */
trait LoginFormInit extends InitRouter {

  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    println("route init: " + itg)
    if (itg ==* MJsInitTargets.LoginForm) {
      _initLoginForm()
    } else {
      super.routeInitTarget(itg)
    }
  }


  /** Выполнение инициализации формы логина. */
  private def _initLoginForm(): Unit = {
    println("login")
    val modules = new LoginFormModule

    // Форма - это диалог. рендерить её можно в body куда угодно. Поэтому создаём div в конце body.
    val contDiv = VUtil.newDiv()

    // Отрендерить форму в контейнер:
    // TODO Тут очень статический контекст, делающий невозможным переключать язык /id.
    // Когда будет механизм переключения языка, надо законнектить через circuit.connect().
    val loginForm = ReactCommonModule.commonReactCtx.provide( MCommonReactCtx.default )(
      modules.circuit
        .wrap( identity(_) )( modules.loginFormR.apply )
    )
    loginForm.renderIntoDOM( contDiv )

    DocumentVm()
      .body
      .appendChild( contDiv )

    // Показать диалог через посыл соотв.экшена.
    Future {
      modules.circuit.dispatch( LoginShowHide(true) )
      println("login show = true")
    }
    println("login done")
  }

}
