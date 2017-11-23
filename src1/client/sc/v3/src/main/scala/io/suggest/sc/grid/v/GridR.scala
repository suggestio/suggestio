package io.suggest.sc.grid.v

import com.github.dantrain.react.stonecutter.CSSGrid
import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.blk.BlockPaddings
import io.suggest.color.MColorData
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.grid.build.{GridBuildArgs, GridBuildRes_t}
import io.suggest.jd.MJdConf
import io.suggest.jd.render.m.{MJdArgs, MJdCssArgs, MJdRenderArgs}
import io.suggest.jd.render.v.{JdCss, JdCssR, JdGridUtil, JdR}
import io.suggest.jd.tags.JdTag
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.grid.m.{GridBlockClick, GridScroll, HandleGridBuildRes, MGridS}
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sc.tile.TileConstants
import io.suggest.spa.OptFastEq
import io.suggest.ueq.UnivEqUtil._

import scalacss.ScalaCssReact._
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 14:38
  * Description: React-компонент плитки карточек.
  */
class GridR(
             jdGridUtil                 : JdGridUtil,
             jdR                        : JdR,
             jdCssR                     : JdCssR,
             val gridLoaderR            : GridLoaderR,
             getScCssF                  : GetScCssF
           ) {

  import MJdConf.MJdConfFastEq
  import MJdArgs.MJdWithArgsFastEq
  import JdCss.JdCssFastEq
  import gridLoaderR.GridLoaderPropsValFastEq
  import MGridS.MGridSFastEq


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

  /** Модель состояния компонента.
    *
    * @param jdConfOptC Полуготовый инстанс MJdArgs без конкретных эджей и шаблона внутри.
    */
  protected[this] case class State(
                                    jdConfOptC          : ReactConnectProxy[Option[MJdConf]],
                                    gridOptC            : ReactConnectProxy[Option[MGridS]],
                                    loaderPropsOptC     : ReactConnectProxy[Option[gridLoaderR.PropsVal]]
                                  )

  type Props = ModelProxy[Option[PropsVal]]

  class Backend($: BackendScope[Props, State]) {

    /** Завершение обсчёта плитки. */
    private def onGridLayout(layoutRes: GridBuildRes_t): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCB($, HandleGridBuildRes(layoutRes))
    }
    private val _onGridLayoutF = ReactCommonUtil.cbFun1ToF( onGridLayout )


    /** Клик по карточке в плитке. */
    private def onBlockClick(nodeId: String): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCB($, GridBlockClick(nodeId))
    }


    /** Скроллинг плитки. */
    private def onGridScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      ReactDiodeUtil.dispatchOnProxyScopeCB($, GridScroll(scrollTop))
    }


    def render(s: State): VdomElement = {
      s.gridOptC { gridOptProxy =>
        gridOptProxy.value.whenDefinedEl { gridS =>
          val ScCss = getScCssF()
          val GridCss = ScCss.Grid

          <.div(
            ScCss.smFlex, GridCss.outer,

            <.div(
              ScCss.smFlex, GridCss.wrapper,
              ^.onScroll ==> onGridScroll,

              <.div(
                GridCss.content,

                s.jdConfOptC { jdConfOptProxy =>
                  jdConfOptProxy.value.whenDefinedEl { jdConf =>
                    val templates = gridS.ads
                      .getOrElse(Nil)   // TODO Правильно ли тут это?
                      .flatMap { ad =>
                        ad.focused.fold [Seq[Tree[JdTag]]] {
                          ad.main.template :: Nil
                        } { focBlk =>
                          focBlk.template.subForest
                        }
                      }

                    val jdCss = JdCss(
                      MJdCssArgs(
                        templates = templates,
                        conf      = jdConf
                      )
                    )

                    // Начинается [пере]сборка всей плитки
                    <.div(
                      GridCss.container,

                      // Рендер style-тега.
                      jdConfOptProxy.wrap(_ => jdCss)(jdCssR.apply),

                      gridS.gridSz.whenDefined { gridSz =>
                        TagMod(
                          ^.width  := gridSz.width.px,
                          ^.height := (gridSz.height + TileConstants.CONTAINER_OFFSET_BOTTOM).px
                        )
                      },

                      // Наконец сама плитка карточек:
                      CSSGrid {
                        jdGridUtil.mkCssGridArgs(
                          jds       = templates,
                          conf      = jdConf,
                          gridBuildArgsF = { items =>
                            GridBuildArgs(
                              itemsExtDatas   = items,
                              jdConf          = jdConf,
                              onLayout        = Some(_onGridLayoutF),
                              offY            = TileConstants.CONTAINER_OFFSET_TOP
                            )
                          }
                        )
                      } {
                        val iter = for {
                          (scAdData, rootCounter) <- gridS.ads.iterator.flatten.zipWithIndex
                          keyPrefix = scAdData.nodeId
                            .getOrElse( rootCounter.toString )
                          edges = scAdData.focused
                            .fold(scAdData.main.edges)(_.edges)
                          (template, j) <- scAdData.focused
                            .fold [Seq[Tree[JdTag]]] (scAdData.main.template :: Nil) { foc =>
                              foc.template.subForest
                            }
                            .iterator
                            .zipWithIndex
                        } yield {
                          jdConfOptProxy.wrap { _ =>
                            MJdArgs(
                              template    = template,
                              renderArgs  = MJdRenderArgs(
                                edges     = edges
                              ),
                              jdCss = jdCss,
                              conf = jdConf
                            )
                          } { jdArgsProxy =>
                            val keyStr = keyPrefix + `.` + j
                            scAdData.nodeId.fold[VdomElement] {
                              jdR.component
                                .withKey(keyStr)(jdArgsProxy)
                            } { nodeId =>
                              <.div(
                                ^.key := keyStr,
                                <.a(
                                  ^.onClick --> onBlockClick(nodeId),
                                  jdR(jdArgsProxy)
                                )
                              )
                            }
                          }
                        }
                        iter.toVdomArray
                      }
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
    }

  }

  val component = ScalaComponent.builder[Props]("Grid")
    .initialStateFromProps { propsOptProxy =>
      State(
        jdConfOptC = propsOptProxy.connect { propsOpt =>
          for (props <- propsOpt) yield {
            MJdConf(
              isEdit            = false,
              szMult            = props.grid.szMult,
              blockPadding      = BlockPaddings.default,
              gridColumnsCount  = props.grid.columnsCount
            )
          }
        }( OptFastEq.Wrapped ),

        gridOptC = propsOptProxy.connect { propsOpt =>
          for (props <- propsOpt) yield {
            props.grid
          }
        }( OptFastEq.Wrapped ),

        loaderPropsOptC = propsOptProxy.connect { propsOpt =>
          for {
            props     <- propsOpt
            if props.grid.ads.isPending
            fgColor   <- props.fgColor
          } yield {
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

  def apply(props: Props) = component(props)

}
