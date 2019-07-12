package io.suggest.id.login.v.stuff

import com.materialui.{MuiSnackBarContent, MuiSnackBarContentProps}
import japgolly.scalajs.react.{React, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants
import io.suggest.err.MCheckException
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.react.ReactCommonUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.07.19 12:44
  * Description: Уведомление об ошибке.
  */
class ErrorSnackR(
                   commonReactCtxProv    : React.Context[MCommonReactCtx],
                 ) {

  type Props_t = Throwable
  type Props = ModelProxy[Props_t]

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .render_P { throwableOrNullProxy =>
      val throwableOrNull = throwableOrNullProxy.value
      ReactCommonUtil.maybeEl( throwableOrNull != null ) {
        MuiSnackBarContent {
          val msg = <.span(
            commonReactCtxProv.consume { crCtx =>
              crCtx.messages( MsgCodes.`Error` )
            },
            HtmlConstants.COLON, HtmlConstants.SPACE,
            throwableOrNull match {
              case mce: MCheckException =>
                mce
                  .localizedMessage
                  .fold[VdomNode] {
                    commonReactCtxProv.consume { crCtx =>
                      crCtx.messages( mce.getMessage )
                    }
                  } { localized => localized: VdomNode }
              case _ =>
                throwableOrNull.getMessage: VdomNode
            },
          )
          new MuiSnackBarContentProps {
            override val message = msg.rawNode
          }
        }
      }
    }
    .build

}
