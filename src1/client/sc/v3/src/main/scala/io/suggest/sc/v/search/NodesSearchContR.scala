package io.suggest.sc.v.search

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.sc.m.search.NodesScroll
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

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


  case class State(
                    cssClassNameC      : ReactConnectProxy[String],
                  )

  class Backend($: BackendScope[Props, State]) {

    /** Скроллинг в списке найденных узлов. */
    private def _onScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      val scrollHeight = e.target.scrollHeight
      dispatchOnProxyScopeCB($, NodesScroll(scrollTop, scrollHeight) )
    }

    def render(s: State, children: PropsChildren): VdomElement = {
      val onScrollTm =
        ^.onScroll ==> _onScroll

      // Общий скроллинг для всего содержимого:
      s.cssClassNameC { cssClassNameProxy =>
        <.div(
          ^.`class` := cssClassNameProxy.value,
          onScrollTm,
          children,
        )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        cssClassNameC = propsProxy.connect(_.NodesFound.container.htmlClass)( FastEq.ValueEq ),
      )
    }
    .renderBackendWithChildren[Backend]
    .build

  def apply(props: Props)(children: VdomNode*) = component(props)(children: _*)

}
