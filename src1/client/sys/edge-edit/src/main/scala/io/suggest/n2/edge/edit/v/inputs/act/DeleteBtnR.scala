package io.suggest.n2.edge.edit.v.inputs.act

import com.materialui.{Mui, MuiFab, MuiFabClasses, MuiFabProps, MuiFabVariants}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.DeleteEdge
import io.suggest.n2.edge.edit.v.EdgeEditCss
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.01.2020 17:06
  * Description: Кнопка запуска удаления эджа.
  */
class DeleteBtnR(
                  crCtxProv: React.Context[MCommonReactCtx],
                ) {

  type Props_t = Some[Boolean]
  type Props = ModelProxy[Props_t]


  case class State(
                    isEnabledSomeC      : ReactConnectProxy[Props_t],
                  )


  class Backend( $: BackendScope[Props, State] ) {

    private lazy val _onDeleteClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, DeleteEdge(false) )
    }

    def render(s: State): VdomElement = {
      val deleteText = <.span(
        crCtxProv.message( MsgCodes.`Delete` ),
        HtmlConstants.ELLIPSIS,
      )

      val _css = new MuiFabClasses {
        override val root = EdgeEditCss.input.htmlClass
      }

      s.isEnabledSomeC { isEnabledSomeProxy =>
        MuiFab(
          new MuiFabProps {
            override val variant  = MuiFabVariants.extended
            override val onClick  = _onDeleteClick
            override val disabled = !isEnabledSomeProxy.value.value
            override val classes  = _css
          }
        )(
          Mui.SvgIcons.Delete()(),
          deleteText,
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isEnabledSomeC = propsProxy.connect( identity(_) ),
      )
    }
    .renderBackend[Backend]
    .build

}
