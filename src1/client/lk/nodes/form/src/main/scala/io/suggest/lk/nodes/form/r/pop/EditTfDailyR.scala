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
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.i18n.Messages
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import PopupR.PopupPropsValFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 21:57
  * Description: Компонент попапа редактирования тарифа.
  */
object EditTfDailyR {

  type Props = ModelProxy[Option[MEditTfDailyS]]



  class Backend($: BackendScope[Props, Unit]) {

    private def onInheritedModeClick: Callback = {
      dispatchOnProxyScopeCB( $, TfDailyInheritedMode )
    }

    private def onManualModeClick: Callback = {
      dispatchOnProxyScopeCB( $, TfDailyManualMode )
    }

    private def onManualAmountChange(e: ReactEventI): Callback = {
      val v = e.target.value
      dispatchOnProxyScopeCB( $, TfDailyManualAmountChanged(v) )
    }

    private def onSaveClick: Callback = {
      dispatchOnProxyScopeCB( $, TfDailySaveClick )
    }

    private def onCancelClick: Callback = {
      dispatchOnProxyScopeCB( $, TfDailyCancelClick )
    }


    def render(propsProxy: Props): ReactElement = {
      for {
        editS  <- propsProxy()
      } yield {

        propsProxy.wrap { _ =>
          PopupR.PropsVal(
            closeable = Some(onCancelClick)
          )
        } { popupPropsProxy =>

          val modeInputName = "mode"
          val radio = "radio"
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

                for (_ <- editS.mode.manualOpt) yield {
                  val mcurrency = editS.nodeTfOpt
                    .fold(MCurrencies.default)( _.currency )
                  <.div(
                    <.label(
                      ^.`class` := Css.Input.INPUT,
                      Messages( MsgCodes.`Cost` ),
                      HtmlConstants.SPACE,

                      for (mia <- editS.inputAmount) yield {
                        <.input(
                          ^.`type`    := "text",
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
                saveBtnActive ?= {
                  ^.onClick --> onSaveClick
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


  val component = ReactComponentB[Props]("EditTfDaily")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(mEditTfDailyOptProxy: Props) = component(mEditTfDailyOptProxy)

}
