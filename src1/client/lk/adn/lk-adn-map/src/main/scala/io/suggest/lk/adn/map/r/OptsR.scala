package io.suggest.lk.adn.map.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adn.mapf.opts.MLamOpts
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.adn.map.m.{IApply1BoolTo, ILamAction, OnAdvsMapChanged, OnGeoLocChanged}
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement, ReactEventI}
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.sjs.common.spa.OptFastEq.PlainVal
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.i18n.Messages

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.05.17 10:27
  * Description: React-компонент выбора опций размещения узла на карте.
  */
object OptsR {

  type Props = ModelProxy[MLamOpts]

  protected case class State(
                              onAdvMapSomeC : ReactConnectProxy[Some[Boolean]],
                              onGeoLocSomeC : ReactConnectProxy[Some[Boolean]]
                            )


  protected class Backend($: BackendScope[Props, State]) {

    private def _notifyChecked[T <: ILamAction](model: IApply1BoolTo[T])(e: ReactEventI): Callback = {
      dispatchOnProxyScopeCB($, model(e.target.checked))
    }

    def render(s: State): ReactElement = {
      val scb = <.span(
        ^.`class` := Css.Input.STYLED_CHECKBOX
      )
      val inputTypeCheckbox =
        ^.`type`     := HtmlConstants.Input.checkbox

      <.div(

        <.br,

        // Галочка размещения на карте рекламодателей
        <.label(
          ^.`class` := Css.CLICKABLE,

          s.onAdvMapSomeC { onAdvMapSomeProxy =>
            <.input(
              inputTypeCheckbox,
              ^.checked    := onAdvMapSomeProxy().get,
              ^.onChange  ==> _notifyChecked(OnAdvsMapChanged)
            )
          },
          scb,

          Messages( MsgCodes.`Publish.node.on.adv.map` )
        ),

        <.br,
        <.br,

        // Галочка размещения на карте геолокации.
        <.label(
          s.onGeoLocSomeC { onGeoLocSomeProxy =>
            <.input(
              inputTypeCheckbox,
              ^.checked    := onGeoLocSomeProxy().get,
              ^.onChange  ==> _notifyChecked(OnGeoLocChanged)
            )
          },
          scb,

          Messages( MsgCodes.`Users.geo.location.capturing` )
        )

      )
    }

  }


  val component = ReactComponentB[Props]("Opts")
    .initialState_P { propsProxy =>
      State(
        onAdvMapSomeC = propsProxy.connect { props =>
          Some( props.onAdvMap )
        }(PlainVal),
        onGeoLocSomeC = propsProxy.connect { props =>
          Some( props.onGeoLoc )
        }(PlainVal)
      )
    }
    .renderBackend[Backend]
    .build


  def apply(optsProxy: Props) = component(optsProxy)

}
