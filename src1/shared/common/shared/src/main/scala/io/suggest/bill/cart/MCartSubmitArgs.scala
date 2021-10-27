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
case class MCartSubmitArgs(
                            onNodeId: Option[String] = None,
                          )
  extends EmptyProduct


object MCartSubmitArgs {

  def empty = apply()

  object Fields {
    final def ON_NODE_ID = "node"
  }

  @inline implicit def univEq: UnivEq[MCartSubmitArgs] = UnivEq.derive

  /** play-json support for [[MCartSubmitArgs]] instances. */
  implicit def cartSubmitArgsJson: OFormat[MCartSubmitArgs] = {
    val F = Fields
    (__ \ F.ON_NODE_ID)
      .formatNullable[String]
      .inmap[MCartSubmitArgs]( apply, _.onNodeId )
  }

  /** URL qs binding support for [[MCartSubmitArgs]] instances. */
  implicit def cartSubmitArgsQsB(implicit stringOptB: QsBindable[Option[String]]): QsBindable[MCartSubmitArgs] = {
    new QsBindable[MCartSubmitArgs] {

      override def bindF: QsBinderF[MCartSubmitArgs] = { (key, params) =>
        val k = QueryStringBindableUtil.key1F( key )
        val F = Fields
        for {
          onNodeIdE <- stringOptB.bindF( k(F.ON_NODE_ID), params )
        } yield {
          for {
            onNodeId <- onNodeIdE
          } yield {
            MCartSubmitArgs(
              onNodeId = onNodeId,
            )
          }
        }
      }

      override def unbindF: QsUnbinderF[MCartSubmitArgs] = { (key, value) =>
        val k = QueryStringBindableUtil.key1F( key )
        val F = Fields
        stringOptB.unbindF( k(F.ON_NODE_ID), value.onNodeId )
      }

    }
  }

}
