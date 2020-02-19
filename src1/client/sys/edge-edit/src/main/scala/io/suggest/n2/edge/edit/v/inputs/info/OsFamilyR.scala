package io.suggest.n2.edge.edit.v.inputs.info

import com.materialui.{MuiFormControlClasses, MuiMenuItem, MuiMenuItemProps, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants.{`(`, `)`}
import io.suggest.css.Css
import io.suggest.dev.{MOsFamilies, MOsFamily, OsFamiliesR}
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.OsFamilySet
import io.suggest.n2.edge.edit.v.EdgeEditCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.02.2020 2:16
  * Description: Селектор выбора операционной системы.
  */
class OsFamilyR(
                 osFamiliesR    : OsFamiliesR,
                 crCtxProv      : React.Context[MCommonReactCtx],
               ) {

  type Props_t = Option[MOsFamily]
  type Props = ModelProxy[Props_t]

  case class State(
                    osFamilyOptC      : ReactConnectProxy[Props_t],
                  )

  class Backend($: BackendScope[Props, State]) {

    private val _onOsChanged = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      val osf = MOsFamilies.withValueOpt( e.target.value )
      ReactDiodeUtil.dispatchOnProxyScopeCB($, OsFamilySet(osf))
    }

    def render(s: State): VdomElement = {
      val emptyKey = ""
      val _children: List[VdomElement] = {
        MuiMenuItem(
          new MuiMenuItemProps {
            override val value = emptyKey
          }
        )(
          `(`,
          crCtxProv.message( MsgCodes.`empty` ),
          `)`,
        )
      } :: osFamiliesR.osFamiliesMenuItems

      val _label = crCtxProv.message( MsgCodes.`Operating.system.family` ): VdomNode

      val _css = new MuiFormControlClasses {
        override val root = Css.flat( EdgeEditCss.inputLeft.htmlClass, EdgeEditCss.w400.htmlClass )
      }

      s.osFamilyOptC { osFamilyOptProxy =>
        val _value = osFamilyOptProxy.value.fold( emptyKey )( _.value )
        MuiTextField(
          new MuiTextFieldProps {
            override val select = true
            override val value  = _value
            override val label  = _label.rawNode
            override val onChange = _onOsChanged
            override val classes = _css
          }
        )( _children: _* )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        osFamilyOptC = propsProxy.connect(identity),
      )
    }
    .renderBackend[Backend]
    .build

}
