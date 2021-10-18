package io.suggest.lk.r

import io.suggest.pick.MimeConst
import io.suggest.react.ReactCommonUtil
import japgolly.scalajs.react.{Callback, CallbackOption, Ref, ScalaComponent}
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^.<
import io.suggest.sjs.dom2._
import org.scalajs.dom

/** For injection of <script> tags, this component may be used.
  */
final class ScriptTagR {

  case class Props(
                    src           : String,
                    onLoad        : Option[dom.Event => Unit]       = None,
                    onError       : Option[dom.ErrorEvent => Unit]  = None,
                    async         : Boolean                         = false,
                    defer         : Boolean                         = false,
                    shouldUpdate  : Boolean                         = true,
                  )


  class Backend($: BackendScope[Props, _]) {

    /** Script tag reference, if any. */
    private val _scriptTagRef = Ref[dom.html.Script]

    /** Append script tags to document body. */
    def _appendScriptTags: Callback = {
      for {
        scriptTagOpt <- CallbackOption.liftCallback {
          _scriptTagRef.get.asCallback
        }
        // if script tags NOT yet installed...
        if scriptTagOpt.isEmpty
        props <- CallbackOption.liftCallback( $.props )
        scriptTagAdded = {
          // Adding script tags to document.body
          val body = dom.document.body

          val scriptTag = dom.document
            .createElement( <.script.name )
            .asInstanceOf[dom.html.Script]

          scriptTag.`type` = MimeConst.APPLICATION_JAVASCRIPT
          scriptTag.async = true
          scriptTag.defer = true

          // TODO scriptTag.onload: if many scripts (>1) - many onload with different function-bodies?
          for (f <- props.onLoad)
            scriptTag.onload = f

          for (f <- props.onError)
            scriptTag.onerror = f

          scriptTag.src = props.src
          body.appendChild( scriptTag )

          scriptTag
        }
        r <- _scriptTagRef.set( Some(scriptTagAdded) )
      } yield {
        r
      }
    }

    /** Remove tag from tree and from state. */
    def _remoteScriptTags: Callback = {
      for {
        scriptTag <- _scriptTagRef.get
        _ = {
          dom.document.body.removeChild( scriptTag )
        }
        r <- _scriptTagRef.set( None )
      } yield {
        r
      }
    }

    def render(): VdomElement = {
      (_remoteScriptTags >> _appendScriptTags).runNow()

      ReactCommonUtil.VdomNullElement
    }

  }


  val component = ScalaComponent
    .builder[Props]
    .stateless
    .renderBackend[Backend]
    .componentWillUnmount( _.backend._remoteScriptTags )
    .shouldComponentUpdatePure( _.nextProps.shouldUpdate )
    .build

}
