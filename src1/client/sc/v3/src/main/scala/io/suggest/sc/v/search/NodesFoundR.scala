package io.suggest.sc.v.search

import diode.FastEq
import diode.react.ReactPot._
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import io.suggest.css.Css
import io.suggest.geo.{DistanceUtil, ILPolygonGs, MGeoLoc}
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.maps.u.MapsUtil
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.search.{MNodesFoundS, NodeRowClick, NodesScroll}
import io.suggest.sc.styl.GetScCssF
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromHtml, ScalaComponent}
import japgolly.univeq._
import scalacss.ScalaCssReact._
import io.suggest.common.html.HtmlConstants.`~`

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.07.18 18:28
  * Description: React-компонент, отвечающий за рендер списка найденных узлов: тегов и гео-размещенных узлов.
  */
class NodesFoundR(
                   getScCssF   : GetScCssF
                 ) {

  /** Контейнер данных для рендера компонента.
    *
    * @param found Данные найденных тегов/узлов.
    * @param withDistanceTo Рендерить расстояние до указанной локации. Инстанс mapInit.userLoc.
    */
  case class PropsVal(
                       found            : MNodesFoundS,
                       withDistanceTo   : Option[MGeoLoc]
                     )
  implicit object NodesFoundRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.found ===* b.found) &&
        (a.withDistanceTo ===* b.withDistanceTo)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на клик по одному элементу (ряду, узлу). */
    private def _onNodeRowClick(nodeId: String): Callback = {
      dispatchOnProxyScopeCB($, NodeRowClick(nodeId))
    }

    /** Скроллинг в списке найденных узлов. */
    private def _onNodesListScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      val scrollHeight = e.target.scrollHeight
      dispatchOnProxyScopeCB($, NodesScroll(scrollTop, scrollHeight))
    }


    def render(propsProxy: Props): VdomElement = {
      val scCss = getScCssF()
      val NodesCSS = scCss.Search.Tabs.NodesFound

      val props = propsProxy.value
      val mfound = props.found
      val _tagRowCss = NodesCSS.nodeRow: TagMod
      val _oddCss = NodesCSS.oddRow: TagMod
      lazy val _evenCss = NodesCSS.evenRow: TagMod
      lazy val _distanceCss = NodesCSS.distance: TagMod

      <.div(
        NodesCSS.listDiv,

        // Подписка на события скроллинга:
        ReactCommonUtil.maybe(mfound.hasMore && !mfound.req.isPending) {
          ^.onScroll ==> _onNodesListScroll
        },

        // Рендер нормального списка найденных узлов.
        mfound.req.render { tagsRi =>
          if (tagsRi.resp.isEmpty) {
            <.div(
              _tagRowCss,
              // Надо, чтобы юзер понимал, что запрос поиска отработан.
              tagsRi.textQuery.fold {
                Messages( MsgCodes.`No.tags.here` )
              } { query =>
                Messages( MsgCodes.`No.tags.found.for.1.query`, query )
              }
            )
          } else {
            tagsRi
              .resp
              .iterator
              .zipWithIndex
              .toVdomArray { case (node, i) =>
                val p = node.props

                // Рассчитать наименьшее расстояние от юзера до узла:
                val distancesIter = for {
                  // Если задана точка нахождения юзера...
                  loc <- props.withDistanceTo.iterator
                  locLl = MapsUtil.geoPoint2LatLng( loc.point )
                  // Перебрать гео-шейпы, попутно рассчитывая расстояние до центров:
                  nodeShape <- node.shapes
                } yield {
                  // Поиск точки центра узла.
                  val shapeCenterGp = nodeShape.centerPoint
                    .map { MapsUtil.geoPoint2LatLng }
                    .orElse {
                      nodeShape match {
                        case poly: ILPolygonGs =>
                          val positions = MapsUtil.lPolygon2leafletCoords( poly )
                          val c = MapsUtil.polyLatLngs2center( positions )
                          Some(c)
                        case _ =>
                          None
                      }
                    }
                    .getOrElse {
                      // TODO Последний вариант: взять любую точку шейпа. По-хорошему, этого происходить не должно вообще.
                      MapsUtil.geoPoint2LatLng( nodeShape.firstPoint )
                    }
                  locLl distanceTo shapeCenterGp
                }
                val distanceMOpt = OptionUtil.maybe( distancesIter.nonEmpty )( distancesIter.min )

                // Рендер одного ряда.
                <.div(
                  _tagRowCss,

                  p.colors.bg.whenDefined { bgColor =>
                    ^.backgroundColor := bgColor.hexCode,
                  },
                  p.colors.fg.whenDefined { fgColor =>
                    ^.color := fgColor.hexCode
                  },

                  // Используем nodeId как ключ. Контроллер должен выверять отсутствие дубликатов в списке тегов.
                  ^.key := p.nodeId,

                  // Визуально разделять разные ряды.
                  if (i % 2 ==* 0) _oddCss else _evenCss,

                  // Подсвечивать текущие выделенные теги.
                  ReactCommonUtil.maybe(mfound.selectedId contains p.nodeId) {
                    NodesCSS.selected
                  },

                  ^.onClick --> _onNodeRowClick(p.nodeId),

                  p.hint.whenDefined,

                  // Иконка узла.
                  p.icon.whenDefined { ico =>
                    <.img(
                      NodesCSS.icon,
                      ^.src := ico.url
                    )
                  },

                  // Расстояние от юзера (точки) до указанного узла
                  distanceMOpt.whenDefined { distanceM =>
                    <.span(
                      _distanceCss,
                      `~`,
                      DistanceUtil.formatDistanceM( distanceM )(Messages.f)
                    )
                  }

                )
              }
          }
        },

        // Рендер крутилки ожидания.
        mfound.req.renderPending { _ =>
          <.div(
            _tagRowCss,
            LkPreLoaderR.AnimSmall
          )
        },

        // Рендер ошибки.
        mfound.req.renderFailed { ex =>
          VdomArray(
            <.div(
              _tagRowCss,
              ^.key := "e",
              ^.`class` := Css.Colors.RED,
              ex.getClass.getSimpleName
            ),
            <.div(
              ^.key := "m",
              _tagRowCss,
              ex.getMessage
            )
          )
        }

      ) // nodesList
    }

  }

  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(nodesFound: Props) = component( nodesFound )

}
