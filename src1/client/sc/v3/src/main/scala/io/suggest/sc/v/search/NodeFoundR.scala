package io.suggest.sc.v.search

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.geo.{DistanceUtil, ILPolygonGs, MGeoPoint}
import io.suggest.maps.nodes.MGeoNodePropsShapes
import io.suggest.maps.u.MapsUtil
import io.suggest.msg.Messages
import scalacss.ScalaCssReact._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.search.NodeRowClick
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent, ScalaComponent, raw}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import chandu0101.scalajs.react.components.materialui.{Mui, MuiListItem, MuiListItemClasses, MuiListItemIcon, MuiListItemProps, MuiListItemText, MuiListItemTextClasses, MuiListItemTextProps}
import io.suggest.common.empty.OptionUtil
import ReactCommonUtil.Implicits._
import io.suggest.common.html.HtmlConstants
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.sc.styl.GetScCssF
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.sjs.common.xhr.Xhr

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.18 12:29
  * Description: React-компонент для рендера одного ряда в списке найденных рядов.
  */
class NodeFoundR(getScCssF: GetScCssF) {

  /** Контейнер пропертисов компонента.
    *
    * @param node Данные по узлу, который рендерится.
    * @param searchCss Инстанс класса SearchCss.
    * @param withDistanceTo Отображать расстояние до указанной точки.
    * @param selected Является ли элемент списка выбранным (текущим)?
    */
  case class PropsVal(
                       node               : MGeoNodePropsShapes,
                       searchCss          : SearchCss,
                       withDistanceTo     : MGeoPoint,
                       selected           : Boolean
                     )

  implicit object NodeFoundRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.node               ===* b.node) &&
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
      val props = propsProxy.value
      val NodesCSS = getScCssF().Search.Tabs.NodesFound

      // Рассчитать наименьшее расстояние от юзера до узла:
      val locLl = MapsUtil.geoPoint2LatLng( props.withDistanceTo )
      val distancesIter = for {
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

      val listItemCss = new MuiListItemClasses {
        override val root = props.searchCss.NodesFound.rowItemBgF(p.nodeId).htmlClass
      }
      val isTag = p.ntype ==* MNodeTypes.Tag

      MuiListItem.component.withKey(p.nodeId)(
        new MuiListItemProps {
          override val classes = listItemCss
          override val button = true
          override val onClick = js.defined { _: ReactEvent =>
            _onNodeRowClick(p.nodeId).runNow()
          }
          override val selected = props.selected
          // Если есть картинка, то сдвиги справа-слева лучше убрать
          override val disableGutters = p.icon.nonEmpty
          override val dense = !isTag
        }
      )(
        // Иконка узла, присланная сервером:
        p.icon.whenDefinedEl { icon =>
          MuiListItemIcon()(
            <.img(
              NodesCSS.icon,
              ^.src := Xhr.mkAbsUrlIfPreferred( icon.url )
            )
          )
        },

        // Название узла
        p.hint.whenDefinedEl { nodeName =>
          val theClasses = new MuiListItemTextClasses {
            override val primary = props.searchCss.NodesFound.rowTextPrimaryF(p.nodeId).htmlClass
            override val secondary = props.searchCss.NodesFound.rowTextSecondaryF(p.nodeId).htmlClass
          }

          val text2ndOpt = distanceMOpt
            .map { distanceM =>
              val distanceFmt = DistanceUtil.formatDistanceM(distanceM)(Messages.f)
              (HtmlConstants.`~` + distanceFmt): raw.React.Node
            }
          MuiListItemText(
            new MuiListItemTextProps {
              override val classes = js.defined {
                theClasses
              }
              override val primary = js.defined( nodeName )
              override val secondary = text2ndOpt.toUndef
            }
          )()
        },

        // Если тег, то имеет смысл выставить пометку, что это тег:
        ReactCommonUtil.maybeEl( isTag ) {
          MuiListItemIcon()(
            Mui.SvgIcons.LocalOffer()()
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
