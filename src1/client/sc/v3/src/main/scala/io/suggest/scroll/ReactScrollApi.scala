package io.suggest.scroll

import com.github.fisshy.react.scroll.{AnimateScroll, LinkProps}
import io.suggest.sjs.common.empty.JsOptionUtil

/** Implementation for [[IScrollApi]] for `react-scroll`. */
class ReactScrollApi extends IScrollApi {

  def reactScrollOptions(scrollContainerId: String, isSmooth: Boolean = true): LinkProps = {
    new LinkProps {
      override val smooth = isSmooth
      override val duration = JsOptionUtil.maybeDefined( isSmooth )(0)
      override val containerId = scrollContainerId
    }
  }

  override def scrollTo(containerId: String, relative: Boolean, toPx: Int, smooth: Boolean): Unit = {
    val opts = reactScrollOptions( containerId, smooth )
    if (relative) {
      // relative movement for scrolling
      AnimateScroll.scrollMore( toPx, opts )
    } else if (toPx < 0) {
      // absolute scrolling to top
      AnimateScroll.scrollToBottom( opts )
    } else if (toPx < 1) {
      AnimateScroll.scrollToTop( opts )
    } else {
      AnimateScroll.scrollTo( toPx, opts )
    }
  }

}
