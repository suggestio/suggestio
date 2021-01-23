package org.js.react.leaflet.core

import io.suggest.sjs.leaflet.map.LMap

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 16:19
  * Description: Интерфейс для разных API функций в react-leaflet/core.
  * @see [[https://react-leaflet.js.org/docs/core-introduction/]]
  */
@js.native
@JSImport(PACKAGE_NAME, "useAttribution")
object useAttribution extends js.Function2[LMap, js.UndefOr[String], Unit] {

  /** Attribution component hook.
    *
    * @param map Leaflet map.
    * @param attribution string | null | undefined,
    */
  override def apply(map: LMap,
                     attribution: js.UndefOr[String] = js.undefined): Unit = js.native

}