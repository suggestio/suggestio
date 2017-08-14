package io.suggest.sc.search

import com.softwaremill.macwire._
import io.suggest.sc.search.v._
import io.suggest.sc.styl.ScCssModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.07.17 9:54
  * Description: DI-модуль для пакета поиска выдачи.
  */

class SearchModule( scCssModule: ScCssModule ) {

  import scCssModule._

  lazy val sTextR = wire[STextR]
  lazy val tabsR = wire[TabsR]

  lazy val searchR = wire[SearchR]

}
