package io.suggest.grid

import com.github.fisshy.react.scroll.LinkProps
import io.suggest.text.StringUtil

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.05.18 11:20
  * Description: Утиль для организации скроллинга в плитке.
  */
object GridScrollUtil {

  /** Рандомный id для grid-wrapper.  */
  // TODO Если используем рандомные id, то надо избежать комбинаций букв, приводящих к срабатываниям блокировщиков рекламы.
  lazy val SCROLL_CONTAINER_ID = StringUtil.randomIdLatLc()


  def scrollOptions(isSmooth: Boolean = true): LinkProps = {
    new LinkProps {
      override val smooth = isSmooth
      override val duration = if (isSmooth) js.undefined else js.defined(0)
      // Надо скроллить grid wrapper, а не document:
      override val containerId = SCROLL_CONTAINER_ID
    }
  }

}
