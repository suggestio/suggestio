package io.suggest.lk.nodes.form.r.pop

import diode.react.ModelProxy
import io.suggest.bill.{MCurrencies, MPrice}
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.nodes.form.m.{MEditTfDailyS, TfDailyCancelClick, TfDailyManualAmountChanged, TfDailySaveClick}
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

    private val modeInputName = "mode"
    private val modeInheritedValue = "inh"
    private val modeManualValue = "man"
    private val radio = "radio"


    private def onSaveClick: Callback = {
      dispatchOnProxyScopeCB( $, TfDailySaveClick )
    }

    private def onCancelClick: Callback = {
      dispatchOnProxyScopeCB( $, TfDailyCancelClick )
    }

    private def onModeChanged(e: ReactEventI): Callback = {
      e.target.value match {
        case modeInheritedValue =>
        case modeManualValue =>
      }
      ???
    }

    private def onManualAmountChange(e: ReactEventI): Callback = {
      val v = e.target.value
      e.stopPropagationCB >> {
        dispatchOnProxyScopeCB( $, TfDailyManualAmountChanged(v) )
      }
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

          PopupR( popupPropsProxy )(
            <.h2(
              ^.`class` := Css.Lk.MINOR_TITLE,
              Messages( MsgCodes.`Adv.tariff` )
            ),

            // TODO Форма с radio-кнопками и полем ввода ручного ценника.
            <.div(
              ^.onChange ==> onModeChanged,

              <.div(
                <.label(
                  ^.`class` := Css.Input.INPUT,
                  <.input(
                    ^.`type`  := radio,
                    ^.name    := modeInputName,
                    ^.value   := modeInheritedValue,
                    ^.checked := editS.mode.isInherited
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
                    ^.`type`  := radio,
                    ^.name    := modeInputName,
                    ^.value   := modeManualValue,
                    ^.checked := editS.mode.isManual
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
                      mcurrency.symbol
                    )
                  )
                }

              )
            ),

            // Рендер кнопок сохранения и отмены.
            <.div(
              ^.`class` := Css.flat( Css.Buttons.BTN_W, Css.Size.M ),

              <.a(
                ^.`class` := Css.flat( Css.Buttons.BTN, Css.Buttons.MAJOR, Css.Size.M ),
                ^.onClick --> onSaveClick,
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
