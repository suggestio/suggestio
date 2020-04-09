package io.suggest.ad.edit.v.edit

import com.materialui.{MuiFormControlClasses, MuiTextField, MuiTextFieldProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.TitleEdit
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.04.2020 17:21
  * Description: Компонент редактирования заголовка карточки.
  */
class TitleR(
              lkAdEditCss       : LkAdEditCss,
            ) {

  type Props_t = Option[String]
  type Props = ModelProxy[Props_t]

  case class State(
                    titleOptC        : ReactConnectProxy[Option[String]],
                  )

  class Backend( $: BackendScope[Props, State] ) {

    private lazy val _onTitleChangeCbF = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, TitleEdit( e.target.value ) )
    }

    def render(s: State): VdomElement = {
      val titleMsg = Messages( MsgCodes.`Title` )

      val _css = new MuiFormControlClasses {
        override val root = lkAdEditCss.Title.titleInp.htmlClass
      }

      // Поле редактирования заголовка.
      s.titleOptC { titleEditProxy =>
        val _title = titleEditProxy.value getOrElse ""
        MuiTextField(
          new MuiTextFieldProps {
            override val `type`     = HtmlConstants.Input.text
            override val value      = _title
            override val onChange   = _onTitleChangeCbF
            override val label      = titleMsg
            override val classes    = _css
          }
        )()
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        titleOptC = propsProxy.connect(identity),
      )
    }
    .renderBackend[Backend]
    .build

}
