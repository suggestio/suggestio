package io.suggest.sjs.common.model.browser.opera.desktop.legacy

import org.scalajs.dom
import org.scalajs.dom.Window

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.06.15 13:39
 * Description: Интерфейс для доступа к DOM-функциям opera.
 */
sealed trait WindowOperaStub extends js.Object {

  def opera: UndefOr[DomOperaPrestoStub] = js.native

}

/** Интерфейс объекта window.opera. */
sealed trait DomOperaPrestoStub extends js.Object {

  /** @return "12.16" */
  def version(): UndefOr[String] = js.native

}


object WindowOperaStub {

  def apply(wnd: Window = dom.window): WindowOperaStub = {
    wnd.asInstanceOf[WindowOperaStub]
  }

}
