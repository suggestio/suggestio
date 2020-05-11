package io.suggest.lk.adv.geo.r.rcvr

import diode.react.{ModelProxy, ReactConnectProps}
import io.suggest.adv.rcvr.{IRcvrPopupNode, RcvrKey}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.lk.adv.geo.m.{MRcvr, SetRcvrStatus}
import io.suggest.lk.adv.r.RcvrPopupBackendBaseR
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.r.RangeYmdR
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 14:23
  * Description: React-компонент попапа на ресивере.
  * @see [[http://ochrons.github.io/diode/usage/ApplicationModel.html]] ## Complex Access Patterns
  */

object RcvrPopupR {

  type Props_t = MRcvr
  type Props = ModelProxy[Props_t]


  /** Backend для react-sjs компонена. */
  protected[this] class Backend(override val $: BackendScope[Props, Unit])
    extends RcvrPopupBackendBaseR[MRcvr, Unit] {

    /** Реакция на изменение флага узла-ресивера в попапе узла. */
    private def _rcvrCheckboxChanged(rk: RcvrKey)(e: ReactEventFromInput): Callback = {
      val checked = e.target.checked
      dispatchOnProxyScopeCB( $, SetRcvrStatus(rk, checked) )
    }


    /** Рендер тела одного узла. */
    override protected[this] def _renderNodeRow(node: IRcvrPopupNode, rcvrKey: RcvrKey, v: MRcvr): VdomElement = {
      // Рендер галочки текущего узла, если она задана.
      node.checkbox.whenDefinedEl { n =>
        val nodeName = node.name.getOrElse( node.id )

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
            val checked = v.rcvrsMap.getOrElse(rcvrKey, n.checked)

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

          _infoBtn(rcvrKey)

        ): VdomElement
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build


  private def _apply(props: Props) = component(props)
  val apply: ReactConnectProps[Props_t] = _apply

}
