package io.suggest.ad.edit.v.edit

import scalacss.ScalaCssReact._
import com.materialui.{MuiCheckBox, MuiCheckBoxProps, MuiClickAwayListener, MuiColorTypes, MuiFormControlLabel, MuiFormControlLabelProps, MuiSelect, MuiSelectProps, MuiTextField, MuiTypoGraphy}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.ad.edit.m.{OutlineColorModeSet, OutlineColorSet, OutlineOnOff, OutlineShowHide}
import io.suggest.ad.edit.v.LkAdEditCss
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.jd.tags.MJdOutLine
import io.suggest.lk.m.ColorAddToPalette
import io.suggest.lk.r.color.Color2PickerR
import io.suggest.react.ReactCommonUtil.Implicits.VdomElOptionExt
import io.suggest.react.ReactDiodeUtil.Implicits.ModelProxyExt
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.12.2020 18:39
  * Description: wrap-компонент для формы управления обводкой в редакторе карточки.
  */
final class OutLineR(
                      color2PickerR         : Color2PickerR,
                      lkAdEditCss           : LkAdEditCss,
                      crCtxP                : React.Context[MCommonReactCtx],
                    ) {

  type Props = ModelProxy[Option[MJdOutLine]]

  case class State(
                    onOffSomeC            : ReactConnectProxy[Some[Boolean]],
                    colorNonEmptyC        : ReactConnectProxy[Some[Boolean]],
                    colorDefinedOptC      : ReactConnectProxy[Option[MColorData]],
                  )

  class Backend( $: BackendScope[Props, State] ) {

    private val _onOffChangeCb = ReactCommonUtil.cbFun2ToJsCb {
      (_: ReactEventFromInput, isChecked2: Boolean) =>
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, OutlineOnOff(isChecked2) )
    }

    private val _onColorChange = { (mcd: MColorData) =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, OutlineColorSet(mcd) )
    }

    private val _colorModeChange = ReactCommonUtil.cbFun1ToJsCb {
      (e: ReactEventFromInput) =>
        val v = e.target.value.toBoolean
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, OutlineColorModeSet(v) )
    }

    private def _showOutlineCb(isVisible: Boolean): Callback =
      ReactDiodeUtil.dispatchOnProxyScopeCB($, OutlineShowHide(isVisible))

    private val _onClickAway = ReactCommonUtil.cbFun0ToJsCb { () =>
      _showOutlineCb( false )
    }

    private val _onSelectOpen = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      _showOutlineCb( true )
    }

    /** При закрытии color-picker'а следует отправить последний итоговый цвет в палитру цветов. */
    private def _onOpenClose(isOpen: Boolean): Callback = {
      var acc = _showOutlineCb(true)

      if (!isOpen) {
        acc = acc >> $.props >>= { props: Props =>
          props
            .value
            .flatMap(_.color)
            .fold( Callback.empty ) { mcd =>
              ReactDiodeUtil.dispatchOnProxyScopeCB( $, ColorAddToPalette(mcd) )
            }
        }
      }

      acc
    }
    private val _onOpenCloseSome = Some( _onOpenClose _ )

    def render(s: State): VdomElement = {
      // Нужна галочка активации ручной настройки обводки.
      // Нужно управление цветом: прозрачный цвет, ручной цвет.
      val content = <.div(
        lkAdEditCss.Outline.container,

        // Галочка включения/выключения управления обводкой.
        MuiFormControlLabel {
          val _label = MuiTypoGraphy()(
            crCtxP.message( MsgCodes.`Outline` )
          )
          val _onOffCheckBox = s.onOffSomeC { onOffSomeProxy =>
            val onOff = onOffSomeProxy.value.value
            MuiCheckBox(
              new MuiCheckBoxProps {
                override val checked = js.defined( onOff )
                @JSName("onChange")
                override val onChange2 = _onOffChangeCb
                override val color = MuiColorTypes.secondary
              }
            )
          }
          new MuiFormControlLabelProps {
            override val label = _label.rawNode
            override val control = _onOffCheckBox.rawElement
          }
        },
        HtmlConstants.NBSP_STR,

        // Управление цветом в два этапа: есть цвет, прозрачный цвет.
        s.onOffSomeC { onOffSomeProxy =>
          ReactCommonUtil.maybeEl( onOffSomeProxy.value.value ) {
            val _children = crCtxP.consume { crCtx =>
              React.Fragment(
                <.option(
                  ^.value := false.toString,
                  crCtx.messages( MsgCodes.`Transparent` ),
                ),
                <.option(
                  ^.value := true.toString,
                  crCtx.messages( MsgCodes.`Define.manually` ),
                ),
              )
            }

            s.colorNonEmptyC { colorOptProxy =>
              val colorNonEmpty = colorOptProxy.value.value
              MuiSelect(
                new MuiSelectProps {
                  override val value      = js.defined( colorNonEmpty.toString )
                  override val onChange   = _colorModeChange
                  override val onOpen     = _onSelectOpen
                  override val native     = true
                  override val variant    = MuiTextField.Variants.standard
                }
              )( _children )
            }
          }
        },

        // Если режим заданного цвета, то нужен color picker.
        {
          s.colorDefinedOptC { colorOptProxy =>
            colorOptProxy.value.whenDefinedEl { mcd =>
              color2PickerR.component(
                color2PickerR.PropsVal(
                  colorProxy  = colorOptProxy.resetZoom( mcd ),
                  onChange    = _onColorChange,
                  onOpenClose = _onOpenCloseSome,
                )
              )
            }
          }
        },

      )

      MuiClickAwayListener.component(
        new MuiClickAwayListener.Props {
          override val onClickAway = _onClickAway
        }
      )( content )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        onOffSomeC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.nonEmpty )
        },
        colorNonEmptyC = propsProxy.connect { props =>
          OptionUtil.SomeBool( props.flatMap(_.color).nonEmpty )
        },
        colorDefinedOptC = propsProxy.connect { props =>
          props.flatMap(_.color)
        },
      )
    }
    .renderBackend[Backend]
    .build

}
