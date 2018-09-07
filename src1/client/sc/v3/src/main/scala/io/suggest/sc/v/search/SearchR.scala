package io.suggest.sc.v.search

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.color.MColorData
import io.suggest.css.{Css, CssR}
import io.suggest.sc.m.search._
import io.suggest.sc.styl.GetScCssF
import io.suggest.sc.v.hdr.RightR
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, PropsChildren, ScalaComponent}
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 14:24
  * Description: Компонент поисковой панели (живёт справа).
  */
class SearchR(
               rightR             : RightR,
               geoMapOuterR       : GeoMapOuterR,
               searchMapR         : SearchMapR,
               getScCssF          : GetScCssF,
             ) {

  import searchMapR.SearchMapRPropsValFastEq

  type Props = ModelProxy[MScSearch]


  protected[this] case class State(
                                    searchCssC          : ReactConnectProxy[SearchCss],
                                  )


  class Backend( $: BackendScope[Props, State] ) {

    def render(props: Props, s: State, children: PropsChildren): VdomElement = {
      val scCss = getScCssF()
      val SearchCSS = scCss.Search

      // Рендер вкладки карты:
      val geoMap = props.wrap { props =>
        searchMapR.PropsVal(
          mapInit   = props.geo.mapInit,
          searchCss = props.geo.css
        )
      }( searchMapR.apply )

      // Тело текущего таба.
      val tabContentInner =  <.div(
        // Содержимое вкладки с картой.
        ^.`class` := Css.Display.DISPLAY_BLOCK,
        // Списочек найденных элементов над картой (унесён в ScRoot, т.к. зависит от разных top-level-доступных моделей)
        children,
        // Гео.карта:
        geoMap
      )

      val tabContentOuter = props.wrap { props =>
        geoMapOuterR.PropsVal(props.geo.css, showCrossHair = true)
      } { cssProxy =>
        geoMapOuterR(cssProxy)(
          tabContentInner
        )
      }


      <.div(
        SearchCSS.panel,

        // Рендер очень динамической search-only css'ки:
        s.searchCssC { CssR.apply },

        // Фон панели.
        <.div(
          scCss.Root.panelBg
        ),

        // Наполнение панели.
        <.div(
          scCss.Search.content,

          // Стрелка для сворачивания вкладки.
          props.wrap {_ => Option(MColorData.Examples.WHITE) } ( rightR.applyReusable ),

          // Контент вкладки, наконец.
          tabContentOuter
        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        searchCssC = propsProxy.connect(_.geo.css)( FastEq.AnyRefEq )
      )
    }
    .renderBackendWithChildren[Backend]
    .build

  def apply(scSearchProxy: Props)(children: VdomNode*) = component( scSearchProxy )(children: _*)

}
