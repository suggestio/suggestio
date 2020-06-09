package io.suggest.sc.v.search

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.geo.{DistanceUtil, ILPolygonGs, MGeoPoint}
import io.suggest.maps.nodes.MGeoNodePropsShapes
import io.suggest.maps.u.MapsUtil
import scalacss.ScalaCssReact._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.sc.m.search.NodeRowClick
import japgolly.scalajs.react.{BackendScope, Callback, React, ReactEvent, ScalaComponent, raw}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import io.suggest.common.empty.OptionUtil
import ReactCommonUtil.Implicits._
import com.materialui.{Mui, MuiListItem, MuiListItemClasses, MuiListItemIcon, MuiListItemIconClasses, MuiListItemIconProps, MuiListItemProps, MuiListItemText, MuiListItemTextClasses, MuiListItemTextProps, MuiSvgIconProps}
import io.suggest.common.html.HtmlConstants
import io.suggest.proto.http.client.HttpClient
import io.suggest.n2.node.MNodeTypes
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.i18n.MCommonReactCtx
import io.suggest.sc.v.styl.ScCssStatic

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.18 12:29
  * Description: React-компонент для рендера одного ряда в списке найденных рядов.
  */
class NodeFoundR(
                  crCtxProv     : React.Context[MCommonReactCtx],
                ) {

  /** Контейнер пропертисов компонента.
    *
    * @param node Данные по узлу, который рендерится.
    * @param searchCss Инстанс класса SearchCss.
    * @param withDistanceToNull Отображать расстояние до указанной точки.
    * @param selected Является ли элемент списка выбранным (текущим)?
    */
  case class PropsVal(
                       node               : MGeoNodePropsShapes,
                       searchCss          : SearchCss,
                       withDistanceToNull : MGeoPoint,
                       selected           : Boolean,
                     )

  implicit object NodeFoundRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.node                 ===* b.node) &&
      (a.searchCss            ===* b.searchCss) &&
      (a.withDistanceToNull   ===* b.withDistanceToNull) &&
      (a.selected              ==* b.selected)
    }
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, Props_t]) {

    /** Реакция на клик по одному элементу (ряд-узел). */
    private def _onNodeRowClick(e: ReactEvent): Callback = {
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) { propsProxy: Props =>
        NodeRowClick(
          nodeId = propsProxy.value.node.props.idOrNameOrEmpty
        )
      }
    }
    private val _onNodeRowClickJsF = ReactCommonUtil.cbFun1ToJsCb( _onNodeRowClick )

    /** Рендер вёрстки компонента. */
    def render(propsProxy: Props): VdomElement = {
      val props = propsProxy.value
      val NodesCSS = ScCssStatic.Search.NodesFound

      // Рассчитать наименьшее расстояние от юзера до узла:
      val distanceMOpt = for {
        distanceToPoint <- Option( props.withDistanceToNull )

        locLl = MapsUtil.geoPoint2LatLng( distanceToPoint )

        distancesIter = for {
          // Перебрать гео-шейпы, попутно рассчитывая расстояние до центров:
          nodeShape <- props.node.shapes.iterator
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

        distance <- OptionUtil.maybe(distancesIter.nonEmpty)( distancesIter.min )

      } yield distance

      val p = props.node.props

      // Нельзя nodeId.get, т.к. могут быть узлы без id (по идее - максимум 1 узел в списке).
      val nodeId = p.idOrNameOrEmpty

      val isTag = p.ntype ==* MNodeTypes.Tag

      var rowRootCssAcc = props.searchCss.NodesFound.rowItemBgF(nodeId) :: Nil
      rowRootCssAcc ::= {
        if (isTag) NodesCSS.tagRow
        else NodesCSS.adnNodeRow
      }
      val rowRootCss = rowRootCssAcc
        .map(_.htmlClass)
        .mkString( HtmlConstants.SPACE )

      val listItemCss = new MuiListItemClasses {
        override val root = rowRootCss
      }

      MuiListItem.component.withKey(nodeId)(
        new MuiListItemProps {
          override val classes = listItemCss
          override val button = true
          override val onClick = _onNodeRowClickJsF
          override val selected = props.selected
          override val dense = !isTag
          override val disableGutters = true
        }
      )(
        // Если тег, то имеет смысл выставить пометку, что это тег:
        ReactCommonUtil.maybeEl( isTag ) {
          MuiListItemIcon {
            val cls = new MuiListItemIconClasses {
              override val root = NodesCSS.tagRowIconCont.htmlClass
            }
            new MuiListItemIconProps {
              override val classes = cls
            }
          }(
            Mui.SvgIcons.LocalOffer {
              new MuiSvgIconProps {
                override val className = NodesCSS.tagRowIcon.htmlClass
              }
            }()
          )
        },

        // Название узла
        p.name.whenDefinedEl { nodeName =>
          // Для тегов: они идут кашей, поэтому отступ между названием тега и иконкой уменьшаем.
          val rootCss = JsOptionUtil.maybeDefined(isTag)( NodesCSS.tagRowText.htmlClass )

          // Текст состоит из статической и динамической вёрстки.
          val primaryCss = (
            NodesCSS.tagRowTextPrimary ::
            props.searchCss.NodesFound.rowTextPrimaryF(nodeId) ::
            Nil
          ).toHtmlClass

          val theClasses = new MuiListItemTextClasses {
            override val root = rootCss
            override val primary = primaryCss
            override val secondary = props.searchCss.NodesFound.rowTextSecondaryF(nodeId).htmlClass
          }

          val text2ndOpt = distanceMOpt
            .map { distanceM =>
              crCtxProv.consume { crCtx =>
                val distanceFmt = crCtx.messages( DistanceUtil.formatDistanceM(distanceM) )
                (HtmlConstants.`~` + distanceFmt): raw.React.Node
              }.rawNode
            }
          MuiListItemText(
            new MuiListItemTextProps {
              override val classes   = theClasses
              override val primary   = js.defined( nodeName )
              override val secondary = text2ndOpt.toUndef
            }
          )()
        },

        // Иконка узла, присланная сервером:
        p.logoOpt.whenDefinedNode { icon =>
          MuiListItemIcon()(
            <.img(
              NodesCSS.nodeLogo,
              ^.src := HttpClient.mkAbsUrlIfPreferred( icon.url )
            )
          )
        }

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

}
