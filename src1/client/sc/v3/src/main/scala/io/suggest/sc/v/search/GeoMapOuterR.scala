package io.suggest.sc.v.search

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import scalacss.ScalaCssReact._
import io.suggest.common.html.HtmlConstants
import io.suggest.react.ReactCommonUtil
import japgolly.scalajs.react.{BackendScope, PropsChildren, React, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import OptionUtil.BoolOptOps
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.08.18 17:19
  * Description: Компонент с вёрсткой обёртки вокруг гео-карты.
  * Вынесен из SearchMapR в виду использования очень динамичного SearchCss, а сам вынос уже назревал.
  *
  * Если имена стилей не меняются, то можно юзать через wrap(). Иначе - connect()
  */
class GeoMapOuterR(
                    scCssP    : React.Context[ScCss],
                  ) {

  case class PropsVal(
                       searchCss          : SearchCss,
                     // TODO Удалить showCrossHair
                       showCrossHair      : Boolean
                     )
  case class State(
                    showCrossHairSomeC    : ReactConnectProxy[Option[Boolean]]
                  )

  type Props = ModelProxy[PropsVal]

  class Backend($: BackendScope[Props, State]) {

    def render(searchCssProxy: Props, s: State, children: PropsChildren): VdomElement = {
      scCssP.consume { scCss =>
        val mapTabCSS = scCss.Search.Geo
        val searchCss = searchCssProxy.value.searchCss

        <.div(
          mapTabCSS.outer,

          <.div(
            mapTabCSS.wrapper,

            <.div(
              mapTabCSS.inner,

              // Наконец, непосредственный рендер карты:
              children

            )
          ),

          // Прицел для наведения. Пока не ясно, отображать его всегда или только когда карта перетаскивается.
          s.showCrossHairSomeC { showCrossHairOrNone =>
            ReactCommonUtil.maybeEl( showCrossHairOrNone.value.getOrElseFalse ) {
              <.div(
                ScCssStatic.Search.Geo.crosshair, mapTabCSS.crosshair,
                searchCss.GeoMap.crosshair,
                HtmlConstants.PLUS
              )
            }
          }

        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        showCrossHairSomeC = propsProxy.connect { props =>
          OptionUtil.maybeTrue(props.showCrossHair)
        }( FastEq.ValueEq )
      )
    }
    .renderBackendWithChildren[Backend]
    .build

}
