package cordova.plugins.deeplinks

import io.suggest.sjs.JsApiUtil

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/** Cordova deeplinks / universal-links Scala.js API.
  * @see [[https://github.com/vnc-biz/cordova-plugin-deeplinks#application-launch-handling]]
  */
@js.native
@JSGlobal("universalLinks")
object UniversalLinks extends IUniversalLinks


@js.native
trait IUniversalLinks extends js.Object {

  def subscribe(eventName: String, listener: js.Function1[UlEventData, _]): Unit = js.native

  def unsubscribe(eventName: String): Unit = js.native

}
object IUniversalLinks {

  def isAvailable(): Boolean =
    JsApiUtil.isDefinedSafe( UniversalLinks )

  implicit final class UlScalaExt( private val ul: IUniversalLinks ) extends AnyVal {

    def subscribeF(eventName: String = null)(listener: UlEventData => _): Unit =
      ul.subscribe( eventName, listener )

    def unsubscribeF(eventName: String = null): Unit =
      ul.unsubscribe( eventName )

  }

}


@js.native
trait UlEventData extends js.Object {

  val url,
      scheme,
      host,
      path,
      hash: String

  /** Query string parsed. */
  val params: js.Dictionary[String]

}
