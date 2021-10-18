package ru.yookassa.checkout.widget

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/** checkout-widget.js API for main class
  * @see [[https://yookassa.ru/developers/payment-forms/widget/quick-start]]
  */
@js.native
@JSGlobal("YooMoneyCheckoutWidget")
class YkCheckoutWidget(props: YkCheckoutWidget.Props) extends js.Object {

  def render(containerId: String): Unit = js.native

  def destroy(): Unit = js.native

}


object YkCheckoutWidget {

  /** Initialization properties for checkout widget.
    * @see [[https://yookassa.ru/developers/payment-forms/widget/reference]] */
  trait Props extends js.Object {
    val confirmation_token: String
    val return_url: js.UndefOr[String]
    val error_callback: js.UndefOr[js.Function1[Error_t, _]] = js.undefined
    val customization: js.UndefOr[Customization] = js.undefined
  }

  /** @see [[https://yookassa.ru/developers/payment-forms/widget/reference#errors]] */
  type Error_t <: String
  object Errors {
    final def INTERNAL_SERVICE_ERROR = "internal_service_error".asInstanceOf[Error_t]
    final def INVALID_COMBINATION_OF_PAYMENT_METHODS = "invalid_combination_of_payment_methods".asInstanceOf[Error_t]
    final def INVALID_PAYMENT_METHODS = "invalid_payment_methods".asInstanceOf[Error_t]
    final def INVALID_RETURN_URL = "invalid_payment_methods".asInstanceOf[Error_t]
    final def INVALID_TOKEN = "invalid_token".asInstanceOf[Error_t]
    final def NO_PAYMENT_METHODS_TO_DISPLAY = "no_payment_methods_to_display".asInstanceOf[Error_t]
    final def RETURN_URL_REQUIRED = "return_url_required".asInstanceOf[Error_t]
    final def TOKEN_EXPIRED = "token_expired".asInstanceOf[Error_t]
    final def TOKEN_REQUIRED = "token_required".asInstanceOf[Error_t]
  }

  /** @see [[https://yookassa.ru/developers/payment-forms/widget/additional-settings/design]] */
  trait Customization extends js.Object {
  }

}

