package io.suggest.lk.adv.r

import diode.react.ModelProxy
import diode.react.ReactPot.potWithReact
import io.suggest.adv.rcvr.{IRcvrPopupNode, RcvrKey}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.adv.m.{IRcvrPopupProps, OpenNodeInfoClick}
import japgolly.scalajs.react.{BackendScope, Callback, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.maps.u.{MapIcons, MapsUtil}
import io.suggest.sjs.common.i18n.Messages
import io.suggest.sjs.common.vm.spa.LkPreLoader
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import react.leaflet.layer.LayerGroupR
import react.leaflet.marker.{MarkerPropsR, MarkerR}
import react.leaflet.popup.PopupR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.06.17 16:58
  * Description: Трейт для сборки backend'а для компонента L-попапа над каким-либо ресивером.
  * В разных формах есть карты с ресиверами, но содержимое попапов в них отличается от формы к форме.
  *
  * @tparam PropsVal Тип внутри props ModelProxy.
  * @tparam State Внутреннее состояние компонента.
  */

trait RcvrPopupBackendBaseR[PropsVal <: IRcvrPopupProps, State] {

  /** Инстанс BackendScope. */
  val $: BackendScope[ModelProxy[PropsVal], State]


  protected[this] def _rcvrInfoClick(rk: RcvrKey): Callback = {
    dispatchOnProxyScopeCB($, OpenNodeInfoClick(rk))
  }

  /** Рендер маленькой кнопки-ссылки info для узла. */
  protected[this] def _infoBtn(rcvrKey: RcvrKey): ReactElement = {
    <.div(
      ^.`class` := Css.flat( Css.INFO_BLACK, Css.CLICKABLE ),
      ^.title   := Messages( MsgCodes.`Information` ),
      ^.onClick --> _rcvrInfoClick(rcvrKey),
      HtmlConstants.NBSP_STR
    )
  }

  /** Рендер тела одного узла. */
  protected[this] def _renderNodeRow(node: IRcvrPopupNode, rcvrKey: RcvrKey, v: PropsVal): ReactElement


  /** react render. */
  def render(props: ModelProxy[PropsVal]): ReactElement = {
    val v = props()

    // Погружаемся в состояние попапа ресивера, если оно задано...
    for (state <- v.popupState) yield {

      // Функция для рендера узла и его под-групп. Рекурсивна, т.к. в группах тоже могут быть узлы.
      // node -- узел для рендера
      // parentRcvrKeyRev обратный rcvrKey родительского узла или Nil для рендера top-level узла.
      def __renderNode(node: IRcvrPopupNode, parentRcvrKeyRev: List[String] = Nil): ReactElement = {
        val rcvrKeyRev = node.id :: parentRcvrKeyRev
        val rcvrKey = rcvrKeyRev.reverse

        <.div(
          ^.key := node.id,

          // Рендер всей необходимой инфы по узлу (например галочки текущего узла):
          _renderNodeRow(node, rcvrKey, v),

          // Рендер подгрупп узла
          for {
            (subGrp, i) <- node.subGroups.iterator.zipWithIndex
          } yield {
            <.div(
              ^.key := subGrp.title.getOrElse(i.toString),

              // Заголовок текущей группы, если требуется...
              for (grpTitle <- subGrp.title) yield {
                <.h3( grpTitle )
              },

              // Рендер под-узлов данной группы узлов.
              for (subNode <- subGrp.nodes) yield {
                __renderNode(subNode, rcvrKeyRev)
              }
            )
          }

        )
      }

      val latLng = MapsUtil.geoPoint2LatLng( state.latLng )

      LayerGroupR()(

        // Рендер маркера-крутилки на карте в ожидании рендера.
        v.popupResp.renderPending { _: Int =>
          for (iconUrl <- LkPreLoader.PRELOADER_IMG_URL) yield {
            val icon1 = MapIcons.pendingIcon(iconUrl, 16)
            MarkerR(
              new MarkerPropsR {
                override val position  = latLng
                override val draggable = false
                override val clickable = false
                override val icon      = icon1
                override val title     = Messages( MsgCodes.`Please.wait` )
              }
            )()
          }
        },

        // Рендер попапа. когда всё готово.
        v.popupResp.renderReady { resp =>
          PopupR(
            position = latLng
          )(
            <.div(
              ^.`class` := Css.Lk.Adv.Geo.RCVR_POPUP,

              for (topNode <- resp.node) yield {
                __renderNode(topNode)
              }
            )
          )
        }

      )

    }               // popupState

  } // render()

}
