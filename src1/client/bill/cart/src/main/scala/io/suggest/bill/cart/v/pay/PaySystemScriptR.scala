package io.suggest.bill.cart.v.pay

import com.materialui.{MuiAlert, MuiAlertTitle}
import diode.data.Pot
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.bill.cart.m.PaySystemJsInit
import io.suggest.i18n.{MCommonReactCtx, MMessage, MMessageException, MsgCodes}
import io.suggest.lk.r.ScriptTagR
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.pay.MPaySystem
import io.suggest.ueq.UnivEqUtil._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.spa.{FastEqUtil, OptFastEq}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom
import play.api.libs.json.{JsArray, JsString, Json}

/** Component for pay-system js script-tag. */
class PaySystemScriptR(
                        scriptTagR                : ScriptTagR,
                        crCtxP                    : React.Context[MCommonReactCtx],
                      )
  extends Log
{

  type Props_t = Pot[MPaySystem]
  type Props = ModelProxy[Props_t]


  case class State(
                    errorOptC                   : ReactConnectProxy[Option[Throwable]],
                    paySystemForScriptPotC      : ReactConnectProxy[Props_t],
                  )

  class Backend($: BackendScope[Props, State]) {

    private val _onScriptLoad = Some {
      ReactCommonUtil.cbFun1ToF { _: dom.Event =>
        ReactDiodeUtil.dispatchOnProxyScopeCB( $, PaySystemJsInit( Right( None ) ) )
      }
    }

    private val _onScriptError = Some {
      ReactCommonUtil.cbFun1ToF { e: dom.ErrorEvent =>
        $.props >>= { props: Props =>
          logger.error( ErrorMsgs.CONNECTION_ERROR, msg = e )
          val args = JsArray(
            (for {
              paySystem <- props.value.toOption
              jsUrl <- paySystem.payWidgetJsScriptUrl
            } yield {
              JsString( jsUrl )
            })
              .toSeq
          )

          ReactDiodeUtil.dispatchOnProxyScopeCB( $, PaySystemJsInit( Left( MMessage(ErrorMsgs.CANNOT_CONNECT_TO_REMOTE_SYSTEM, args) ) ) )
        }
      }
    }


    def render(s: State): VdomElement = {
      React.Fragment(

        // Render exception, if any.
        s.errorOptC { errorOptProxy =>
          errorOptProxy.value.whenDefinedEl { ex =>
            MuiAlert(
              new MuiAlert.Props {
                override val severity = MuiAlert.Severity.ERROR
              }
            )(
              MuiAlertTitle()(
                crCtxP.message( MsgCodes.`Something.gone.wrong` ),
              ),
              ex match {
                case m: MMessageException =>
                  <.span(
                    crCtxP.message( m.mmessage.message ),
                    <.br,
                    Json.stringify( m.mmessage.args ),
                  )
                case ex: Throwable =>
                  ex.getMessage
              },
              <.br,
              crCtxP.message( MsgCodes.`Check.your.internet.connection.and.retry` ),
            )
          }
        },

        // Render script tags into document.body:
        s.paySystemForScriptPotC { paySystemPotProxy =>
          (for {
            paySystem <- paySystemPotProxy
              .value
              .toOption
            scriptUrl <- paySystem.payWidgetJsScriptUrl
          } yield {
            scriptTagR.component(
              scriptTagR.Props(
                src       = scriptUrl,
                onLoad    = _onScriptLoad,
                onError   = _onScriptError,
                defer     = true,
                async     = true,
              )
            )
          })
            .whenDefinedEl
        },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]
    .initialStateFromProps { propsProxy =>
      State(

        errorOptC = propsProxy.connect( _.exceptionOption )( OptFastEq.Plain ),

        paySystemForScriptPotC = propsProxy.connect { pot =>
          if (pot.isFailed)
            Pot.empty
          else
            pot
        }( FastEqUtil[Props_t] { (a, b) =>
          (a getOrElse null) ===* (b getOrElse null)
        }),

      )
    }
    .renderBackend[Backend]
    .build

}
