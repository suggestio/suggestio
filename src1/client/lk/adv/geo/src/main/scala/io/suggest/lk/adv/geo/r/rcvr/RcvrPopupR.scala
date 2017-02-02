package io.suggest.lk.adv.geo.r.rcvr

import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.adv.rcvr.{IRcvrPopupNode, RcvrKey}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.lk.adv.geo.a.SetRcvrStatus
import io.suggest.lk.adv.geo.m.MRcvr
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.react.r.RangeYmdR
import io.suggest.sjs.common.vm.spa.PreLoaderLk
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement, ReactEventI}
import react.leaflet.layer.LayerGroupR
import react.leaflet.marker.{MarkerPropsR, MarkerR}
import react.leaflet.popup.PopupR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 14:23
  * Description: React-компонент попапа на ресивере.
  * @see [[http://ochrons.github.io/diode/usage/ApplicationModel.html]] ## Complex Access Patterns
  */

object RcvrPopupR {

  type Props = ModelProxy[MRcvr]


  /** Backend для react-sjs компонена. */
  protected[this] class Backend($: BackendScope[Props, _]) {

    /** Реакция на изменение флага узла-ресивера в попапе узла. */
    def rcvrCheckboxChanged(rk: RcvrKey)(e: ReactEventI): Callback = {
      val checked = e.target.checked
      $.props >>= { props =>
        props.dispatchCB( SetRcvrStatus(rk, checked) )
      }
    }


    /** react render. */
    def render(props: Props): ReactElement = {
      val v = props()

      // Погружаемся в состояние попапа ресивера, если оно задано...
      for (state <- v.popupState) yield {

        // Функция для рендера узла и его под-групп. Рекурсивна, т.к. в группах тоже могут быть узлы.
        // node -- узел для рендера
        // parentRcvrKeyRev обратный rcvrKey родительского узла или Nil для рендера top-level узла.
        def __renderNode(node: IRcvrPopupNode, parentRcvrKeyRev: List[String] = Nil): ReactElement = {
          val rcvrKeyRev = node.nodeId :: parentRcvrKeyRev
          val rcvrKey = rcvrKeyRev.reverse

          <.div(
            ^.key := node.nodeId,

            // Рендер галочки текущего узла, если она задана.
            for (n <- node.checkbox) yield {
              <.div(
                //^.key     := rcvrKey.mkString("."),
                ^.`class` := Css.Lk.LK_FIELD,

                if (n.dateRange.nonEmpty) {
                  // Есть какая-то инфа по текущему размещению на данном узле.
                  // TODO Пока что рендерится неактивный чекбокс. Когда на сервере будет поддержка отмены размещений, то надо удалить эту ветвь if, оставив только тело else.
                  <.label(
                    ^.classSet1(
                      Css.Lk.LK_FIELD_NAME,
                      Css.Colors.GREEN -> true
                    ),
                    <.input(
                      ^.`type`   := "checkbox",
                      ^.checked  := true,
                      ^.disabled := true
                    ),
                    <.span,

                    n.name,

                    HtmlConstants.SPACE,
                    RangeYmdR(
                      RangeYmdR.Props(
                        capFirst    = true,
                        rangeYmdOpt = n.dateRange,
                        Messages    = Messages
                      )
                    )
                  )
                } else {
                  // Нет текущего размещения. Рендерить активный рабочий чекбокс.
                  val checked = v.rcvrsMap.getOrElse(rcvrKey, n.checked)

                  <.label(
                    ^.classSet1(
                      Css.Lk.LK_FIELD_NAME,
                      Css.Colors.RED   -> (!checked && !n.isCreate),
                      Css.Colors.GREEN -> (checked && n.isCreate)
                    ),
                    <.input(
                      ^.`type`    := "checkbox",
                      ^.checked   := checked,
                      ^.onChange ==> rcvrCheckboxChanged(rcvrKey)
                    ),
                    <.span,
                    n.name
                  )
                }

              )
            },

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

        val latLng = LkAdvGeoFormUtil.geoPoint2LatLng( state.latLng )


        LayerGroupR()(

          // Рендер маркера-крутилки на карте в ожидании рендера.
          v.popupResp.renderPending { _: Int =>
            for (iconUrl <- PreLoaderLk.PRELOADER_IMG_URL) yield {
              val icon1 = LkAdvGeoFormUtil.pendingIcon(iconUrl, 16)
              MarkerR(
                new MarkerPropsR {
                  override val position  = latLng
                  override val draggable = false
                  override val clickable = false
                  override val icon      = icon1
                  override val title     = Messages("Please.wait")
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


  val component = ReactComponentB[Props]("RcvrPop")
    .renderBackend[Backend]
    .build


  def apply(props: Props) = component(props)

}
