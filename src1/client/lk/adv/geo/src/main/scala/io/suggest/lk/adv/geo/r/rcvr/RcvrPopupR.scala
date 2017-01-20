package io.suggest.lk.adv.geo.r.rcvr

import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.lk.adv.geo.a.SetRcvrStatus
import io.suggest.lk.adv.geo.m.MRcvr
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.react.r.RangeYmdR
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement, ReactEventI}
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

      for (state <- v.popupState) yield {
        PopupR(
          position = LkAdvGeoFormUtil.geoPoint2LatLng( state.latLng )
        )(
          <.div(
            v.popupResp.renderEmpty {
              Messages("Please.wait")
            },
            v.popupResp.renderReady { resp =>
              for (g <- resp.groups.iterator) yield {
                // Значение key не суть важно, просто оно должно быть здесь.
                val groupId = g.groupId.getOrElse("0")
                <.div(
                  ^.key := groupId,
                  for (gname <- g.nameOpt) yield {
                    <.h3(gname)
                  },
                  for (n <- g.nodes.iterator) yield {
                    val rcvrKey = RcvrKey(
                      from = state.nodeId,
                      to = n.nodeId,
                      groupId = g.groupId
                    )
                    val name = n.nameOpt.getOrElse[String]("???")

                    <.div(
                      ^.key := rcvrKey.toString,
                      ^.`class` := Css.Lk.LK_FIELD,

                      if (n.dateRange.nonEmpty) {
                        // Есть какая-то инфа по текущему размещению на данном узле.
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

                          name,

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
                            Css.Colors.RED -> (!checked && !n.isCreate),
                            Css.Colors.GREEN -> (checked && n.isCreate)
                          ),
                          <.input(
                            ^.`type` := "checkbox",
                            ^.checked := checked,
                            ^.onChange ==> rcvrCheckboxChanged(rcvrKey)
                          ),
                          <.span,
                          name
                        )
                      }

                    )
                  }   // n in g.nodes
                )
              }       // g in groups
            }
          )           // Popup div
        )
      }               // popupState

    } // render()

  }


  val component = ReactComponentB[Props]("RcvrPop")
    .renderBackend[Backend]
    .build


  def apply(props: Props) = component(props)

}
