package io.suggest.lk.nodes.form.r.pop

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.Css
import io.suggest.lk.m.MDeleteConfirmPopupS
import io.suggest.lk.nodes.form.m.{MCreateNodeS, MEditTfDailyS, MLknPopups}
import io.suggest.lk.r.DeleteConfirmPopupR
import io.suggest.lk.r.popup.PopupsContR
import io.suggest.spa.OptFastEq.Wrapped
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.{VdomElement, VdomNode}
import japgolly.scalajs.react.vdom.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 22:26
  * Description: React-компонент всех попапов этой формы. Рендерится параллельно с корневым компонентом формы.
  */
class LknPopupsR(
                  val createNodeR   : CreateNodeR,
                  val editTfDailyR  : EditTfDailyR
                ) {

  import MCreateNodeS.MCreateNodeSFastEq
  import MDeleteConfirmPopupS.MDeleteConfirmPopupSFastEq
  import MEditTfDailyS.MTfDailyEditSFastEq
  import PopupsContR.PopContPropsValFastEq


  type Props = ModelProxy[MLknPopups]


  case class State(
                    popContPropsConn    : ReactConnectProxy[PopupsContR.PropsVal],
                    createNodeOptConn   : ReactConnectProxy[Option[MCreateNodeS]],
                    deleteNodeOptConn   : ReactConnectProxy[Option[MDeleteConfirmPopupS]],
                    editTfDailyOptConn  : ReactConnectProxy[Option[MEditTfDailyS]]
                  )

  class Backend($: BackendScope[Props, State]) {

    def render(propsProxy: Props, state: State): VdomElement = {

      val popups = List[VdomNode](
        // Рендер попапа создания нового узла:
        state.createNodeOptConn { createNodeR.apply },

        // Рендер попапа удаления существующего узла:
        state.deleteNodeOptConn { DeleteConfirmPopupR.apply },

        // Рендер попапа редактирования тарифа текущего узла.
        state.editTfDailyOptConn { editTfDailyR.apply }
      )

      state.popContPropsConn { popContPropsProxy =>
        // Рендер контейнера попапов:
        PopupsContR( popContPropsProxy )(
          popups: _*
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { p =>
      State(
        popContPropsConn = {
          // Храним строку css-классов снаружи функции, чтобы избежать ложных отрицательных результатов a.css eq b.css.
          val contCss = Css.Lk.Nodes.LKN
          p.connect { v =>
            PopupsContR.PropsVal(
              visible   = v.nonEmpty,
              css       = contCss
            )
          }
        },
        createNodeOptConn  = p.connect(_.createNodeS),
        deleteNodeOptConn  = p.connect(_.deleteNodeS),
        editTfDailyOptConn = p.connect(_.editTfDailyS)
      )
    }
    .renderBackend[Backend]
    .build


  def apply(props: Props) = component(props)

}
