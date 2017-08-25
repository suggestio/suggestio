package io.suggest.lk.adv.r

import diode.react.ModelProxy
import diode.react.ReactPot.potWithReact
import io.suggest.adv.rcvr.{IRcvrPopupNode, RcvrKey}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.adv.m.{IRcvrPopupProps, OpenNodeInfoClick}
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.maps.u.{MapIcons, MapsUtil}
import io.suggest.sjs.common.i18n.Messages
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback}
import react.leaflet.layer.LayerGroupR
import react.leaflet.popup.{LPopupPropsR, LPopupR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.06.17 16:58
  * Description: React-утиль для сборки rcvr-попапа
  *
  */

/** Трейт для сборки backend'а для компонента L-попапа над каким-либо ресивером.
  * В разных формах есть карты с ресиверами, но содержимое попапов в них отличается от формы к форме.
  *
  * @tparam PropsVal Тип внутри props ModelProxy.
  * @tparam State Внутреннее состояние компонента.
  */
trait RcvrPopupBackendBaseR[PropsVal <: IRcvrPopupProps, State] {

  /** Инстанс BackendScope. */
  val $: BackendScope[ModelProxy[PropsVal], State]


  /** Рендер маленькой кнопки-ссылки info для узла. */
  protected[this] def _infoBtn(rcvrKey: RcvrKey): VdomElement = {
    <.div(
      ^.`class` := Css.flat( Css.INFO_BLACK, Css.CLICKABLE ),
      ^.title   := Messages( MsgCodes.`Information` ),
      ^.onClick --> _rcvrInfoClick(rcvrKey),
      HtmlConstants.NBSP_STR
    )
  }

  protected[this] def _rcvrInfoClick(rk: RcvrKey): Callback = {
    dispatchOnProxyScopeCB($, OpenNodeInfoClick(rk))
  }

  /** Рендер тела одного узла. */
  protected[this] def _renderNodeRow(node: IRcvrPopupNode, rcvrKey: RcvrKey, v: PropsVal): VdomElement


  /** react render. */
  def render(props: ModelProxy[PropsVal]): VdomElement = {
    val v = props()

    // Погружаемся в состояние попапа ресивера, если оно задано...
    v.popupState.whenDefinedEl { state =>

      // Функция для рендера узла и его под-групп. Рекурсивна, т.к. в группах тоже могут быть узлы.
      // node -- узел для рендера
      // parentRcvrKeyRev обратный rcvrKey родительского узла или Nil для рендера top-level узла.
      def __renderNode(node: IRcvrPopupNode, parentRcvrKeyRev: List[String] = Nil): VdomElement = {
        val rcvrKeyRev = node.id :: parentRcvrKeyRev
        val rcvrKey = rcvrKeyRev.reverse

        <.div(
          ^.key := node.id,

          // Рендер всей необходимой инфы по узлу (например галочки текущего узла):
          _renderNodeRow(node, rcvrKey, v),

          // Рендер подгрупп узла
          node.subGroups
            .iterator
            .zipWithIndex
            .toVdomArray { case (subGrp, i) =>
              <.div(
                ^.key := subGrp.title.getOrElse(i.toString),

                // Заголовок текущей группы, если требуется...
                subGrp.title.whenDefined { grpTitle =>
                  <.h3( grpTitle )
                },

                // Рендер под-узлов данной группы узлов.
                subGrp.nodes.toVdomArray { subNode =>
                  __renderNode(subNode, rcvrKeyRev)
                }
              ): VdomElement
            }
        )
      }

      val latLng = MapsUtil.geoPoint2LatLng( state.latLng )

      LayerGroupR()(

        // Рендер маркера-крутилки на карте в ожидании рендера.
        MapIcons.preloaderLMarkerPot( v.popupResp, latLng ),

        // Рендер попапа. когда всё готово.
        v.popupResp.renderReady { resp =>
          LPopupR(
            new LPopupPropsR {
              override val position = latLng
            }
          )(
            <.div(
              ^.`class` := Css.Lk.Adv.Geo.RCVR_POPUP,

              resp.node.whenDefined { topNode =>
                __renderNode(topNode)
              }
            )
          )
        }

      )

    }               // popupState Option

  } // render()

}
