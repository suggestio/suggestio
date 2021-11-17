package io.suggest.sjs

import org.scalajs.dom
import org.scalajs.dom.experimental.Headers

import scala.scalajs.js
import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.12.18 13:00
  * Description: Единый импорт для поддержки улучшенных dom-интерфейсов.
  */
package object dom2 {

  @inline implicit def posOptsToRaw(posOpts: PositionOptions): dom.PositionOptions =
    posOpts.asInstanceOf[dom.PositionOptions]

  @inline implicit def geolocationFromRaw(geolocation: dom.Geolocation): Geolocation =
    geolocation.asInstanceOf[Geolocation]

  /** Тип идентификатора watch'ера в Geolocation API. */
  type GeoLocWatchId_t = js.Any

  @inline implicit def domErrorEventExt(domErrorEvent: dom.ErrorEvent): DomErrorEventExt =
    domErrorEvent.asInstanceOf[DomErrorEventExt]

  @inline implicit def jsErrorExt(jsError: js.Error): JsErrorExt =
    jsError.asInstanceOf[JsErrorExt]

  @inline implicit def netInfoNavigatorExt( navigator: dom.Navigator ): Dom2WndNav_Connection =
    navigator.asInstanceOf[Dom2WndNav_Connection]

  @inline implicit def fetchHeadersExt(headers: Headers ): FetchHeaders =
    headers.asInstanceOf[FetchHeaders]

  @inline implicit def coords2domCoords( coords: Coordinates ): dom.Coordinates =
    coords.asInstanceOf[dom.Coordinates]

  @inline implicit def position2domPosition( position: Position ): dom.Position =
    position.asInstanceOf[dom.Position]

  @inline implicit def posError2domPosError( posError: PositionError ): dom.PositionError =
    posError.asInstanceOf[dom.PositionError]

  @inline implicit def xhr2(xhr: dom.XMLHttpRequest): XmlHttpRequest2 =
    xhr.asInstanceOf[XmlHttpRequest2]

  @inline implicit def htmlScriptTagExt( scriptTag: dom.html.Script ): HtmlScriptTagExt =
    scriptTag.asInstanceOf[HtmlScriptTagExt]

}
