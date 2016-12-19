package io.suggest.lk.adv.geo.r.rcvr

import diode.FastEq
import diode.data.Pot
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.suggest.adv.geo._
import io.suggest.css.Css
import io.suggest.lk.adv.geo.a.SetRcvrStatus
import io.suggest.lk.adv.geo.u.LkAdvGeoFormUtil
import io.suggest.lk.vm.LkMessagesWindow.Messages
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

  type Props = ModelProxy[PropsVal]


  /** Содержимое ModelProxy. */
  case class PropsVal(
                       rcvrsMap   : RcvrsMap_t,
                       state      : Option[MRcvrPopupState],
                       resp       : Pot[MRcvrPopupResp]
                     )



  /** Поддержка быстрого сравнения содержимого сложного контейнера для diode circuit. */
  implicit object PropsValEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.rcvrsMap eq b.rcvrsMap) &&
        (a.state eq b.state) &&
        (a.resp eq b.resp)
    }
  }


  /** Backend для react-sjs компонена. */
  protected[this] class Backend($: BackendScope[Props, _]) {

    def _date(msg: String, dateFmt: MDateFormatted) = {
      <.span(
        ^.title := dateFmt.dow,
        Messages(msg),
        dateFmt.date
      )
    }


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
      v.state.fold[ReactElement](null) { state =>
        PopupR(
          position = LkAdvGeoFormUtil.geoPoint2LatLng( state.latLng )
        )(
          <.div(
            v.resp.renderEmpty {
              Messages("Please.wait")
            },
            v.resp.renderReady { resp =>
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

                      // TODO Произвести дедубликацию кода, т.к. обе ветки очень похожи.
                      if (n.dateRange.size == 2) {
                        // Есть какая-то инфа по текущему размещению на данном узле.
                        <.label(
                          ^.classSet1(
                            Css.Lk.LK_FIELD_NAME,
                            Css.Colors.GREEN -> true
                          ),
                          <.input(
                            ^.`type` := "checkbox",
                            ^.checked := true,
                            ^.disabled := true
                          ),
                          <.span,
                          name,
                          " ",
                          _date("from._date", n.dateRange.head),
                          " ",
                          _date("till._date", n.dateRange.last)
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
                  }
                )
              }
            }
          ) // Popup div
        )
      }
    }

  }


  val component = ReactComponentB[Props]("RcvrPop")
    .renderBackend[Backend]
    .build


  def apply(props: Props) = component(props)

}
