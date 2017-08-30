package com.github.zenoamaro.react

import com.quilljs.delta.Delta
import org.scalajs.dom.Element

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.08.17 15:53
  */
package object quill {

  type ContentValue_t = String | Delta

  type Source_t = js.Object

  type Range_t = js.Object

  type Bounds_t = String | Element

}
