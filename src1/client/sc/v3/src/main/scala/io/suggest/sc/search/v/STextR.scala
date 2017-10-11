package io.suggest.sc.search.v

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.i18n.MsgCodes
import io.suggest.sc.search.m.MScSearchText
import io.suggest.sc.styl.GetScCssF
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.sjs.common.i18n.Messages
import io.suggest.spa.OptFastEq.Plain

import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.07.17 12:50
  * Description: React-компонент поиского поля.
  * Скорее всего, его можно использовать через .wrap() вместо .connect.
  */
class STextR( getScCssF: GetScCssF ) {

  type Props = ModelProxy[Option[MScSearchText]]

  case class State(
                    textOptC: ReactConnectProxy[Option[String]]
                  )


  class Backend( $: BackendScope[Props, State] ) {

    def render(s: State): VdomElement = {
      val scCss = getScCssF()
      val CSS = scCss.Search.SearchBar

      <.div(
        CSS.bar,

        // Рендер текстового поля с input'ом.
        s.textOptC { textOptProxy =>
          val textOpt = textOptProxy()

          <.div(
            CSS.Field.field,

            // Рендерить __active, когда происходит ввод данных.
            textOpt.whenDefined { _ =>
              CSS.Field.active
            },

            <.div(
              CSS.Field.fieldWrapper,
              <.input(
                CSS.Field.input,
                ^.placeholder := Messages( MsgCodes.`Quick.search.for.offers` ),
                ^.value := textOpt.getOrElse("")
              )
            )

          )
        }

      )
    }

  }


  val component = ScalaComponent.builder[Props]("SText")
    .initialStateFromProps { propsProxy =>
      State(
        textOptC = propsProxy.connect( _.map(_.query) )
      )
    }
    .renderBackend[Backend]
    .build

  def apply(scSearchTextOptProxy: Props) = component( scSearchTextOptProxy )

}
