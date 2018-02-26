package io.suggest.pwa

import io.suggest.sjs.common.vm.wnd.WindowVm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 11:50
  * Description: Утиль для web-приложений.
  */
object WebAppUtil {

  /** Определить, исполняется ли текущий в контексте вёб-приложения?
    *
    * @see [[https://stackoverflow.com/questions/21125337/how-to-detect-if-web-app-running-standalone-on-chrome-mobile]]
    * @return true - страница (выдача) установлена на home screen.
    *         false - страница исполняется в браузере.
    */
  def isStandalone(): Boolean = {
    val wnd = WindowVm()

    // Для андройда и вообще по дефолту: window.matchMedia('(display-mode: standalone)').matches
    wnd.matchMedia("(display-mode: standalone)")
      .map(_.matches)
      .orElse {
        // Для iOS: window.navigator.standalone
        wnd.navigator
          .flatMap(_.standalone)
      }
      .contains(true)
  }

}
