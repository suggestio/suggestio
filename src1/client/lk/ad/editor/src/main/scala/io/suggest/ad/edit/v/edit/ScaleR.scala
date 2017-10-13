package io.suggest.ad.edit.v.edit

import diode.FastEq
import diode.react.ModelProxy
import io.suggest.ad.edit.m.SetScale
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.dev.MSzMult
import io.suggest.i18n.MsgCodes
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.{BackendScope, Callback, ReactEventFromInput, ScalaComponent}
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.sjs.common.i18n.Messages
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.17 18:56
  * Description: Компонент управления масштабом в виде select'а.
  */
class ScaleR {

  /** Класс модели пропертисов компонента.
    *
    * @param variants Доступные варианты.
    * @param current Текущий выбранный вариант.
    */
  case class PropsVal(
                       current  : MSzMult,
                       variants : Seq[MSzMult]
                     )

  /** Поддержка FastEq для инстансов [[PropsVal]]. */
  implicit object ScaleRPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.current ===* b.current) &&
        (a.variants ===* b.variants)
    }
  }

  type Props = ModelProxy[Option[PropsVal]]


  /** Ядро компонента. */
  class Backend($: BackendScope[Props, Unit]) {

    /** Реакция на смену масштаба. */
    private def onScaleChange(e: ReactEventFromInput): Callback = {
      val newValue = e.target.value
      $.props >>= { propsOptProxy =>
        // Распарсить значение масштаба в процентах:
        val szMultPct = newValue.toInt
        // Найти выбранный вариант в props и отправить сигнал с ним наверх.
        propsOptProxy
          .value
          .iterator
          .flatMap(_.variants)
          .find(_.multPct ==* szMultPct)
          .fold(Callback.empty) { szMult =>
            propsOptProxy.dispatchCB( SetScale(szMult) )
          }
      }
    }

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { props =>
        <.div(
          ^.`class`  := Css.Input.INPUT,

          <.h1(
            Messages( MsgCodes.`Scale` )
          ),

          <.select(
            ^.`type`   := HtmlConstants.Input.select,
            ^.value    := props.current.multPct.toString,
            ^.onChange ==> onScaleChange,

            props.variants.toVdomArray { szMult =>
              val code = szMult.multPct.toString
              <.option(
                ^.key   := code,
                ^.value := code,
                szMult.multPct.pct
              )
            }
          )
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("Scale")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(propsOptProxy: Props) = component(propsOptProxy)

}
