package io.suggest.id.login.v.reg

import chandu0101.scalajs.react.components.materialui.MuiPaper
import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.id.login.m.{RegAccept, RegPdnSetAccepted, RegTosSetAccepted}
import io.suggest.id.login.m.reg.MRegFinishS
import io.suggest.id.login.v.stuff.{ButtonR, CheckBoxR}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.19 17:59
  * Description: Компонент страницы подтверждения регистрации нового юзера.
  * Юзер прошёл гос.услуги, и в первый раз вернулся в suggest.io.
  */
class RegFinishR(
                  buttonR         : ButtonR,
                  checkBoxR       : CheckBoxR,
                ) {

  type Props_t = MRegFinishS
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    def render(p: Props): VdomElement = {
      MuiPaper()(

        // TODO Текст соглашения в виде jd-рендера.

        // Галочка согласия с офертой suggest.io:
        p.wrap { props =>
          checkBoxR.PropsVal(
            checked   = props.tos.isChecked,
            msgCode   = MsgCodes.`I.accept.terms.of.service`,
            onChange  = RegTosSetAccepted,
          )
        }(checkBoxR.apply)(implicitly, checkBoxR.CheckBoxRFastEq),

        // Галочка разрешения на обработку ПДн:
        p.wrap { props =>
          checkBoxR.PropsVal(
            checked   = props.pdn.isChecked,
            msgCode   = MsgCodes.`I.allow.personal.data.processing`,
            onChange  = RegPdnSetAccepted,
          )
        }(checkBoxR.apply)(implicitly, checkBoxR.CheckBoxRFastEq),

        // И кнопка завершения регистрации:
        p.wrap { props =>
          buttonR.PropsVal(
            disabled = !(props.pdn.isChecked && props.tos.isChecked),
            onClick  = RegAccept,
            msgCode  = MsgCodes.`_to.Finish`,
          )
        }( buttonR.apply )(implicitly, buttonR.ButtonRPropsValFastEq),

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsProxy: Props ) = component( propsProxy )

}
