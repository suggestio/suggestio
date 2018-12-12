package io.suggest.sys.mdr

import io.suggest.init.routed.InitRouter
import japgolly.univeq._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.view.VUtil
import japgolly.scalajs.react.vdom.Implicits._
import org.scalajs.dom.html
import io.suggest.sys.mdr.m.MSysMdrRootS.MSysMdrRootSFastEq

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.18 12:11
  * Description: Трейт для инициализации
  */
trait SysMdrInit extends InitRouter {

  override protected def routeInitTarget(itg: MJsInitTarget): Unit = {
    if (itg ==* MJsInitTargets.SysMdrForm) {
      Future {
        _initSysMdr()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }


  /** Метод-инициализацитор sys-mdr-формы. */
  private def _initSysMdr(): Unit = {
    val modules = new SysMdrModules

    // Выполнить инициализацию circuit'а:
    val circuit = modules.sysMdrCircuit

    // Линковка circuit - react views - DOM:
    val domContainer = VUtil.getElementById[html.Div]( SysMdrConst.FORM_ID ).get
    val reactForm = circuit.wrap( identity(_) )( modules.sysMdrFormR.apply )
    reactForm.renderIntoDOM( domContainer )
  }

}
