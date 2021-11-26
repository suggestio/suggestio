package io.suggest

import japgolly.scalajs.react.vdom.VdomElement

package object react {

  /** Type alias for component.apply type (no children). */
  type ComponentFunctionR[P] = P => VdomElement

}
