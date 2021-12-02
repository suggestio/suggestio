package io.suggest.n2.edge.edit.v.inputs.info

import com.materialui.{Mui, MuiList, MuiListItem, MuiListItemIcon, MuiListItemProps, MuiListItemText, MuiPaper, MuiSx}
import diode.react.ModelProxy
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.n2.edge.edit.m.MEdgeEditRoot
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

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
                 paySystemR           : PaySystemR,
                 payOutTypeR          : PayOutTypeR,
                 payOutDataR          : PayOutDataR,
                 crCtxProv            : React.Context[MCommonReactCtx],
               ) {

  type Props_t = MEdgeEditRoot
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    def render(mrootProxy: Props): VdomElement = {
      val p = mrootProxy.zoom(_.edge.info)

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

          MuiListItem()(
            MuiListItemText()(
              p.wrap( _.paySystem )( paySystemR.component.apply ),
            )
          ),

          MuiListItem(
            new MuiListItemProps {
              override val sx = new MuiSx {
                override val flexDirection = js.defined( "column" )
              }
            }
          )(
            payOutTypeR.component( mrootProxy ),
            payOutDataR.component( mrootProxy ),
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
