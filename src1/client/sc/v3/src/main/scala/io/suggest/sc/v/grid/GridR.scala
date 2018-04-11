package io.suggest.sc.v.grid

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps, ReactConnectProxy}
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.jd.render.v._
import io.suggest.react.ReactDiodeUtil
import io.suggest.sc.m.grid.{GridScroll, MGridS}
import io.suggest.sc.styl.GetScCssF
import io.suggest.sc.tile.TileConstants
import io.suggest.spa.OptFastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 14:38
  * Description: React-компонент плитки карточек.
  */
class GridR(
             gridCoreR                  : GridCoreR,
             jdCssR                     : JdCssR,
             val gridLoaderR            : GridLoaderR,
             getScCssF                  : GetScCssF
           ) {

  import JdCss.JdCssFastEq
  import MGridS.MGridSFastEq
  import gridLoaderR.GridLoaderPropsValFastEq


  /** Модель пропертисов компонента плитки.
    *
    * param grid Состояние плитки.
    * param screen Состояние экрана.
    */
  case class PropsVal(
                       grid       : MGridS,
                       fgColor    : Option[MColorData]
                     )
  implicit object GridPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.grid ===* b.grid) &&
        (a.fgColor ===* b.fgColor)
    }
  }


  /** Модель состояния компонента. */
  protected[this] case class State(
                                    jdCssC              : ReactConnectProxy[JdCss],
                                    gridC               : ReactConnectProxy[MGridS],
                                    gridSzC             : ReactConnectProxy[Option[MSize2di]],
                                    loaderPropsOptC     : ReactConnectProxy[Option[gridLoaderR.PropsVal]]
                                  )

  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, State]) {

    /** Скроллинг плитки. */
    private def onGridScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      ReactDiodeUtil.dispatchOnProxyScopeCB($, GridScroll(scrollTop))
    }

    def render(s: State): VdomElement = {
      val ScCss = getScCssF()
      val GridCss = ScCss.Grid

      <.div(
        ScCss.smFlex, GridCss.outer,

        <.div(
          ScCss.smFlex, GridCss.wrapper,
          ^.onScroll ==> onGridScroll,

          <.div(
            GridCss.content,

            // Рендер style-тега.
            s.jdCssC { jdCssR.apply },

            // Начинается [пере]сборка всей плитки
            // TODO Функция сборки плитки неоптимальна и перегенеривается на каждый чих. Это вызывает лишние перерендеры контейнера плитки.
            {
              // Непосредственный рендер плитки, снаружи от рендера connect-зависимого контейнера плитки.
              val gridCore = s.gridC { gridCoreR.apply }

              s.gridSzC { gridSzOptProxy =>
                <.div(
                  GridCss.container,

                  gridSzOptProxy.value.whenDefined { gridSz =>
                    TagMod(
                      ^.width  := gridSz.width.px,
                      ^.height := (gridSz.height + TileConstants.CONTAINER_OFFSET_BOTTOM + TileConstants.CONTAINER_OFFSET_TOP).px
                    )
                  },

                  gridCore
                )
              }
            },

            // Крутилка подгрузки карточек.
            s.loaderPropsOptC { gridLoaderR.apply }
          )
        )
      )
    }

  }


  val component = ScalaComponent.builder[Props]("Grid")
    .initialStateFromProps { propsProxy =>
      // Наконец, сборка самого состояния.
      State(
        jdCssC = propsProxy.connect { props =>
          props.grid.jdCss
        },

        gridC = propsProxy.connect { props =>
          props.grid
        },

        gridSzC = propsProxy.connect { props =>
          props.grid.gridSz
        }( OptFastEq.OptValueEq ),

        loaderPropsOptC = propsProxy.connect { props =>
          OptionUtil.maybe(props.grid.ads.isPending) {
            val fgColor = props.fgColor.getOrElse( MColorData.Examples.WHITE )
            gridLoaderR.PropsVal(
              fgColor = fgColor,
              // В оригинальной выдачи, линия отрыва шла через весь экран. Тут для простоты -- только под внутренним контейнером.
              widthPx = props.grid.gridSz.map(_.width)
            )
          }
        }( OptFastEq.Wrapped )

      )
    }
    .renderBackend[Backend]
    .build

  private def _apply(props: Props) = component(props)
  val apply: ReactConnectProps[Props_t] = _apply

}
