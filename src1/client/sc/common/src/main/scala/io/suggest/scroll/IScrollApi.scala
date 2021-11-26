package io.suggest.scroll

/** Interface for scrolling API. */
trait IScrollApi {

  /** Scroll container to custom scrolling position.
    *
    * scrollToTop => (relative=false, toPx=0)
    * scrollMore => (relative=true, toPx=50)
    * scrollTo => (relative=false, toPx=50)
    * scrollToBottom => (relative=false, toPx=-1)
    *
    * @param containerId Scrolling container id.
    * @param relative true => scroll more by +toPx.
    *                 false => scroll absolutely to toPx.
    * @param toPx Pixels to scroll.
    * @param smooth Animated scrolling?
    *               false - non-animated scroll to new position.
    *               true - animated by default.
    */
  def scrollTo(containerId: String, relative: Boolean, toPx: Int, smooth: Boolean = true): Unit

}
