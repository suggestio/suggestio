package io.suggest.lk.tags.edit.r

import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiTextField, MuiTextFieldProps}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.common.tags.edit.MTagsEditQueryProps
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.tags.edit.m.{AddCurrentTag, SetTagSearchQuery}
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import org.scalajs.dom.ext.KeyCode
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 18:41
  * Description: Компонент инпута названия тега для поиска/создания оного.
  */
final class TagNameInpR(
                         crCtxP: React.Context[MCommonReactCtx],
                       ) {

  type Props = ModelProxy[MTagsEditQueryProps]

  class Backend($: BackendScope[Props, Unit]) {

    /**
      * Коллбэк ввода текста в поле имени нового тега.
      * Надо обновить состояние и запустить поисковый запрос, если требуется.
      */
    private def onQueryChange(e: ReactEventFromInput): Callback = {
      val queryStr2 = e.target.value
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SetTagSearchQuery(queryStr2) )
    }
    private val _onTextChangeCb = ReactCommonUtil.cbFun1ToJsCb { e: ReactEventFromInput =>
      onQueryChange(e)
    }

    private def _onAddClick: Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, AddCurrentTag )


    /** Коллбек для реакции на нажатие некоторых особых клавиш на клавиатуре во время ввода. */
    private def onKeyUp(e: ReactKeyboardEvent): Callback = {
      if (e.keyCode ==* KeyCode.Enter) {
        _onAddClick
      } else {
        Callback.empty
      }
    }
    private val _onKeyUpCb = ReactCommonUtil.cbFun1ToJsCb { e: ReactKeyboardEvent =>
      onKeyUp(e)
    }

    private val _onAddClickCb = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      _onAddClick
    }


    def render(p: Props): VdomElement = {
      // Пытаемся дизайнить в стиле второй части примера https://material-ui.com/components/text-fields/#customized-inputs
      <.div(

        MuiTextField(
          new MuiTextFieldProps {
            override val `type` = HtmlConstants.Input.text
            override val label = crCtxP.message( MsgCodes.`Add.tags` ).rawElement
            override val onChange = _onTextChangeCb
            override val onKeyUp = _onKeyUpCb
            override val value = p.value.text
          }
        )(),

        MuiIconButton(
          new MuiIconButtonProps {
            override val onClick = _onAddClickCb
          }
        )(
          Mui.SvgIcons.Add()()
        ),

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
