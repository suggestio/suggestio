package io.suggest.sc.search.v

import diode.react.ModelProxy
import io.suggest.i18n.MsgCodes
import io.suggest.sc.search.m.{MScSearchText, SearchTextChanged, SearchTextFocus}
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.i18n.Messages
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 12:50
  * Description: React-компонент поиского поля.
  * Скорее всего, его можно использовать через .wrap() вместо .connect.
  */
class STextR( getScCssF: GetScCssF ) {

  type Props = ModelProxy[MScSearchText]


  class Backend( $: BackendScope[Props, Unit] ) {

    /** Происходит ввод текста в input. */
    private def _onInput(e: ReactEventFromInput): Callback = {
      val text = e.target.value
      dispatchOnProxyScopeCB($, SearchTextChanged(text))
    }

    private def _onFocusChange(focused: Boolean): Callback = {
      dispatchOnProxyScopeCB($, SearchTextFocus(focused))
    }

    def render(propsProxy: Props): VdomElement = {
      val scCss = getScCssF()
      val CSS = scCss.Search.SearchBar

      val p = propsProxy.value

      <.div(
        CSS.bar,

        // Рендер текстового поля с input'ом.
        <.div(
          CSS.Field.field,

          // Рендерить __active, когда происходит ввод данных.
          if (p.focused) {
            CSS.Field.active
          } else {
            EmptyVdom
          },

          <.div(
            CSS.Field.fieldWrapper,
            <.input(
              CSS.Field.input,
              ^.placeholder := Messages( MsgCodes.`Quick.search.for.offers` ),
              ^.onChange ==> _onInput,
              ^.onFocus  --> _onFocusChange(true),
              ^.onBlur   --> _onFocusChange(false),
              ^.value     := p.query
            )
          )

        )

      )
    }

  }


  val component = ScalaComponent.builder[Props]("SText")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(scSearchTextOptProxy: Props) = component( scSearchTextOptProxy )

}
