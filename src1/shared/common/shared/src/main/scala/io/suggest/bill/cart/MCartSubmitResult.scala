package io.suggest.bill.cart

import io.suggest.pay.MPaySystem
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

/** Billing cart submission result container.
  * Server can redirect user somewhere or start some js-side pay-system effects.
  * @param cartIdea Cart action on server.
  * @param paySystem Payment system, if need to pay (cartIdea == NeedMoney).
  * @param metadata Additional data for client-side payment-system init.
  */
final case class MCartSubmitResult(
                                    cartIdea      : MCartIdea,
                                    paySystem     : Option[MPaySystem]  = None,
                                    metadata      : Option[JsObject]    = None,
                                  )


object MCartSubmitResult {

  @inline implicit def univEq: UnivEq[MCartSubmitResult] = UnivEq.derive

  implicit def cartSubmitResultJson: OFormat[MCartSubmitResult] = {
    (
      (__ \ "cart_idea").format[MCartIdea] and
      (__ \ "pay_system").formatNullable[MPaySystem] and
      (__ \ "metadata").formatNullable[JsObject]
    )(apply, unlift(unapply))
  }

}
