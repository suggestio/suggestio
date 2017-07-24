package io.suggest.sc.hdr

import com.softwaremill.macwire._
import io.suggest.sc.hdr.v.SearchBtnR
import io.suggest.sc.hdr.v._
import io.suggest.sc.styl.ScCssModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.07.17 9:33
  * Description: DI-модуль для разных элементов заголовка.
  */
class HeaderModule( scCssModule: ScCssModule ) {

  import scCssModule._

  lazy val headerR = wire[HeaderR]
  lazy val logoR = wire[LogoR]
  lazy val menuBtnR = wire[MenuBtnR]
  lazy val nodeNameR = wire[NodeNameR]
  lazy val leftR = wire[LeftR]
  lazy val rightR = wire[RightR]
  lazy val searchBtnR = wire[SearchBtnR]

}
