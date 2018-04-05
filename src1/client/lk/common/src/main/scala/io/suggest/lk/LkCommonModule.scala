package io.suggest.lk

import com.softwaremill.macwire._
import io.suggest.lk.r.color.{ColorBtnR, ColorPickerR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.04.18 21:47
  */
class LkCommonModule {

  lazy val colorPickerR = wire[ColorPickerR]

  lazy val colorBtnR = wire[ColorBtnR]

}
