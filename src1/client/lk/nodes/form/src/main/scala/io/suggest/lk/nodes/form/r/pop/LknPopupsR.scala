package io.suggest.lk.nodes.form.r.pop

import diode.react.ModelProxy
import io.suggest.lk.nodes.form.m.MLkNodesRoot
import io.suggest.lk.r.DeleteConfirmPopupR
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
                  deleteConfirmPopupR: DeleteConfirmPopupR,
                ) {

  type Props = ModelProxy[MLkNodesRoot]


  class Backend($: BackendScope[Props, Unit]) {

    def render(propsProxy: Props): VdomElement = {
      val popupsProxy = propsProxy.zoom(_.popups)
      React.Fragment(

        // Попап редактирования названия узла.
        nameEditDiaR.component( propsProxy ),

        // Рендер попапа создания нового узла:
        propsProxy.wrap { mroot =>
          createNodeR.PropsVal(
            create = mroot.popups.createNodeS,
            tree   = mroot.tree.tree.idsTree,
            nodesMap = mroot.tree.tree.nodesMap,
          )
        }( createNodeR.component.apply ),

        // Рендер попапа редактирования тарифа текущего узла.
        popupsProxy.wrap( _.editTfDailyS )( editTfDailyR.component.apply ),

        // Рендер попапа удаления существующего узла:
        popupsProxy.wrap( _.deleteNodeS )( deleteConfirmPopupR.component.apply ),

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

}
