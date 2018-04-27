package io.suggest.adn.edit

import com.softwaremill.macwire._
import io.suggest.adn.edit.v._
import io.suggest.lk.LkCommonModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 17:35
  * Description: macwire compile-time DI.
  */
class LkAdnEditModule {

  val lkCommonModule = wire[LkCommonModule]
  import lkCommonModule._

  lazy val lkAdnEditCircuit = wire[LkAdnEditCircuit]


  // views

  lazy val lkAdEditCss = wire[LkAdnEditCss]

  lazy val lkAdnEditFormR = wire[LkAdnEditFormR]

  lazy val oneRowR = wire[OneRowR]

  lazy val wcFgContR = wire[WcFgContR]

  lazy val lkAdnEditPopupsR = wire[LkAdnEditPopupsR]

  lazy val nodeGalleryR = wire[NodeGalleryR]

  lazy val rightBarR = wire[RightBarR]

}
