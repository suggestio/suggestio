package io.suggest.lk.nodes.form.r.pop

import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.i18n.MsgCodes
import io.suggest.lk.nodes.form.m.{MEditTfDailyS, TfDailyCancelClick, TfDailySaveClick}
import io.suggest.lk.pop.PopupR
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.lk.r.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.i18n.Messages

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 21:57
  * Description: Компонент попапа редактирования тарифа.
  */
object EditTfDailyR {

  type Props = ModelProxy[Option[MEditTfDailyS]]


  class Backend($: BackendScope[Props, Unit]) {

    private def onSaveClick: Callback = {
      dispatchOnProxyScopeCB( $, TfDailySaveClick )
    }

    private def onCancelClick: Callback = {
      dispatchOnProxyScopeCB( $, TfDailyCancelClick )
    }

    def render(propsProxy: Props): ReactElement = {
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

          // TODO Форма с radio-кнопками и полем ручного ценника.

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


  val component = ReactComponentB[Props]("EditTfDaily")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(mEditTfDailyOptProxy: Props) = component(mEditTfDailyOptProxy)

}
