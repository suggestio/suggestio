package org.js.react.leaflet.core

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 17:09
  */

trait LeafletElement[T, C <: js.Any] extends js.Object {
  val instance: T
  val context: LeafletContextInterface
  val container: js.UndefOr[C] = js.undefined
}


@js.native
@JSImport(PACKAGE_NAME, "createElementHook")
object createElementHook extends js.Function {

  /** @param updateElement (instance: E, NEXTProps: P, PREVProps: P) => Unit. */
  def apply[E <: js.Object, P <: js.Object, C <: js.Any]
  (
    createElement: js.Function2[P, LeafletContextInterface, LeafletElement[E, C]],
    updateElement: js.Function3[E, P, P, Unit] = js.native,
  ): ElementHookRef[E, C] = js.native

}
