package io.suggest.lk.nodes.form.r.pop

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.Css
import io.suggest.lk.m.MDeleteConfirmPopupS
import io.suggest.lk.nodes.form.m.{MCreateNodeS, MEditTfDailyS, MLkNodesRoot}
import io.suggest.lk.r.DeleteConfirmPopupR
import io.suggest.lk.r.popup.PopupsContR
import io.suggest.spa.OptFastEq
import io.suggest.spa.OptFastEq.Wrapped
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 22:26
  * Description: React-компонент всех попапов этой формы. Рендерится параллельно с корневым компонентом формы.
  */
class LknPopupsR(
                  createNodeR       : CreateNodeR,
                  editTfDailyR      : EditTfDailyR,
                  nameEditDiaR      : NameEditDiaR,
                ) {

  import MCreateNodeS.MCreateNodeSFastEq
  import MDeleteConfirmPopupS.MDeleteConfirmPopupSFastEq
  import MEditTfDailyS.MTfDailyEditSFastEq
  import PopupsContR.PopContPropsValFastEq


  type Props = ModelProxy[MLkNodesRoot]


  case class State(
                    popContPropsConn    : ReactConnectProxy[PopupsContR.PropsVal],
                    deleteNodeOptConn   : ReactConnectProxy[Option[MDeleteConfirmPopupS]],
                  )

  class Backend($: BackendScope[Props, State]) {

    def render(propsProxy: Props, state: State): VdomElement = {

      val popups = List[VdomNode](

        // Рендер попапа удаления существующего узла:
        state.deleteNodeOptConn { DeleteConfirmPopupR.component.apply },

      )

      React.Fragment(

        state.popContPropsConn { popContPropsProxy =>
          // Рендер контейнера попапов:
          PopupsContR( popContPropsProxy )(
            popups: _*
          )
        },

        // Попап редактирования названия узла.
        propsProxy.wrap { mroot =>
          for {
            edit0 <- mroot.popups.editName
            loc0 <- mroot.tree.openedLoc
          } yield {
            nameEditDiaR.PropsVal(
              nameOrig = loc0.getLabel.info.name,
              state    = edit0
            )
          }
        }( nameEditDiaR.component.apply )( implicitly, OptFastEq.Wrapped(nameEditDiaR.nameEditPvFeq) ),

        // Рендер попапа создания нового узла:
        propsProxy.wrap(_.popups.createNodeS)( createNodeR.component.apply ),

        // Рендер попапа редактирования тарифа текущего узла.
        propsProxy.wrap( _.popups.editTfDailyS )( editTfDailyR.component.apply ),

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { mrootProxy =>
      val p = mrootProxy.zoom(_.popups)
      State(
        popContPropsConn = {
          // Храним строку css-классов снаружи функции, чтобы избежать ложных отрицательных результатов a.css eq b.css.
          val contCss = Css.Lk.Nodes.LKN
          p.connect { v =>
            PopupsContR.PropsVal(
              visible   = v.deleteNodeS.nonEmpty,
              css       = contCss
            )
          }
        },
        deleteNodeOptConn  = p.connect(_.deleteNodeS),
      )
    }
    .renderBackend[Backend]
    .build

}
