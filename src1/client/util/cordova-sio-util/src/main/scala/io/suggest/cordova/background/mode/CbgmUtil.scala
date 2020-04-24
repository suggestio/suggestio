package io.suggest.cordova.background.mode

import cordova.plugins.background.mode.CbgmDefaults
import io.suggest.daemon.MDaemonNotifyOpts

import scala.scalajs.js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.2020 17:38
  * Description: Утиль для обёртки вокруг плагина cordova-plugin-background-mode.
  */
object CbgmUtil {

  /** Конверсия sio-опций нотификации в CBGM JSON формат для setDefaults()/configure(). */
  def notifyOpts2cbgm(notOpts: Option[MDaemonNotifyOpts]): CbgmDefaults = {
    notOpts.fold[CbgmDefaults] {
      new CbgmDefaults {
        override val silent = true
      }
    } { not =>
      val _colorCode = not.color
        .orUndefined
        .map(_.code)
      new CbgmDefaults {
        override val title = not.title.orUndefined
        override val text = not.text.orUndefined
        override val icon = not.icon.orUndefined
        override val color = _colorCode
        override val resume = not.resumeAppOnClick.orUndefined
        override val hidden = not.lockScreen.orUndefined.map(!_)
        override val bigText = not.bigText.orUndefined

        override val channelName = not.channelTitle.orUndefined
        override val channelDescription = not.channelDescr.orUndefined
      }
    }
  }

}
