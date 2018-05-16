package com.github.fisshy.react.scroll

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.18 18:30
  * Description:
  */

@JSImport(REACT_SCROLL, "animateScroll")
@js.native
object AnimateScroll extends js.Object {

  def scrollToTop(options: LinkProps = js.native): Unit = js.native

  def scrollToBottom(options: LinkProps = js.native): Unit = js.native


  /** Do scroll to specified position or element.
    *
    * @param to Int - position
    *           String - dom id
    *           Element - html element.
    */
  def scrollTo(to: Int | String | dom.html.Element,
               options: LinkProps = js.native): Unit = js.native

  def scrollMore(pos: Int, options: LinkProps = js.native): Unit = js.native

}

