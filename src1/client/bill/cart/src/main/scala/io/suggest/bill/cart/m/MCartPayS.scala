package io.suggest.bill.cart.m

import diode.data.Pot
import io.suggest.bill.cart.MCartSubmitResult
import io.suggest.pay.MPaySystem
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.ueq.JsUnivEqUtil._

object MCartPayS {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MCartPayS] = UnivEq.derive

  def cartSubmit = GenLens[MCartPayS]( _.cartSubmit )
  def paySystemInit = GenLens[MCartPayS]( _.paySystemInit )

}


/** State container for payments subsystems.
  *
  * @param cartSubmit First request: preparing for pay using s.io server.
  * @param paySystemInit Client-side PaySystem widget init steps.
  *                      Ready+Pending => js-script tag added, waiting for loading.
  *                      Ready => add pay-system react-component (with js-code related) into vdom.
  *                      Error => remove script tag and render error.
  */
case class MCartPayS(
                      cartSubmit          : Pot[MCartSubmitResult]       = Pot.empty,
                      paySystemInit       : Pot[MPaySystem]              = Pot.empty,
                    )
