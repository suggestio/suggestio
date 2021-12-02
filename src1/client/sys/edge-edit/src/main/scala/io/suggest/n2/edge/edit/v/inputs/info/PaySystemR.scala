package io.suggest.n2.edge.edit.v.inputs.info

import com.materialui.{MuiFormControlClasses, MuiMenuItem, MuiMenuItemProps, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants.{`(`, `)`}
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.{MEdge, MEdgeInfo}
import io.suggest.n2.edge.edit.m.UpdateWithLens
import io.suggest.n2.edge.edit.v.EdgeEditCss
import io.suggest.pay.{MPaySystem, MPaySystems}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/** Payment system select component. */
final class PaySystemR(
                        crCtxProv: React.Context[MCommonReactCtx],
                      ) {

  type Props_t = Option[MPaySystem]
  type Props = ModelProxy[Props_t]

  case class State(
                    paySystemOptC     : ReactConnectProxy[Option[MPaySystem]],
                  )


  class Backend($: BackendScope[Props, State]) {

    private val _onPaySystemChange = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val paySysOpt2 = MPaySystems.withValueOpt( e.target.value )
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, UpdateWithLens.edge( MEdge.info andThen MEdgeInfo.paySystem, paySysOpt2 ) )
    }


    def render(s: State): VdomElement = {
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
      } :: MPaySystems
        .values
        .iterator
        .map { paySys =>
          MuiMenuItem.component
            .withKey( paySys.value )(
              new MuiMenuItemProps {
                override val value = paySys.value
              }
            )(
              crCtxProv.message( paySys.nameI18n ),
            ): VdomElement
        }
        .toList

      val _label = crCtxProv.message( MsgCodes.`Pay.system` ): VdomNode

      val _selectCss = new MuiFormControlClasses {
        override val root = Css.flat( EdgeEditCss.inputLeft.htmlClass, EdgeEditCss.w400.htmlClass )
      }

      React.Fragment(

        // PaySystem selector:
        s.paySystemOptC { paySystemOptProxy =>
          val paySystemOpt = paySystemOptProxy.value
          val _value = paySystemOpt.fold( emptyKey )( _.value )
          MuiTextField(
            new MuiTextFieldProps {
              override val select = true
              override val value  = _value
              override val label  = _label.rawNode
              override val onChange = _onPaySystemChange
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
        paySystemOptC = propsProxy.connect( identity ),
      )
    }
    .renderBackend[Backend]
    .build

}
