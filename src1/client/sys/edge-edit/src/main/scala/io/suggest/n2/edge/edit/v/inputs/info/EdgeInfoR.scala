package io.suggest.n2.edge.edit.v.inputs.info

import com.materialui.{Mui, MuiList, MuiListItem, MuiListItemIcon, MuiListItemText, MuiPaper}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.MEdgeInfo
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.02.2020 14:52
  * Description: Раздел редактирования edge.info-контейнера.
  */
class EdgeInfoR(
                 flagR                : FlagR,
                 textNiR              : TextNiR,
                 extServiceR          : ExtServiceR,
                 osFamilyR            : OsFamilyR,
                 crCtxProv            : React.Context[MCommonReactCtx],
               ) {

  type Props_t = MEdgeInfo
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    def render(p: Props): VdomElement = {
      MuiPaper()(
        MuiList()(

          // Заголовок.
          MuiListItem()(
            MuiListItemIcon()(
              Mui.SvgIcons.Info()(),
            ),
            MuiListItemText()(
              crCtxProv.message( MsgCodes.`Info` ),
            ),
          ),

          // Флаг
          MuiListItem()(
            MuiListItemText()(
              p.wrap( _.flag )( flagR.component.apply ),
            )
          ),

          MuiListItem()(
            MuiListItemText()(
              p.wrap( _.textNi )( textNiR.component.apply ),
            )
          ),

          MuiListItem()(
            MuiListItemText()(
              p.wrap( _.extService )( extServiceR.component.apply ),
            )
          ),

          MuiListItem()(
            MuiListItemText()(
              p.wrap( _.osFamily )( osFamilyR.component.apply ),
            )
          ),

        )
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
