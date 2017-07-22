package io.suggest.sc

import com.softwaremill.macwire._
import io.suggest.sc.hdr.v._
import io.suggest.sc.inx.v.IndexR
import io.suggest.sc.inx.v.wc.WelcomeR
import io.suggest.sc.root.v.{ScCssR, ScRootR}
import io.suggest.sc.search.v.{STextR, SearchR, TabsR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.07.17 23:03
  * Description: DI-модуль для compile-time связывания разных классов в граф.
  *
  * Используется macwire, т.к. прост и лёгок: никаких runtime-зависимостей,
  * весь банальнейший код генерится во время компиляции.
  */
trait Sc3Module {

  lazy val sc3Api = wire[Sc3ApiXhrImpl]

  lazy val sc3Circuit = wire[Sc3Circuit]


  lazy val scRootR = wire[ScRootR]

  lazy val indexR  = wire[IndexR]
  lazy val indexWelcomeR = wire[WelcomeR]

  lazy val headerR = wire[HeaderR]
  lazy val headerLogoR = wire[LogoR]
  lazy val headerMenuBtnR = wire[MenuBtnR]
  lazy val nodeNameR = wire[NodeNameR]
  lazy val headerLeftR = wire[LeftR]
  lazy val headerRightR = wire[RightR]
  lazy val headerSearchBtnR = wire[SearchBtnR]

  lazy val scCssR  = wire[ScCssR]

  lazy val searchR = wire[SearchR]
  lazy val searchTextR = wire[STextR]
  lazy val searchTabsR = wire[TabsR]

}
