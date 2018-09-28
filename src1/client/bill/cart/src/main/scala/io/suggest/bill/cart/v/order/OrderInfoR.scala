package io.suggest.bill.cart.v.order

import chandu0101.scalajs.react.components.materialui.{MuiCard, MuiCardContent, MuiTypoGraphy, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.i18n.MsgCodes
import io.suggest.mbill2.m.order.MOrder
import io.suggest.msg.Messages
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.r.YmdR
import io.suggest.dt.CommonDateTimeUtil.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.09.18 11:02
  * Description: Рендер мета-данных по одному ордеру.
  * Для ордера-корзины это не рендерилось, в основном для архивных и остальных ордеров.
  */
class OrderInfoR {

  type Props_t = Option[MOrder]
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Unit] ) {

    def render(propsOptProxy: Props): VdomElement = {
      propsOptProxy.value.whenDefinedEl { morder =>
        MuiCard()(
          MuiCardContent()(

            // Заголовок - Заказ N3343 | или Корзина | ...
            MuiTypoGraphy(
              new MuiTypoGraphyProps {
                override val variant = MuiTypoGraphyVariants.headline
              }
            )(
              morder.id
                .fold( Messages(MsgCodes.`Cart`) ) { orderId =>
                  Messages(MsgCodes.`Order.N`, orderId)
                }
            ),

            // Статус заказа с датой выставления статуса.
            MuiTypoGraphy(
              new MuiTypoGraphyProps {
                override val variant = MuiTypoGraphyVariants.caption
              }
            )(
              Messages( morder.status.singular ),

              HtmlConstants.SPACE,
              HtmlConstants.PIPE,
              HtmlConstants.SPACE,

              // Дата выставления статуса
              YmdR(
                morder.dateStatus.toLocalDate.toYmd
              )()
            )

          )
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply( propsOptProxy: Props ) = component( propsOptProxy )

}
