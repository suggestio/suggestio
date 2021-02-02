package io.suggest.lk.adv.geo.r.rcvr

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adv.rcvr.{MRcvrPopupNode, MRcvrPopupResp, RcvrKey}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.adv.geo.m.{MRcvr, SetRcvrStatus}
import io.suggest.lk.adv.m.OpenNodeInfoClick
import io.suggest.maps.m.MRcvrPopupS
import io.suggest.maps.u.MapIcons
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.r.RangeYmdR
import io.suggest.spa.FastEqUtil
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ScalaComponent}
import org.js.react.leaflet.{LayerGroup, Popup, PopupProps}
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 14:23
  * Description: React-компонент попапа на ресивере.
  * @see [[http://ochrons.github.io/diode/usage/ApplicationModel.html]] ## Complex Access Patterns
  */

final class RcvrPopupR {

  type Props_t = MRcvr
  type Props = ModelProxy[Props_t]


  case class State(
                    pendingAtC        : ReactConnectProxy[Option[MRcvrPopupS]],
                    popupContentC     : ReactConnectProxy[MRcvr],
                    shownAtOptC       : ReactConnectProxy[Option[MRcvrPopupS]],
                  )

  /** Backend для react-sjs компонена. */
  class Backend( $: BackendScope[Props, State] ) {

    private val _stopPropagationCB = ReactCommonUtil.stopPropagationCB _


    /** Реакция на изменение флага узла-ресивера в попапе узла. */
    private def _rcvrCheckboxChanged(rk: RcvrKey)(e: ReactEventFromInput): Callback = {
      val checked = e.target.checked
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SetRcvrStatus(rk, checked) ) >>
      _stopPropagationCB(e)
    }

    private def _rcvrInfoClick(rk: RcvrKey): Callback = {
      dispatchOnProxyScopeCB($, OpenNodeInfoClick(rk))
    }


    /** Рендер маленькой кнопки-ссылки info для узла. */
    private def _infoBtn(rcvrKey: RcvrKey): VdomElement = {
      <.div(
        ^.`class` := Css.flat( Css.INFO_BLACK, Css.CLICKABLE ),
        ^.title   := Messages( MsgCodes.`Information` ),
        ^.onClick --> _rcvrInfoClick(rcvrKey),
        HtmlConstants.NBSP_STR
      )
    }


    /** Рендер тела одного узла. */
    private def _renderNodeRow(node: MRcvrPopupNode, rcvrKey: RcvrKey, rcvr: MRcvr): VdomElement = {
      // Рендер галочки текущего узла, если она задана.
      node.checkbox.whenDefinedEl { n =>
        val nodeName = node.name getOrElse node.id

        <.div(
          ^.`class` := Css.flat( Css.Lk.LK_FIELD, Css.Lk.Adv.Geo.NODE_NAME_CONT ),

          if (n.dateRange.nonEmpty) {
            // Есть какая-то инфа по текущему размещению на данном узле.
            // TODO Пока что рендерится неактивный чекбокс. Когда на сервере будет поддержка отмены размещений, то надо удалить эту ветвь if, оставив только тело else.
            <.label(
              ^.classSet1(
                Css.Lk.LK_FIELD_NAME,
                Css.Colors.GREEN -> true
              ),
              <.input(
                ^.`type`   := HtmlConstants.Input.checkbox,
                ^.checked  := true,
                ^.disabled := true
              ),
              <.span,

              nodeName,

              HtmlConstants.SPACE,
              RangeYmdR(
                RangeYmdR.Props(
                  capFirst    = true,
                  rangeYmdOpt = n.dateRange
                )
              )
            )
          } else {
            // Нет текущего размещения. Рендерить активный рабочий чекбокс.
            val checked = rcvr.rcvrsMap.getOrElse(rcvrKey, n.checked)

            <.label(
              ^.classSet1(
                Css.flat( Css.Lk.LK_FIELD_NAME, Css.CLICKABLE ),
                Css.Colors.RED   -> (!checked && !n.isCreate),
                Css.Colors.GREEN -> (checked && n.isCreate)
              ),
              <.input(
                ^.`type`    := HtmlConstants.Input.checkbox,
                ^.checked   := checked,
                ^.onChange ==> _rcvrCheckboxChanged(rcvrKey)
              ),
              <.span,
              nodeName
            )
          },

          HtmlConstants.NBSP_STR,

          _infoBtn(rcvrKey),

        ): VdomElement
      }
    }


    /** react render. */
    def render(props: Props, s: State): VdomElement = {

      val popupContent = s.popupContentC { rcvrProxy =>
        // Функция для рендера узла и его под-групп. Рекурсивна, т.к. в группах тоже могут быть узлы.
        // node -- узел для рендера
        // parentRcvrKeyRev обратный rcvrKey родительского узла или Nil для рендера top-level узла.
        val rcvr = rcvrProxy.value

        def __renderNode(node: MRcvrPopupNode, parentRcvrKeyRev: List[String] = Nil): VdomElement = {
          val rcvrKeyRev = node.id :: parentRcvrKeyRev
          val rcvrKey = rcvrKeyRev.reverse

          <.div(
            ^.key := node.id,

            // Рендер всей необходимой инфы по узлу (например галочки текущего узла):
            _renderNodeRow(node, rcvrKey, rcvr),

            // Рендер подгрупп узла
            node.subGroups
              .iterator
              .zipWithIndex
              .toVdomArray { case (subGrp, i) =>
                <.div(
                  ^.key := subGrp.title getOrElse i.toString,

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

        <.div(
          ^.`class` := Css.Lk.Adv.Geo.RCVR_POPUP,

          rcvr.popupResp
            .toOption
            .flatMap(_.node)
            .whenDefinedEl { topNode =>
              __renderNode(topNode)
            },
        )
      }

      LayerGroup()(

        // Рендер маркера-крутилки на карте в ожидании рендера.
        s.pendingAtC {
          _.value.whenDefinedEl { rcvrPopupPendingS =>
            MapIcons.preloaderLMarker( rcvrPopupPendingS.leafletLatLng )
          }
        },

        // Рендер попапа. когда всё готово.
        // TODO Pot.renderReady, renderPending: react тут начала выбрасывать ошибку, что react element expected, а мы шлём ей массивы.
        s.shownAtOptC { rcvrPopupProxy =>
          rcvrPopupProxy.value.whenDefinedEl { resp =>
            Popup(
              new PopupProps {
                override val position = resp.leafletLatLng
                override val minWidth = 250
                override val maxWidth = 350
              }
            )(
              popupContent,
            )
          }
        },

      )

    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(

        pendingAtC = propsProxy.connect { props =>
          props
            .popupState
            .filter( _ => props.popupResp.isPending )
        },

        popupContentC = propsProxy.connect( identity ) {
          FastEqUtil[MRcvr] {
            val respFeq = FastEqUtil.PotAsOptionFastEq( FastEqUtil.AnyRefFastEq[MRcvrPopupResp] )
            (a, b) =>
              respFeq.eqv( a.popupResp, b.popupResp ) &&
              (a.rcvrsMap ===* b.rcvrsMap)
          }
        },

        shownAtOptC = propsProxy.connect( _.popupState ),

      )
    }
    .renderBackend[Backend]
    .build

}
