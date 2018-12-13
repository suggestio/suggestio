package io.suggest.sc.v.grid

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.grid.{GridConst, GridScrollUtil}
import io.suggest.jd.render.v._
import io.suggest.react.ReactDiodeUtil
import io.suggest.sc.m.grid.{GridScroll, MGridCoreS, MGridS}
import io.suggest.sc.styl.{GetScCssF, ScCssStatic}
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
  import MGridCoreS.MGridCoreSFastEq
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
                                    coreC               : ReactConnectProxy[MGridCoreS],
                                    gridSzC             : ReactConnectProxy[MSize2di],
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
      val smFlex: TagMod = ScCssStatic.smFlex

      <.div(
        smFlex, GridCss.outer,

        <.div(
          smFlex, GridCss.wrapper,
          ^.id := GridScrollUtil.SCROLL_CONTAINER_ID,
          ^.onScroll ==> onGridScroll,

          <.div(
            GridCss.content,

            // Рендер style-тега.
            s.jdCssC { jdCssR.apply },

            // Начинается [пере]сборка всей плитки
            // TODO Функция сборки плитки неоптимальна и перегенеривается на каждый чих. Это вызывает лишние перерендеры контейнера плитки.
            {
              // Непосредственный рендер плитки, снаружи от рендера connect-зависимого контейнера плитки.
              val gridCore = s.coreC { gridCoreR.apply }

              s.gridSzC { gridSzProxy =>
                val gridSz = gridSzProxy.value

                <.div(
                  GridCss.container,
                  ^.width  := gridSz.width.px,
                  ^.height := (gridSz.height + GridConst.CONTAINER_OFFSET_BOTTOM + GridConst.CONTAINER_OFFSET_TOP).px,

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


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      // Наконец, сборка самого состояния.
      State(
        jdCssC = propsProxy.connect { props =>
          props.grid.core.jdCss
        },

        coreC = propsProxy.connect { props =>
          props.grid.core
        },

        gridSzC = propsProxy.connect { props =>
          props.grid.core.gridBuild.gridWh
        }( FastEq.ValueEq ) ,

        loaderPropsOptC = propsProxy.connect { props =>
          OptionUtil.maybe(props.grid.core.ads.isPending) {
            val fgColor = props.fgColor.getOrElse( MColorData.Examples.WHITE )
            gridLoaderR.PropsVal(
              fgColor = fgColor,
              // В оригинальной выдачи, линия отрыва шла через весь экран. Тут для простоты -- только под внутренним контейнером.
              widthPx = Some( props.grid.core.gridBuild.gridWh.width )
            )
          }
        }( OptFastEq.Wrapped )

      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
