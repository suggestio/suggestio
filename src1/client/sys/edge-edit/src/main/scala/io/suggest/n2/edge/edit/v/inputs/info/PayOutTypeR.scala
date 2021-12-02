package io.suggest.n2.edge.edit.v.inputs.info

import com.materialui.{MuiFormControlClasses, MuiMenuItem, MuiMenuItemProps, MuiTextField, MuiTextFieldProps}
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants.{`(`, `)`}
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.{EdgeUpdateWithTraverse, MEdgeEditRoot, MEdgeEditS, UpdateWithLens}
import io.suggest.n2.edge.edit.v.EdgeEditCss
import io.suggest.n2.edge.payout.{MEdgePayOut, MEdgePayOutType, MEdgePayoutTypes}
import io.suggest.n2.edge.{MEdge, MEdgeInfo}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.{DAction, OptFastEq}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import monocle.Traversal
import play.api.libs.json.{JsObject, Json}
import io.suggest.ueq.JsUnivEqUtil._

import scala.util.Try

/** Payout mechanism description editing component. */
final class PayOutTypeR(
                         crCtxProv: React.Context[MCommonReactCtx],
                       ) {

  type Props_t = MEdgeEditRoot
  type Props = ModelProxy[Props_t]

  case class State(
                    poTypeOptC        : ReactConnectProxy[Option[MEdgePayOutType]],
                  )


  private def _edge_info_payout_LENS = MEdge.info
    .andThen( MEdgeInfo.payOut )
  private def _edge_info_payout_TRAV = _edge_info_payout_LENS
    .andThen( Traversal.fromTraverse[Option, MEdgePayOut] )


  class Backend($: BackendScope[Props, State]) {

    private val _onPayoutTypeChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      $.props >>= { mrootProxy: Props =>
        val actions = MEdgePayoutTypes
          .withValueOpt( e.target.value )
          .fold [List[DAction]] {
            UpdateWithLens.edge( _edge_info_payout_LENS, None ) :: Nil
          } { poType2 =>
            val mroot = mrootProxy.value
            mroot.edge.info.payOut.fold [List[DAction]] {
              // Try to parse JSON data from previous payout-data editor state.
              val payoutEdit0 = mroot.edit.payoutData
              val newData = payoutEdit0
                .toOption
                .flatMap { payoutDataStr =>
                  Try( Json.parse(payoutDataStr) )
                    .toOption
                }
                .flatMap( _.asOpt[JsObject] )
                .getOrElse( JsObject.empty )

              val payOutDefault = MEdgePayOut( poType2, newData )
              var acc: List[DAction] = UpdateWithLens.edge( _edge_info_payout_LENS, Some(payOutDefault) ) :: Nil

              // Also, initialize editor:
              if (payoutEdit0.isEmpty)
                acc ::= UpdateWithLens( MEdgeEditRoot.edit andThen MEdgeEditS.payoutData, Pot.empty[String].ready(newData.toString()) )

              acc
            } { _ =>
              EdgeUpdateWithTraverse( _edge_info_payout_TRAV andThen MEdgePayOut.poType, poType2 ) :: Nil
            }
          }
        actions
          .iterator
          .map {
            ReactDiodeUtil.dispatchOnProxyScopeCB( $, _ )
          }
          .reduceLeft(_ >> _)
      }
    }


    def render(p: Props, s: State): VdomElement = {
      val emptyKey = ""

      val _paySystemsChildren: List[VdomElement] = {
        MuiMenuItem(
          new MuiMenuItemProps {
            override val value = emptyKey
          }
        )(
          `(`,
          crCtxProv.message( MsgCodes.`empty` ),
          `)`,
        )
      } :: MEdgePayoutTypes
        .values
        .iterator
        .map { poType =>
          MuiMenuItem.component
            .withKey( poType.value )(
              new MuiMenuItemProps {
                override val value = poType.value
              }
            )(
              crCtxProv.message( poType.singularI18n ),
            ): VdomElement
        }
        .toList

      val _label = crCtxProv.message( MsgCodes.`Payout` ): VdomNode

      val _selectCss = new MuiFormControlClasses {
        override val root = Css.flat( EdgeEditCss.inputLeft.htmlClass, EdgeEditCss.w400.htmlClass )
      }

      React.Fragment(

        // PaySystem selector:
        s.poTypeOptC { poTypeOptProxy =>
          val poTypeOpt = poTypeOptProxy.value
          val _value = poTypeOpt.fold( emptyKey )( _.value )
          MuiTextField(
            new MuiTextFieldProps {
              override val select = true
              override val value  = _value
              override val label  = _label.rawNode
              override val onChange = _onPayoutTypeChange
              override val classes = _selectCss
              override val variant = MuiTextField.Variants.standard
            }
          )( _paySystemsChildren: _* )
        },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]
    .initialStateFromProps { propsProxy =>
      State(

        poTypeOptC = propsProxy.connect { mroot =>
          mroot.edge.info.payOut
            .map(_.poType)
        }( OptFastEq.Plain ),

      )
    }
    .renderBackend[Backend]
    .build

}
