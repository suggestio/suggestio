package io.suggest.sc.v.search

import diode.FastEq
import diode.data.Pot
import diode.react.ReactPot._
import diode.react.ModelProxy
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.geo.{DistanceUtil, ILPolygonGs, MGeoLoc}
import io.suggest.i18n.MsgCodes
import io.suggest.lk.r.LkPreLoaderR
import io.suggest.maps.u.MapsUtil
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCBf
import io.suggest.sc.m.search.{MSearchRespInfo, NodeRowClick, NodesScroll}
import io.suggest.sc.styl.GetScCssF
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromHtml, ScalaComponent}
import japgolly.univeq._
import scalacss.ScalaCssReact._
import io.suggest.common.html.HtmlConstants.`~`
import io.suggest.maps.nodes.MGeoNodePropsShapes
import io.suggest.sc.search.MSearchTab
import io.suggest.sjs.common.xhr.Xhr

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
                       req              : Pot[MSearchRespInfo[Seq[MGeoNodePropsShapes]]],
                       hasMore          : Boolean,
                       // TODO Когда станет допустимо сразу несколько тегов, надо заменить на Set[String].
                       selectedId       : Option[String],
                       withDistanceTo   : Option[MGeoLoc],
                       onTab            : MSearchTab,
                       // TODO Сделать без orNull неопциональным, когда панель тегов будет унифицирована с основным списком.
                       searchCssOrNull  : SearchCss = null
                     )
  implicit object NodesFoundRPropsValFastEq extends FastEq[PropsVal] {
    import io.suggest.ueq.JsUnivEqUtil._
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.req ===* b.req) &&
        (a.hasMore ==* b.hasMore) &&
        (a.selectedId ===* b.selectedId) &&
        (a.withDistanceTo ===* b.withDistanceTo) &&
        (a.onTab ===* b.onTab) &&
        // Наверное, проверять css не нужно. Но мы всё же перерендериваем.
        (a.searchCssOrNull ===* b.searchCssOrNull)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на клик по одному элементу (ряду, узлу). */
    private def _onNodeRowClick(nodeId: String): Callback = {
      dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        val v = propsProxy.value.onTab
        NodeRowClick(nodeId, v)
      }
    }

    /** Скроллинг в списке найденных узлов. */
    private def _onNodesListScroll(e: ReactEventFromHtml): Callback = {
      val scrollTop = e.target.scrollTop
      val scrollHeight = e.target.scrollHeight
      dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        NodesScroll(scrollTop, scrollHeight, propsProxy.value.onTab)
      }
    }


    def render(propsProxy: Props): VdomElement = {
      val scCss = getScCssF()
      val NodesCSS = scCss.Search.Tabs.NodesFound

      val props = propsProxy.value
      val _tagRowCss = NodesCSS.nodeRow: TagMod
      // TODO Сделать неопциональным, когда tags tab будет унифицирована с основной вкладкой.
      val searchCssOpt = Option(props.searchCssOrNull)

      <.div(
        NodesCSS.listDiv,
        searchCssOpt.whenDefined( _.NodesFound.nodesList ),

        // Подписка на события скроллинга:
        ReactCommonUtil.maybe(props.hasMore && !props.req.isPending) {
          ^.onScroll ==> _onNodesListScroll
        },

        // Рендер нормального списка найденных узлов.
        props.req.render { tagsRi =>
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
            val _oddCss = NodesCSS.oddRow: TagMod
            val _evenCss = NodesCSS.evenRow: TagMod
            val _distanceCss = NodesCSS.distance: TagMod
            val _rowHasIconCss = NodesCSS.rowHasIcon: TagMod
            val _rowNoIconCss = NodesCSS.rowNoIcon: TagMod
            val _selectedCss = NodesCSS.selected: TagMod
            val _iconCss = NodesCSS.icon: TagMod
            val _rowCss2 = searchCssOpt.whenDefined(_.NodesFound.nodeRow)

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
                      // Последний вариант: взять любую точку шейпа. По-хорошему, этого происходить не должно вообще, но TODO сервер пока не шлёт точку, только шейпы.
                      MapsUtil.geoPoint2LatLng( nodeShape.firstPoint )
                    }
                  locLl distanceTo shapeCenterGp
                }
                val distanceMOpt = OptionUtil.maybe( distancesIter.nonEmpty )( distancesIter.min )

                // Рендер одного ряда.
                <.div(
                  _tagRowCss, _rowCss2,

                  ^.onClick --> _onNodeRowClick(p.nodeId),

                  // Цвет фона. Для прозрачных лого - нужна очень.
                  p.colors.bg.whenDefined { bgColor =>
                    ^.backgroundColor := bgColor.hexCode
                  },
                  // Цвет текста поверх фона c bgColor.
                  p.colors.fg.whenDefined { fgColor =>
                    ^.color := fgColor.hexCode
                  },

                  // Используем nodeId как ключ. Контроллер должен выверять отсутствие дубликатов в списке тегов.
                  ^.key := p.nodeId,

                  // Визуально разделять разные ряды.
                  if (i % 2 ==* 0) _oddCss else _evenCss,

                  // Подсвечивать текущие выделенные теги.
                  ReactCommonUtil.maybe(props.selectedId contains p.nodeId)( _selectedCss ),

                  // Иконка узла.
                  p.icon.fold(_rowNoIconCss) { ico =>
                    TagMod(
                      _rowHasIconCss,
                      <.img(
                        _iconCss,
                        ^.src := Xhr.mkAbsUrlIfPreferred( ico.url )
                      )
                    )
                  },
                  HtmlConstants.NBSP_STR,

                  p.hint.whenDefined,

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
        props.req.renderPending { _ =>
          <.div(
            _tagRowCss,
            LkPreLoaderR.AnimSmall
          )
        },

        // Рендер ошибки.
        props.req.renderFailed { ex =>
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
