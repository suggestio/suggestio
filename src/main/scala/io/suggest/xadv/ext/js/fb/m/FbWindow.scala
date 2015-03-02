package io.suggest.xadv.ext.js.fb.m

import org.scalajs.dom.raw.Window

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.03.15 17:45
 * Description: Дополнительные поля dom window для нужд facebook.
 */

class FbWindow extends Window {
  var fbAsyncInit: js.Function0[_] = js.native
}


object FbWindow {

  implicit def w2fbw(w: Window): FbWindow = {
    w.asInstanceOf[FbWindow]
  }

}
