package io.suggest.bill.cart.v.pay.systems

import diode.react.ModelProxy
import io.suggest.bill.cart.m.{MCartRootS, PaySystemJsInit}
import io.suggest.i18n.MMessage
import io.suggest.pay.yookassa.YooKassaConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.react.ReactDiodeUtil
import io.suggest.routes.routes
import io.suggest.text.StringUtil
import io.suggest.xplay.json.PlayJsonSjsUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import play.api.libs.json.JsObject
import ru.yookassa.checkout.widget.YkCheckoutWidget
import scalajs.js.JSConverters._

import scala.scalajs.js

/** YooKassa widget wrapper. */
class YooKassaCartR {

  type Props_t = MCartRootS
  type Props = ModelProxy[Props_t]


  class Backend($: BackendScope[Props, _]) {

    private val _idKey = StringUtil.randomId()
    private val _widgetRef = Ref[YkCheckoutWidget]

    private def _onWidgetError(error: YkCheckoutWidget.Error_t): Unit = {
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, PaySystemJsInit( Left(MMessage(error)) ) )
        .runNow()
    }

    /** After mount, initialize widget. */
    def componentDidMount: Callback = {
      $.props >>= { propsProxy: Props =>
        val mroot = propsProxy.value
        val cartSubmitResult = mroot.pay.cartSubmit.get

        val metaObj = cartSubmitResult.pay
          .get.metadata
          .get.value( YooKassaConst.Metadata.WEB_WIDGET_ARGS )
          .as[JsObject]

        val metaProps = PlayJsonSjsUtil
          .toNativeJsonObj( metaObj )
          .asInstanceOf[YkCheckoutWidget.Props]

        val returnRoute = mroot.conf.onNodeId.fold {
          // TODO Return URL inside showcase? Make an URL
          routes.controllers.sc.ScSite.geoSite()

        } { onNodeId =>
          // Return URL inside personal cabinet.
          routes.controllers.LkBill2.orderPage(
            nodeId = onNodeId,
            orderId = mroot.conf.orderId
              .map(_.toDouble)
              .orUndefined,
          )
        }

        val args = new YkCheckoutWidget.Props {
          override val confirmation_token = metaProps.confirmation_token
          override val return_url = HttpClient.mkAbsUrl( returnRoute.absoluteURL() )
          override val error_callback = js.defined( _onWidgetError )
        }
        val widget = new YkCheckoutWidget( args )

        widget.render( _idKey )
        _widgetRef.set( Some(widget) )
      }
    }

    /** Before unmount, deinitialize widget. */
    def componentWillUnmount: Callback = {
      _widgetRef.get >>= { widget =>
        widget.destroy()
        _widgetRef.set( None )
      }
    }

    def render: VdomElement = {
      <.div(
        ^.key := _idKey, // This is needed here?
        ^.id := _idKey,
      )
    }

  }


  val component = ScalaComponent
    .builder[Props]
    .stateless
    .renderBackend[Backend]
    .shouldComponentUpdateConst( false )
    .componentDidMount( _.backend.componentDidMount )
    .componentWillUnmount( _.backend.componentWillUnmount )
    .build

}
