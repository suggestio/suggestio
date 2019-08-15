package com.github.souporserious.react

import japgolly.scalajs.react.raw.React.RefHandle
import org.scalajs.dom.html

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.08.2019 19:07
  */
package object measure {

  final val NPM_PACKAGE_NAME = "react-measure"

  /** single point for defining ref-type for onResize(ref). */
  type RefHandle_t = RefHandle[html.Element]

}
