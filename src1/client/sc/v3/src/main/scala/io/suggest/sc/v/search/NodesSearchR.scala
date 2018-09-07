package io.suggest.sc.v.search

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.sc.m.search.{MScSearchText, NodesScroll}
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.09.18 21:08
  * Description: wrap-компонент, объединяющий [[NodesSearchR]] и [[STextR]] под общим скроллингом.
  * Появился именно из-за необходимости общего скроллинга на фоне других потребностей.
  */
class NodesSearchR(
                    val sTextR        : STextR,
                    val nodesFoundR   : NodesFoundR,
                  ) {


  import MScSearchText.MScSearchTextFastEq
  import NodesFoundR.NodesFoundRPropsValFastEq

  case class PropsVal(
                       text       : MScSearchText,
                       nodeFound  : NodesFoundR.PropsVal,
                       searchCss  : SearchCss,
                     )
  implicit object NodesSearchRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.text ===* b.text) &&
      (a.nodeFound ===* b.nodeFound) &&
      (a.searchCss ===* b.searchCss)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  case class State(
                    sTextC      : ReactConnectProxy[sTextR.Props_t],
                    nodesFoundC : ReactConnectProxy[nodesFoundR.Props_t]
                  )

  class Backend($: BackendScope[Props, State]) {

    /** Скроллинг в списке найденных узлов. */
    private def _onScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      val scrollHeight = e.target.scrollHeight
      dispatchOnProxyScopeCB($, NodesScroll(scrollTop, scrollHeight) )
    }

    def render(propsProxy: Props, s: State): VdomElement = {
      val props = propsProxy.value
      <.div(
        // Общий скроллинг для всего содержимого:
        props.searchCss.NodesFound.container,
        ^.onScroll ==> _onScroll,

        // Поисковое текстовое поле:
        s.sTextC { sTextR.apply },
        // Панель поиска: контент, зависимый от корневой модели:
        s.nodesFoundC { nodesFoundR.apply }
      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        sTextC = propsProxy.connect(_.text),
        nodesFoundC = propsProxy.connect(_.nodeFound)
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
