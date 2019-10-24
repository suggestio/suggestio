package io.suggest.lk.r.color

import diode.FastEq
import diode.react.{ModelProxy, ReactConnectProps, ReactConnectProxy}
import io.suggest.color.{IColorPickerMarker, MColorData}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.lk.m.ColorCheckboxChange
import io.suggest.lk.r.{LkCheckBoxR, LkCss}
import io.suggest.react.ReactCommonUtil
import io.suggest.spa.OptFastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 11:38
  * Description: React-компонент с галочкой выбора цвета.
  */
class ColorCheckBoxR(
                      lkCss           : LkCss,
                      val lkCheckBoxR : LkCheckBoxR,
                      val colorBtnR   : ColorBtnR,
                    ) {

  /** Модель пропертисов этого компонента.
    *
    * @param color Цвет.
    *              None значит прозрачный.
    */
  case class PropsVal(
                       color          : Option[MColorData],
                       label          : VdomNode,
                       marker         : Option[IColorPickerMarker],
                     )
  implicit object ColorCheckBoxPropsValFastEq extends FastEq[PropsVal] {
    override def eqv(a: PropsVal, b: PropsVal): Boolean = {
      (a.color ===* b.color) &&
      (a.label eq b.label) &&
      (a.marker ===* b.marker)
    }
  }

  type Props_t = Option[PropsVal]
  type Props = ModelProxy[Props_t]

  case class State(
                    isVisibleSomeC      : ReactConnectProxy[Some[Boolean]],
                    isCheckedSomeC      : ReactConnectProxy[Some[Boolean]],
                    colorBtnPropsOptC   : ReactConnectProxy[colorBtnR.Props_t],
                  )

  private lazy val _colorBtnCssOpt = Option( lkCss.ColorOptPicker.colorRound.htmlClass )

  class Backend($: BackendScope[Props, State]) {

    def render(propsOptProxy: Props, s: State): VdomElement = {
      lazy val allDiv = {
        // Галочка для переключения между прозрачным фоном и заливкой цветом
        val onOffCheckBox = propsOptProxy.wrap { propsOpt =>
          for (props <- propsOpt) yield {
            lkCheckBoxR.PropsVal(
              label     = props.label,
              checked   = props.color.nonEmpty,
              onChange  = { isChecked2 =>
                ColorCheckboxChange(isChecked2, marker = props.marker)
              },
            )
          }
        }( lkCheckBoxR.apply )(implicitly, OptFastEq.Wrapped(lkCheckBoxR.LkCheckBoxMinimalFastEq))

        // Кнопка color-picker, когда включена галочка.
        lazy val colorPickBtn = <.span(
          ^.`class` := Css.flat(Css.CLICKABLE, Css.Display.INLINE_BLOCK),
          ^.onClick ==> ReactCommonUtil.stopPropagationCB,

          HtmlConstants.NBSP_STR,
          HtmlConstants.NBSP_STR,

          // Кнопка активации color-picker'а:
          s.colorBtnPropsOptC { colorBtnR.apply },
        )

        <.div(
          lkCss.ColorOptPicker.container,

          onOffCheckBox,

          s.isCheckedSomeC { isCheckedSomeProxy =>
            ReactCommonUtil.maybeEl( isCheckedSomeProxy.value.value ) {
              colorPickBtn
            }
          }
        )
      }

      // В не-контент редакторах вообще эта строка скрыта целиком.
      s.isVisibleSomeC { isVisibleSomeProxy =>
        ReactCommonUtil.maybeEl( isVisibleSomeProxy.value.value ) {
          allDiv
        }
      }

    }
  }


  val component = ScalaComponent.builder[Props](getClass.getSimpleName)
    .initialStateFromProps { propsOptProxy =>
      State(
        isVisibleSomeC = propsOptProxy.connect { propsOpt =>
          OptionUtil.SomeBool( propsOpt.nonEmpty )
        }( FastEq.AnyRefEq ),

        isCheckedSomeC = propsOptProxy.connect { propsOpt =>
          OptionUtil.SomeBool( propsOpt.exists(_.color.nonEmpty) )
        }( FastEq.AnyRefEq ),

        colorBtnPropsOptC = propsOptProxy.connect { propsOpt =>
          for (props <- propsOpt; mcd <- props.color) yield {
            colorBtnR.PropsVal(
              color     = mcd,
              cssClass  = _colorBtnCssOpt,
              marker    = props.marker,
            )
          }
        }( OptFastEq.Wrapped(colorBtnR.ColorBtnRPropsValFastEq) ),

      )
    }
    .renderBackend[Backend]
    .build

  def _apply(propsValOptProxy: Props) = component( propsValOptProxy )
  val apply: ReactConnectProps[Props_t] = _apply

}
