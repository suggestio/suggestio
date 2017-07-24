package io.suggest.sc.inx

import com.softwaremill.macwire._
import io.suggest.sc.hdr.HeaderModule
import io.suggest.sc.inx.v.IndexR
import io.suggest.sc.inx.v.wc.WelcomeR
import io.suggest.sc.search.SearchModule
import io.suggest.sc.styl.ScCssModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.07.17 9:53
  * Description: DI-модуль пакета индекса выдачи.
  */
class IndexModule(
                   headerModule   : HeaderModule,
                   searchModule   : SearchModule,
                   scCssModule    : ScCssModule
                 ) {

  import headerModule._
  import searchModule._
  import scCssModule._

  lazy val welcomeR = wire[WelcomeR]

  lazy val indexR = wire[IndexR]

}
