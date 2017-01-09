package io.suggest.sjs.common.vm.wnd.hist

import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.{History, Window}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.06.15 14:55
 * Description: Безопасный доступ к window.history API.
 */

trait HistoryApiT extends IVm {

  override type T <: Window

  def history: Option[HistoryObj] = {
    WndHistoryStub(_underlying)
      .history
      .toOption
      .map { HistoryObj.apply }
  }

}

@js.native
sealed trait WndHistoryStub extends js.Object {
  def history: UndefOr[History] = js.native
}

object WndHistoryStub {
  def apply(wnd: Window): WndHistoryStub = {
    wnd.asInstanceOf[WndHistoryStub]
  }
}


