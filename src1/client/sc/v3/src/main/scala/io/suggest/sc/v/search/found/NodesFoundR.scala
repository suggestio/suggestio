package io.suggest.sc.v.search.found

import com.materialui.{MuiToolBar, MuiToolBarProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.MCommonReactCtx
import io.suggest.sc.m.MScReactCtx
import io.suggest.sc.m.inx.MScIndex
import io.suggest.sc.m.search.{MNodesFoundRowProps, MScSearchText}
import io.suggest.sc.v.hdr.RightR
import io.suggest.sc.v.search.STextR
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.06.2020 12:03
  * Description: Компонент поиска узлов.
  */
final class NodesFoundR(
                         rightR                   : RightR,
                         sTextR                   : STextR,
                         nodesSearchContR         : NodesSearchContR,
                         nfListR                  : NfListR,
                         scReactCtxP              : React.Context[MScReactCtx],
                         crCtxP                   : React.Context[MCommonReactCtx],
                       ) {

  type Props_t = MScIndex
  type Props = ModelProxy[Props_t]


  case class State(
                    nodesFoundRowsC               : ReactConnectProxy[Seq[MNodesFoundRowProps]],
                  )

  class Backend($: BackendScope[Props, State]) {

    def render(propsProxy: Props, s: State): VdomElement = {
      // Наполнение контейнера поиска узлов:
      val nodeSearchInner = <.div(
        // Поисковое текстовое поле:
        MuiToolBar(
          new MuiToolBarProps {
            override val disableGutters = true
          }
        )(
          // Элементы строки поиска:
          propsProxy.wrap(_.search.text)( sTextR.component.apply )(implicitly, MScSearchText.MScSearchTextFastEq),

          // Кнопка сворачивания:
          propsProxy.wrap(_ => None)( rightR.apply ),
        ),

        // Панель поиска: контент, зависимый от корневой модели:
        {
          val nodesRows = scReactCtxP.consume { scReactCtx =>
            crCtxP.consume { crCtx =>
              val nfRowR = NfRowR( crCtx, scReactCtx )
              s.nodesFoundRowsC( nfRowR.rows )
            }
          }
          propsProxy.wrap( _.search.geo.found )( nfListR.component(_)( nodesRows ) )
        },
      )

      // Нода с единым скроллингом, передаваемая в children для SearchR:
      propsProxy.wrap( _.search.geo.css )( nodesSearchContR.component(_)( nodeSearchInner ) )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        nodesFoundRowsC = propsProxy.connect { mroot =>
          mroot.searchGeoNodesFoundProps
        }( FastEqUtil.CollFastEq(MNodesFoundRowProps.MNodesFoundRowPropsFeq) ),

      )
    }
    .renderBackend[Backend]
    .build

}
