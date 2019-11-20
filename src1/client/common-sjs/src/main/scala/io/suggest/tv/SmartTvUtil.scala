package io.suggest.tv

import io.suggest.sjs.common.vm.wnd.WindowVm
import io.suggest.common.empty.OptionUtil.BoolOptOps

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.2019 15:29
  * Description: Утиль для взаимодействия со SmartTV-устройствами.
  */
object SmartTvUtil {

  def isSmartTvUserAgent(userAgent: String): Boolean = {
    "(?i)[()/;\\s](smart|google|web|inet)-?tv(browser)?[()/;\\s]".r
      .pattern
      .matcher(userAgent)
      .find()
  }
  def isSmartTvUserAgent(): Boolean = {
    Try {
      for {
        nav <- WindowVm().navigator
        ua  <- nav.userAgent
        if ua.nonEmpty
      } yield {
        isSmartTvUserAgent( ua )
      }
    }
      .toOption
      .flatten
      .getOrElseFalse
  }

  /** Относится ли текущий User-Agent к Smart-TV-девайсам? */
  lazy val isSmartTvUserAgentCached: Boolean = {
    Try {
      for {
        nav <- WindowVm().navigator
        ua  <- nav.userAgent
        if ua.nonEmpty
      } yield {
        ua matches "(?i)[;(\\s/$](smart|google|web)-?tv[;)\\s/$]"
      }
    }
      .toOption
      .flatten
      .getOrElseFalse
  }

}
