package io.suggest.bill.cart

import io.suggest.pay.MPaySystem
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.ueq.UnivEqUtil._

/** Billing cart submission result container.
  * Server can redirect user somewhere or start some js-side pay-system effects.
  * @param cartIdea Cart action on server.
  */
final case class MCartSubmitResult(
                                    cartIdea      : MCartIdea,
                                    pay           : Option[MCartPayInfo]  = None,
                                  )
object MCartSubmitResult {

  @inline implicit def univEq: UnivEq[MCartSubmitResult] = UnivEq.derive

  implicit def cartSubmitResultJson: OFormat[MCartSubmitResult] = {
    (
      (__ \ "cart_idea").format[MCartIdea] and
      (__ \ "pay").formatNullable[MCartPayInfo]
    )(apply, unlift(unapply))
  }

}


/** Payment system init.data container.
  * @param paySystem Payment system, if need to pay (cartIdea == NeedMoney).
  * @param metadata Payment system metadata JSON (options, settings, props, etc) - depends on implementation.
  */
final case class MCartPayInfo(
                               paySystem    : MPaySystem,
                               metadata     : Option[JsObject]    = None,
                             )
object MCartPayInfo {

  @inline implicit def univEq: UnivEq[MCartPayInfo] = UnivEq.derive

  implicit def cartPayInfo: OFormat[MCartPayInfo] = {
    (
      (__ \ "pay_system").format[MPaySystem] and
      (__ \ "metadata").formatNullable[JsObject]
    )(apply, unlift(unapply))
  }

}