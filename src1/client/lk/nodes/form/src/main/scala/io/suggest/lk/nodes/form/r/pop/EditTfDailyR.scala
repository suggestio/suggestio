package io.suggest.lk.nodes.form.r.pop

import diode.react.ModelProxy
import diode.react.ReactPot.potWithReact
import io.suggest.bill.MCurrencies
import io.suggest.common.html.HtmlConstants
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.pop.PopupR
import io.suggest.lk.r.LkPreLoaderR
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.react.ReactCommonUtil.Implicits._
import PopupR.PopupPropsValFastEq
import io.suggest.msg.Messages

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 21:57
  * Description: Компонент попапа редактирования тарифа.
  */
class EditTfDailyR {

  type Props = ModelProxy[Option[MEditTfDailyS]]



  class Backend($: BackendScope[Props, Unit]) {

    private def onInheritedModeClick: Callback = {
      dispatchOnProxyScopeCB( $, TfDailyInheritedMode )
    }

    private def onManualModeClick: Callback = {
      dispatchOnProxyScopeCB( $, TfDailyManualMode )
    }

    private def onManualAmountChange(e: ReactEventFromInput): Callback = {
      val v = e.target.value
      dispatchOnProxyScopeCB( $, TfDailyManualAmountChanged(v) )
    }

    private def onSaveClick: Callback = {
      dispatchOnProxyScopeCB( $, TfDailySaveClick )
    }

    private def onCancelClick: Callback = {
      dispatchOnProxyScopeCB( $, TfDailyCancelClick )
    }


    def render(propsProxy: Props): VdomElement = {
      propsProxy().whenDefinedEl { editS =>

        propsProxy.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some(onCancelClick)
          )
        } { popupPropsProxy =>

          val modeInputName = "mode"
          val radio = HtmlConstants.Input.radio
          val editSValid = editS.isValid
          val isPending = editS.request.isPending
          val saveBtnActive = editSValid && !isPending

          PopupR( popupPropsProxy )(
            <.h2(
              ^.`class` := Css.Lk.MINOR_TITLE,
              Messages( MsgCodes.`Adv.tariff` )
            ),

            // Форма с radio-кнопками и полем ввода ручного ценника.
            <.div(
              ^.`class` := Css.Lk.Popup.ALEFT,

              <.div(
                <.label(
                  ^.`class` := Css.Input.INPUT,
                  <.input(
                    ^.`type`    := radio,
                    ^.name      := modeInputName,
                    ^.value     := "inh",
                    ^.checked   := editS.mode.isInherited,
                    ^.onChange --> onInheritedModeClick
                  ),
                  <.span,
                  Messages( MsgCodes.`Inherited` )
                )
              ),
              <.br,

              <.div(
                <.label(
                  ^.`class` := Css.Input.INPUT,
                  <.input(
                    ^.`type`    := radio,
                    ^.name      := modeInputName,
                    ^.value     := "man",
                    ^.checked   := editS.mode.isManual,
                    ^.onChange --> onManualModeClick
                  ),
                  <.span,
                  Messages( MsgCodes.`Set.manually` )
                ),

                editS.mode.manualOpt.whenDefinedEl { _ =>
                  val mcurrency = editS.nodeTfOpt
                    .fold(MCurrencies.default)( _.currency )
                  <.div(
                    <.label(
                      ^.`class` := Css.Input.INPUT,
                      Messages( MsgCodes.`Cost` ),
                      HtmlConstants.SPACE,

                      editS.inputAmount.whenDefinedEl { mia =>
                        <.input(
                          ^.`type`    := HtmlConstants.Input.text,
                          ^.`class`   := Css.Lk.Nodes.Inputs.INPUT70,
                          ^.value     := mia.value,
                          ^.onChange ==> onManualAmountChange
                        )
                      },

                      mcurrency.symbol,
                      Messages( MsgCodes.`_per_.day` )
                    )
                  )
                }

              )
            ),

            // Рендер кнопок сохранения и отмены.
            <.div(
              ^.`class` := Css.flat( Css.Buttons.BTN_W, Css.Size.M ),

              <.a(
                ^.classSet1(
                  Css.flat( Css.Buttons.BTN, Css.Size.M ),
                  Css.Buttons.MAJOR -> saveBtnActive,
                  Css.Buttons.MINOR -> !saveBtnActive
                ),
                if (saveBtnActive) {
                  ^.onClick --> onSaveClick
                } else {
                  EmptyVdom
                },
                Messages( MsgCodes.`Save` )
              ),

              <.a(
                ^.`class` := Css.flat( Css.Buttons.BTN, Css.Buttons.NEGATIVE, Css.Size.M, Css.Buttons.LIST ),
                ^.onClick --> onCancelClick,
                Messages( MsgCodes.`Cancel` )
              ),

              editS.request.renderPending { _ =>
                LkPreLoaderR.AnimMedium
              }
            ),

            editS.request.renderFailed { ex =>
              <.div(
                <.span(
                  ^.`class` := Css.Colors.RED,
                  Messages( MsgCodes.`Error` ),
                  HtmlConstants.SPACE,
                  ex.toString
                ),
                <.br
              )
            }
          )
        }

      }
    }

  }


  val component = ScalaComponent.builder[Props]("EditTfDaily")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(mEditTfDailyOptProxy: Props) = component(mEditTfDailyOptProxy)

}
