package io.suggest.sc.v.search

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import scalacss.ScalaCssReact._
import io.suggest.geo.{DistanceUtil, ILPolygonGs, MGeoLoc}
import io.suggest.maps.nodes.MGeoNodePropsShapes
import io.suggest.maps.u.MapsUtil
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.styl.GetScCssF
import io.suggest.ueq.UnivEqUtil._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.search.NodeRowClick
import io.suggest.sjs.common.xhr.Xhr
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import HtmlConstants.`~`
import io.suggest.common.empty.OptionUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.18 12:29
  * Description: React-компонент для рендера одного ряда в списке найденных рядов.
  */
class NodeFoundR(
                  getScCssF   : GetScCssF
                ) {

  /** Контейнер пропертисов компонента.
    *
    * @param node Данные по узлу, который рендерится.
    * @param i Порядковый номер в списке узлов.
    * @param searchCss Инстанс класса SearchCss.
    * @param withDistanceTo Отображать расстояние до указанной точки.
    * @param selected Является ли элемент списка выбранным (текущим)?
    */
  case class PropsVal(
                       node               : MGeoNodePropsShapes,
                       i                  : Int,
                       searchCss          : SearchCss,
                       withDistanceTo     : Option[MGeoLoc],
                       selected           : Boolean
                     )

  implicit object NodeFoundRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.node               ===* b.node) &&
        (a.i                 ==* b.i) &&
        (a.searchCss        ===* b.searchCss) &&
        (a.withDistanceTo   ===* b.withDistanceTo) &&
        (a.selected          ==* b.selected)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    /** Реакция на клик по одному элементу (ряд-узел). */
    private def _onNodeRowClick(nodeId: String): Callback = {
      dispatchOnProxyScopeCB($, NodeRowClick(nodeId) )
    }

    /** Рендер вёрстки компонента. */
    def render(propsProxy: Props): VdomElement = {
      val scCss = getScCssF()
      val NodesCSS = scCss.Search.Tabs.NodesFound

      val props = propsProxy.value

      // Рассчитать наименьшее расстояние от юзера до узла:
      val distancesIter = for {
        // Если задана точка нахождения юзера...
        loc <- props.withDistanceTo.iterator
        locLl = MapsUtil.geoPoint2LatLng( loc.point )
        // Перебрать гео-шейпы, попутно рассчитывая расстояние до центров:
        nodeShape <- props.node.shapes
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
      val distanceMOpt = OptionUtil.maybe(distancesIter.nonEmpty)( distancesIter.min )

      val p = props.node.props

      // Рендер одного ряда.
      <.div(
        // Без key: ключ задаётся на уровне списка NodesFoundR, а не здесь.
        NodesCSS.nodeRow,
        props.searchCss.NodesFound.nodeRow,

        ^.onClick --> _onNodeRowClick(p.nodeId),

        // Цвет фона. Для прозрачных лого - нужна очень.
        p.colors.bg.whenDefined { bgColor =>
          ^.backgroundColor := bgColor.hexCode
        },
        // Цвет текста поверх фона c bgColor.
        p.colors.fg.whenDefined { fgColor =>
          ^.color := fgColor.hexCode
        },

        // Визуально разделять разные ряды.
        if (props.i % 2 ==* 0)
          NodesCSS.oddRow
        else
          NodesCSS.evenRow,

        // Подсвечивать текущие выделенные теги.
        ReactCommonUtil.maybe(props.selected)( NodesCSS.selected ),

        // Иконка узла.
        p.icon.fold[TagMod]( NodesCSS.rowNoIcon ) { ico =>
          TagMod(
            NodesCSS.rowHasIcon,
            <.img(
              NodesCSS.icon,
              ^.src := Xhr.mkAbsUrlIfPreferred( ico.url )
            )
          )
        },
        HtmlConstants.NBSP_STR,

        p.hint.whenDefined,

        // Расстояние от юзера (точки) до указанного узла
        distanceMOpt.whenDefined { distanceM =>
          <.span(
            NodesCSS.distance,
            `~`,
            DistanceUtil.formatDistanceM( distanceM )(Messages.f)
          )
        }
      )
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

}
