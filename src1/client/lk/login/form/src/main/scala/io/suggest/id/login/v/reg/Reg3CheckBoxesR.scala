package io.suggest.id.login.v.reg

import com.materialui.{MuiFormControlLabel, MuiFormControlLabelProps, MuiFormGroup, MuiFormGroupProps}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.id.login.m.{RegPdnSetAccepted, RegTosSetAccepted}
import io.suggest.id.login.m.reg.step3.MReg3CheckBoxes
import io.suggest.id.login.v.stuff.CheckBoxR
import io.suggest.react.ReactCommonUtil
import io.suggest.routes.routes
import japgolly.scalajs.react.{BackendScope, React, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 18:52
  * Description: Шаг финальных галочек и окончания регистрации.
  */
class Reg3CheckBoxesR(
                       checkBoxR           : CheckBoxR,
                       crCtx               : React.Context[MCommonReactCtx],
                     ) {

  type Props = ModelProxy[MReg3CheckBoxes]


  class Backend( $: BackendScope[Props, Unit] ) {

    def render(p: Props): VdomElement = {
      MuiFormGroup(
        new MuiFormGroupProps {
          override val row = true
        }
      )(

        // Галочка согласия с офертой suggest.io:
        MuiFormControlLabel {
          val acceptTosText = crCtx.consume { crCtx =>
            <.span(
              crCtx.messages( MsgCodes.`I.accept` ),
              HtmlConstants.SPACE,
              <.a(
                ^.href := routes.controllers.Static.privacyPolicy().url,
                ^.target.blank,
                ^.onClick ==> ReactCommonUtil.stopPropagationCB,
                crCtx.messages( MsgCodes.`terms.of.service` ),
              )
            )
          }
          val cbx = p.wrap { props =>
            checkBoxR.PropsVal(
              checked   = props.tos.isChecked,
              onChange  = RegTosSetAccepted,
            )
          }(checkBoxR.apply)(implicitly, checkBoxR.CheckBoxRFastEq)
          new MuiFormControlLabelProps {
            override val control = cbx.rawElement
            override val label   = acceptTosText.rawNode
          }
        },

        // Галочка разрешения на обработку ПДн:
        MuiFormControlLabel {
          val pdnText = crCtx.message( MsgCodes.`I.allow.personal.data.processing` )

          val cbx = p.wrap { props =>
            checkBoxR.PropsVal(
              checked   = props.pdn.isChecked,
              onChange  = RegPdnSetAccepted,
            )
          }(checkBoxR.apply)(implicitly, checkBoxR.CheckBoxRFastEq)
          new MuiFormControlLabelProps {
            override val control = cbx.rawElement
            override val label   = pdnText.rawNode
          }
        },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .renderBackend[Backend]
    .build

}
