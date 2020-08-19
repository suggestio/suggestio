package io.suggest.sc.v.search.found

import com.materialui.{Mui, MuiGrid, MuiGridClasses, MuiGridProps, MuiListItem, MuiListItemClasses, MuiListItemIcon, MuiListItemIconClasses, MuiListItemIconProps, MuiListItemProps, MuiListItemText, MuiListItemTextClasses, MuiListItemTextProps, MuiSvgIconClasses, MuiSvgIconProps}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.css.ScalaCssUtil.Implicits._
import io.suggest.geo.{DistanceUtil, ILPolygonGs}
import io.suggest.i18n.MCommonReactCtx
import io.suggest.maps.u.MapsUtil
import io.suggest.n2.node.MNodeTypes
import io.suggest.proto.http.client.HttpClient
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.m.search.MNodesFoundRowProps
import io.suggest.sc.v.styl.{ScCss, ScCssStatic}
import io.suggest.sjs.common.empty.JsOptionUtil
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.spa.DAction
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import scalacss.ScalaCssReact._

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.18 12:29
  * Description: React-компонент для рендера одного ряда в списке найденных рядов.
  *
  * Без dependency injection, чтобы снизить кол-во React.Context().consume-компонентов: пусть всё будет в списке снаружи.
  */
final class NfRowR(
                    scCssP                   : React.Context[ScCss],
                    crCtxP                   : React.Context[MCommonReactCtx],
                  ) {

  def apply( nodesFoundRowsC: ReactConnectProxy[Seq[MNodesFoundRowProps]] ) =
    _using( nfRowR => nodesFoundRowsC( nfRowR.rows ) )

  def apply( nodesFoundProxy: ModelProxy[Seq[MNodesFoundRowProps]] ) =
    _using( nfRowR => nfRowR.rows(nodesFoundProxy) )


  private def _using(f: NfRowR2 => VdomElement) = {
    (onClickF: MSc3IndexResp => DAction) =>
      crCtxP.consume { crCtx =>
        scCssP.consume { scCss =>
          val nfRowR = NfRowR2( crCtx, scCss, onClickF )
          f(nfRowR)
        }
      }
  }

}


/** Чтобы не плодить кучу ctx.consume()-компонентов, тут класс-контейнер единственного инстанса
  * компонента под готовые контексты.
  */
final case class NfRowR2(
                          crCtx        : MCommonReactCtx,
                          scCss        : ScCss,
                          onClickF     : MSc3IndexResp => DAction,
                        ) {

  type Props_t = MNodesFoundRowProps
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Props_t]) {

    /** Реакция на клик по одному элементу (ряд-узел). */
    private val _onNodeRowClickJsF = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCBf($) {
        propsProxy: Props =>
          onClickF( propsProxy.value.node.props )
      }
    }

    /** Рендер вёрстки компонента. */
    def render(propsProxy: Props): VdomElement = {
      val props = propsProxy.value
      val NodesCSS = ScCssStatic.Search.NodesFound

      // Рассчитать наименьшее расстояние от юзера до узла:
      val distanceMOpt = for {
        distanceToPoint <- Option( props.withDistanceToNull )

        locLl = MapsUtil.geoPoint2LatLng( distanceToPoint )

        distancesIter = {
          (
            // Поискать geoPoint среди основных данных узла.
            props.node.props.geoPoint
              .iterator
              .map( MapsUtil.geoPoint2LatLng ) #::
            // Перебрать гео-шейпы, попутно рассчитывая расстояние до центров:
            (for {
              nodeShape <- props.node.shapes.iterator
            } yield {
              // Поиск точки центра узла.
              nodeShape.centerPoint
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
            }) #::
            // Всё, иных вариантов-источников гео-точки нет.
            LazyList.empty
          )
            .iterator
            .flatten
            .map { locLl.distanceTo }
        }

        distance <- OptionUtil.maybe(distancesIter.nonEmpty)( distancesIter.min )

      } yield distance

      val p = props.node.props

      // Нельзя nodeId.get, т.к. могут быть узлы без id (по идее - максимум 1 узел в списке).
      val nodeId = p.idOrNameOrEmpty

      val isTag = p.ntype ==* MNodeTypes.Tag

      var rowRootCssAcc = props.searchCss.NodesFound.rowItemBgF(nodeId) ::
        Nil
      rowRootCssAcc ::= {
        if (isTag) NodesCSS.tagRow
        else NodesCSS.nodeRow
      }
      val rowRootCss = rowRootCssAcc
        .map(_.htmlClass)
        .mkString( HtmlConstants.SPACE )

      val listItemCss = new MuiListItemClasses {
        override val root = rowRootCss
      }

      val listItem = MuiListItem.component.withKey(nodeId)(
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
              override val root = Css.flat(
                NodesCSS.tagRowIconCont.htmlClass,
                scCss.fgColor.htmlClass,
              )
            }
            new MuiListItemIconProps {
              override val classes = cls
            }
          }(
            Mui.SvgIcons.LocalOffer {
              val css = new MuiSvgIconClasses {
                override val root = NodesCSS.tagRowIcon.htmlClass
              }
              new MuiSvgIconProps {
                override val classes = css
              }
            }()
          )
        },

        // Название узла
        p.name.whenDefinedEl { nodeName =>
          // Для тегов: они идут кашей, поэтому отступ между названием тега и иконкой уменьшаем.
          val rootCss = JsOptionUtil.maybeDefined(isTag)(
            Css.flat( NodesCSS.tagRowText.htmlClass, scCss.fgColor.htmlClass )
          )

          val theClasses = new MuiListItemTextClasses {
            override val root = rootCss

            override val primary = (
              ScCssStatic.thinText ::
                NodesCSS.tagRowTextPrimary ::
                props.searchCss.NodesFound.rowTextPrimaryF(nodeId) ::
                Nil
              ).toHtmlClass

            override val secondary = Css.flat(
              props.searchCss.NodesFound.rowTextSecondaryF(nodeId).htmlClass,
              ScCssStatic.thinText.htmlClass,
            )
          }

          val text2ndOpt = distanceMOpt
            .map { distanceM =>
              val distanceFmt = crCtx.messages( DistanceUtil.formatDistanceM(distanceM) )
              (HtmlConstants.`~` + distanceFmt): raw.React.Node
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

      MuiGrid {
        val css = JsOptionUtil.maybeDefined( !isTag ) {
          new MuiGridClasses {
            override val root = ScCssStatic.Search.NodesFound.gridRowAdn.htmlClass
          }
        }
        new MuiGridProps {
          override val item = true
          override val classes = css
        }
      }( listItem )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build


  def row(outerProxy: ModelProxy[_], rowProps: MNodesFoundRowProps) = {
    val nodeId = rowProps.node.props.idOrNameOrEmpty
    component.withKey(nodeId)( outerProxy.resetZoom(rowProps) )
  }

  def rows(rowsPropsProxy: ModelProxy[Seq[MNodesFoundRowProps]]): VdomElement = {
    React.Fragment(
      rowsPropsProxy.value.toVdomArray { rowProps =>
        row( rowsPropsProxy, rowProps )
      }
    )
  }

}
