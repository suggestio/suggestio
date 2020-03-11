package io.suggest.sc.v.dia.settings

import com.materialui.{MuiListItem, MuiListItemProps, MuiListItemText}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants._
import io.suggest.css.Css
import io.suggest.dev.MTlbr
import io.suggest.react.ReactCommonUtil
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sc.m.{MScRoot, UpdateUnsafeScreenOffsetBy}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ScalaComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.06.18 10:36
  * Description: Отладочный компонент для ручного выставления сдвига безопасной области экрана в пикселях.
  */
class UnsafeOffsetSettingR {

  type Props_t = MScRoot
  type Props = ModelProxy[Props_t]

  case class State(
                    isEnabledSomeC    : ReactConnectProxy[Some[Boolean]],
                    mtbrOptC          : ReactConnectProxy[MTlbr],
                  )

  class Backend($: BackendScope[Props, State]) {

    private def _onIncDecClick(incDecBy: Int): Callback =
      dispatchOnProxyScopeCB( $, UpdateUnsafeScreenOffsetBy(incDecBy) )

    def render(s: State): VdomElement = {
      // Ссылка на вход или на личный кабинет
      lazy val content = MuiListItem(
        new MuiListItemProps {
          override val button = false
        }
      )(
        MuiListItemText()(
          <.span(
            "Unsafe offset",

            <.span(
              ^.`class` := Css.Floatt.RIGHT,

              // Уменьшение
              <.a(
                ^.onClick --> _onIncDecClick(-1),
                LESSER,
                MINUS
              ),

              NBSP_STR,

              // Увеличение
              <.a(
                s.mtbrOptC { mtbrOptProxy =>
                  <.span(
                    mtbrOptProxy.value.toString,
                  )
                },
                NBSP_STR,
                ^.onClick --> _onIncDecClick(1),
                PLUS,
                GREATER
              )
            )
          )
        )
      )

      s.isEnabledSomeC { isEnabledSomeProxy =>
        ReactCommonUtil.maybeEl( isEnabledSomeProxy.value.value )( content )
      }
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        isEnabledSomeC = propsProxy.connect { mroot =>
          OptionUtil.SomeBool( mroot.internals.conf.debug )
        },
        mtbrOptC = propsProxy.connect( _.dev.screen.info.unsafeOffsets ),
      )
    }
    .renderBackend[Backend]
    .build

  def apply(mtlbrOptProxy: Props) = component( mtlbrOptProxy )

}
