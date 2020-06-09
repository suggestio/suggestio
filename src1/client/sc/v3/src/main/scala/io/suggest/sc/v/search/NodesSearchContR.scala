package io.suggest.sc.v.search

import diode.react.ModelProxy
import io.suggest.sc.m.search.NodesScroll
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.09.18 21:08
  * Description: wrap-компонент, объединяющий [[NodesSearchContR]], [[STextR]] и т.д. под общим скроллингом.
  * Появился именно из-за необходимости общего скроллинга на фоне других потребностей.
  */
class NodesSearchContR {

  type Props_t = SearchCss
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Unit]) {

    /** Скроллинг в списке найденных узлов. */
    private def _onScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      val scrollHeight = e.target.scrollHeight
      dispatchOnProxyScopeCB($, NodesScroll(scrollTop, scrollHeight) )
    }

    def render(propsProxy: Props, children: PropsChildren): VdomElement = {
      <.div(
        // Общий скроллинг для всего содержимого:
        propsProxy.value.NodesFound.container,
        ^.onScroll ==> _onScroll,

        children
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackendWithChildren[Backend]
    .build

  def apply(props: Props)(children: VdomNode*) = component(props)(children: _*)

}
