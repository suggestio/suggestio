package io.suggest.id.login

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.id.login.m.LoginShowHide
import io.suggest.init.routed.InitRouter
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.doc.DocumentVm
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
    if (itg ==* MJsInitTargets.LoginForm) {
      _initLoginForm()
    } else if (itg ==* MJsInitTargets.PwChange) {
      _initPwChangeForm()
    } else {
      super.routeInitTarget(itg)
    }
  }


  /** Выполнение инициализации формы логина. */
  private def _initLoginForm(): Unit = {
    val modules = new LoginFormModule

    // Форма - это диалог. рендерить её можно в body куда угодно. Поэтому создаём div в конце body.
    val contDiv = VUtil.newDiv()

    // Отрендерить форму в контейнер
    modules.loginFormSpaRouter
      .router()
      .renderIntoDOM( contDiv )

    DocumentVm()
      .body
      .appendChild( contDiv )

    // Показать диалог через посыл соотв.экшена.
    Future {
      modules.loginFormSpaRouter.circuit.dispatch( LoginShowHide(true) )
    }
  }


  /** Инициализация формы смены пароля. */
  private def _initPwChangeForm(): Unit = {
    val modules = new LoginFormModule

    // Форма - это диалог. рендерить её можно в body куда угодно. Поэтому создаём div в конце body.
    val contDiv = VUtil.newDiv()

    // Отрендерить форму в контейнер
    modules.pwChangeCircuit
      .wrap( modules.pwChangeCircuit.rootRO )( modules.pwChangeR.component.apply )
      .renderIntoDOM( contDiv )

    DocumentVm()
      .body
      .appendChild( contDiv )
  }

}
