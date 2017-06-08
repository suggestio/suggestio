package io.suggest.lk.adn.map.r

import diode.react.ModelProxy
import io.suggest.adv.rcvr.{IRcvrPopupNode, RcvrKey}
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.lk.adv.m.IRcvrPopupProps
import io.suggest.lk.adv.r.RcvrPopupBackendBaseR
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 14:23
  * Description: React-компонент попапа на ресивере.
  * @see [[http://ochrons.github.io/diode/usage/ApplicationModel.html]] ## Complex Access Patterns
  */

object LamRcvrPopupR {

  // Используется упрощённая модель для упрощённого коннекшена.
  type Props = ModelProxy[IRcvrPopupProps]


  /** Backend для react-sjs компонена. */
  protected[this] class Backend(override val $: BackendScope[Props, Unit])
    extends RcvrPopupBackendBaseR[IRcvrPopupProps, Unit] {

    /** Рендер тела одного узла. */
    override protected[this] def _renderNodeRow(node: IRcvrPopupNode, rcvrKey: RcvrKey, v: IRcvrPopupProps): ReactElement = {
      for (nodeName <- node.name) yield {
        <.div(
          ^.`class` := Css.flat( Css.Lk.LK_FIELD, Css.Lk.Adv.Geo.NODE_NAME_CONT ),

          // Есть какая-то инфа по текущему размещению на данном узле.
          // TODO Пока что рендерится неактивный чекбокс. Когда на сервере будет поддержка отмены размещений, то надо удалить эту ветвь if, оставив только тело else.
          <.label(
            ^.classSet1(
              Css.Lk.LK_FIELD_NAME,
              Css.Colors.GREEN -> true
            ),

            nodeName
          ),

          HtmlConstants.NBSP_STR,

          _infoBtn(rcvrKey)

        ): ReactElement
      }
    }

  }


  val component = ReactComponentB[Props]("LamRcvrPop")
    .stateless
    .renderBackend[Backend]
    .build


  def apply(props: Props) = component(props)

}
