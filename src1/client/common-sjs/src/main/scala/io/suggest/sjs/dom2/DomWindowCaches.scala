package io.suggest.sjs.dom2

import org.scalajs.dom.Window
import org.scalajs.dom.experimental.serviceworkers.CacheStorage

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.18 11:13
  * Description: Sjs-интерфейсы для dom.window.caches .
  */

@js.native
trait DomWindowCaches extends js.Object {

  @JSName("caches")
  val cachesOrUndef: js.UndefOr[js.Function] = js.native

  def caches: CacheStorage = js.native

}

object DomWindowCaches {
  implicit def apply( window: Window ): DomWindowCaches =
    window.asInstanceOf[DomWindowCaches]
}

