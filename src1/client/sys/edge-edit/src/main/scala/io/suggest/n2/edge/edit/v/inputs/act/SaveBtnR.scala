package io.suggest.n2.edge.edit.v.inputs.act

import com.materialui.{Mui, MuiCircularProgress, MuiCircularProgressProps, MuiFab, MuiFabClasses, MuiFabProps, MuiFabVariants, MuiProgressVariants}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.Save
import io.suggest.n2.edge.edit.v.EdgeEditCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.2020 8:03
  * Description: Компонент кнопки запуска сохранения.
  */
class SaveBtnR(
                crCtxProv            : React.Context[MCommonReactCtx],
              ) {

  type Props_t = Some[Boolean]
  type Props = ModelProxy[Props_t]


  case class State(
                    isPendingSomeC    : ReactConnectProxy[Some[Boolean]],
                  )

  class Backend( $: BackendScope[Props, State] ) {

    private lazy val _onSaveClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, Save )
    }

    def render(s: State): VdomElement = {
      val _saveMsg = crCtxProv.message( MsgCodes.`Save` )

      val css = new MuiFabClasses {
        override val root = Css.flat( EdgeEditCss.input.htmlClass, EdgeEditCss.w200.htmlClass )
      }

      s.isPendingSomeC { isPendingSomeProxy =>
        val isPending = isPendingSomeProxy.value.value
        MuiFab(
          new MuiFabProps {
            override val variant  = MuiFabVariants.extended
            override val onClick  = _onSaveClick
            override val disabled = isPending
            override val classes  = css
          }
        )(
          // Рендерить иконку или крутилку.
          if (isPending)
            MuiCircularProgress(
              new MuiCircularProgressProps {
                override val variant = MuiProgressVariants.indeterminate
              }
          ) else
            Mui.SvgIcons.Save()(),

          _saveMsg,
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isPendingSomeC = propsProxy.connect( identity(_) ),
      )
    }
    .renderBackend[Backend]
    .build

}
