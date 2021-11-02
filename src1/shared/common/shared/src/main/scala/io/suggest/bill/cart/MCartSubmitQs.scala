package io.suggest.bill.cart

import io.suggest.common.empty.EmptyProduct
import io.suggest.url.bind.{QsBindable, QsBinderF, QsUnbinderF, QueryStringBindableUtil}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/** URL QueryString arguments container for cartSubmit() server action.
  *
  * @param onNodeId Current node id, currently visited by user (in personal cabinet).
  */
case class MCartSubmitQs(
                          payVia      : MPayableVia,
                          onNodeId    : Option[String] = None,
                        )
  extends EmptyProduct


object MCartSubmitQs {

  @inline implicit def univEq: UnivEq[MCartSubmitQs] = UnivEq.derive

  object Fields {
    final def PAY_VIA = "pay"
    final def ON_NODE_ID = "node"
  }


  /** play-json support for [[MCartSubmitQs]] instances. */
  implicit def cartSubmitArgsJson: OFormat[MCartSubmitQs] = {
    val F = Fields
    (
      (__ \ F.PAY_VIA).format[MPayableVia] and
      (__ \ F.ON_NODE_ID).formatNullable[String]
    )(apply, unlift(unapply))
  }


  /** URL qs binding support for [[MCartSubmitQs]] instances. */
  implicit def cartSubmitArgsQsB(implicit
                                 stringOptB: QsBindable[Option[String]],
                                 payViaB : QsBindable[MPayableVia],
                                ): QsBindable[MCartSubmitQs] = {
    new QsBindable[MCartSubmitQs] {

      override def bindF: QsBinderF[MCartSubmitQs] = { (key, params) =>
        val k = QueryStringBindableUtil.key1F( key )
        val F = Fields
        for {
          payViaE   <- payViaB.bindF( k(F.PAY_VIA), params )
          onNodeIdE <- stringOptB.bindF( k(F.ON_NODE_ID), params )
        } yield {
          for {
            payVia   <- payViaE
            onNodeId <- onNodeIdE
          } yield {
            MCartSubmitQs(
              payVia   = payVia,
              onNodeId = onNodeId,
            )
          }
        }
      }

      override def unbindF: QsUnbinderF[MCartSubmitQs] = { (key, value) =>
        val k = QueryStringBindableUtil.key1F( key )
        val F = Fields
        QueryStringBindableUtil._mergeUnbinded1(
          payViaB.unbindF( k(F.PAY_VIA), value.payVia ),
          stringOptB.unbindF( k(F.ON_NODE_ID), value.onNodeId ),
        )
      }

    }
  }

}
