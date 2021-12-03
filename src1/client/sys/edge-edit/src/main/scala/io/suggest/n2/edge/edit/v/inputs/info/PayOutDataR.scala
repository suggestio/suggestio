package io.suggest.n2.edge.edit.v.inputs.info

import com.materialui.{MuiSx, MuiTextField, MuiTextFieldProps}
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.{MEdge, MEdgeInfo}
import io.suggest.n2.edge.edit.m.{EdgeUpdateWithTraverse, MEdgeEditRoot, MEdgeEditS, UpdateWithLens}
import io.suggest.n2.edge.payout.MEdgePayOut
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.spa.DAction
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.Traversal
import play.api.libs.json.{JsObject, Json}
import io.suggest.ueq.UnivEqUtil._

import scala.scalajs.js
import scala.util.Try


final class PayOutDataR(
                         crCtxProv: React.Context[MCommonReactCtx],
                       ) {

  type Props_t = MEdgeEditRoot
  type Props = ModelProxy[Props_t]


  case class State(
                    dataStrPotC       : ReactConnectProxy[Pot[String]],
                  )



  class Backend( $: BackendScope[Props, State] ) {

    private val _onChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val newValue = e.target.value

      var newValuePot = Pot.empty[String].ready( newValue )
      var actionsAcc = List.empty[DAction]

      Try( Json.parse( newValue ) )
        .flatMap( json => Try( json.as[JsObject] ) )
        .fold(
          {_ =>
            newValuePot = newValuePot.pending()
          },
          {jsObject =>
            val trav = MEdge.info
              .andThen( MEdgeInfo.payOut )
              .andThen( Traversal.fromTraverse[Option, MEdgePayOut] )
              .andThen( MEdgePayOut.data )
            val action = EdgeUpdateWithTraverse( trav, jsObject )
            actionsAcc ::= action
          }
        )

      actionsAcc ::= UpdateWithLens( MEdgeEditRoot.edit andThen MEdgeEditS.payoutData, newValuePot )

      actionsAcc
        .iterator
        .map {
          ReactDiodeUtil.dispatchOnProxyScopeCB( $, _ )
        }
        .reduce(_ >> _)
    }


    def render(s: State): VdomElement = {
      lazy val _label = crCtxProv.message( MsgCodes.`Metadata` ): VdomElement
      lazy val _textAreaSx = new MuiSx {
        override val marginTop = js.defined( "18px" )
        override val width = js.defined( "100%" )
      }

      s.dataStrPotC { dataStrPotProxy =>
        val dataStrPot = dataStrPotProxy.value
        dataStrPot.toOption.whenDefinedEl { dataStr =>
          MuiTextField.component(
            new MuiTextFieldProps {
              override val multiline = true
              override val value = dataStr
              override val label = _label.rawNode
              override val onChange = _onChange
              override val required = true
              override val placeholder = "JSON object"
              override val sx = _textAreaSx
              override val error = dataStrPot.isFailed || dataStrPot.isPending
            }
          )()
        }
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]
    .initialStateFromProps { rootProxy =>
      // To preserve reference-equality, keeping default empty string instance outside connect functions:
      State(

        dataStrPotC = rootProxy.connect { mroot =>
          mroot.edit.payoutData
            .filter( _ => mroot.edge.info.payOut.nonEmpty )
        },

      )
    }
    .renderBackend[Backend]
    .build

}
