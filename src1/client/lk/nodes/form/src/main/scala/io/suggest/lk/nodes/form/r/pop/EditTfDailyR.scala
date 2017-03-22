package io.suggest.lk.nodes.form.r.pop

import diode.react.ModelProxy
import io.suggest.bill.{MCurrencies, MPrice}
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.pop.PopupR
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.i18n.Messages
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl

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

          PopupR( popupPropsProxy )(
            <.h2(
              ^.`class` := Css.Lk.MINOR_TITLE,
              Messages( MsgCodes.`Adv.tariff` )
            ),

            // Форма с radio-кнопками и полем ввода ручного ценника.
            <.div(

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

                for (manMode <- editS.mode.manualOpt) yield {
                  val mcurrency = editS.nodeTfOpt
                    .fold(MCurrencies.default)( _.currency )
                  val mprice = MPrice( manMode.amount, mcurrency )
                  <.div(
                    <.label(
                      ^.`class` := Css.flat( Css.Lk.Nodes.Inputs.INPUT70, Css.Input.INPUT ),
                      Messages( MsgCodes.`Cost` ),
                      <.input(
                        ^.`type` := "text",
                        ^.value  := MPrice.amountStr( mprice ),
                        ^.onChange ==> onManualAmountChange
                      ),
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
                  Css.Buttons.MAJOR -> editSValid,
                  Css.Buttons.MINOR -> !editSValid
                ),
                editSValid ?= {
                  ^.onClick --> onSaveClick
                },
                Messages( MsgCodes.`Save` )
              ),

              <.a(
                ^.`class` := Css.flat( Css.Buttons.BTN, Css.Buttons.NEGATIVE, Css.Size.M, Css.Buttons.LIST ),
                ^.onClick --> onCancelClick,
                Messages( MsgCodes.`Cancel` )
              )
            )
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
