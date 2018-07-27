package io.suggest.sc.v.search

import diode.react.ModelProxy
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, PropsChildren, ScalaComponent}
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.17 16:53
  * Description: React-компонент для поиска и выборки тегов.
  * Использовать через wrap().
  */
class TagsSearchR(
                   getScCssF    : GetScCssF
                 ) {

  type Props = ModelProxy[_]


  class Backend($: BackendScope[Props, Unit]) {

    def render(children: PropsChildren): VdomElement = {
      val scCss = getScCssF()
      val TabCSS = scCss.Search.Tabs.TagsTab

      <.div(
        TabCSS.outer,

        <.div(
          TabCSS.wrapper,

          <.div(
            TabCSS.inner,

            // Наконец, начинается содержимое вкладки с тегами:
            // TODO Надо контейнер отделить от динамической части. Тут по сути проброс в Props
            children

          ) // inner
        )   // wrap
      )     // outer
    }       // render()

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackendWithChildren[Backend]
    .build

  def apply(tagSearchProxy: Props)(children: VdomNode*) = component(tagSearchProxy)(children: _*)

}
