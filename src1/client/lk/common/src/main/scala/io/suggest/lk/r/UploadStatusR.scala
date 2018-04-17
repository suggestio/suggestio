package io.suggest.lk.r

import diode.data.Pot
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import diode.react.{ModelProxy, ReactConnectProps}
import diode.react.ReactPot._
import io.suggest.common.html.HtmlConstants._
import io.suggest.css.Css
import io.suggest.file.up.MFileUploadS
import io.suggest.i18n.MsgCodes
import io.suggest.msg.Messages
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.spa.OptFastEq.Wrapped

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.04.18 10:20
  * Description: react-компонент, отображающий состояние текущей заливки файла на сервер.
  */
class UploadStatusR {

  import MFileUploadS.MFileUploadSFastEq


  type Props_t = Option[MFileUploadS]
  type Props = ModelProxy[Props_t]

  class Backend($: BackendScope[Props, Props_t]) {

    def render(upStateOptProxy: Props): VdomElement = {
      upStateOptProxy.value.whenDefinedEl { upState =>
        <.div(

          ReactCommonUtil.maybeNode( upState.uploadReq.isPending || upState.prepareReq.isPending ) {
            <.span(
              LkPreLoaderR.AnimSmall,

              Messages( MsgCodes.`Uploading.file` ),
              ELLIPSIS
            )
          },

          upState.progress.whenDefined { progress =>
            <.span(
              `(`, progress.pct, `)`
            )
          },

          //upState.prepareReq
          _renderReqErr( upState.prepareReq, MsgCodes.`Preparing` ),
          _renderReqErr( upState.uploadReq, MsgCodes.`Uploading.file` ),

        )
      }
    }

    private def _renderReqErr(reqPot: Pot[_], commentCode: String): TagMod = {
      reqPot.renderFailed { ex =>
        <.span(
          ^.`class` := Css.Colors.RED,
          Messages( MsgCodes.`Error` ), COLON, SPACE,
          `(`, Messages( commentCode ), `)`, SPACE,

          ex.toString,
          COLON, SPACE,
          ex.getMessage
        )
      }
    }

  }


  val component = ScalaComponent.builder[Props]("UploadStatus")
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate )
    .build

  def _apply(upStateOptProxy: Props) = component( upStateOptProxy)
  val apply: ReactConnectProps[Props_t] = _apply

}
