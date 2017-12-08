package io.suggest.sc

import com.softwaremill.macwire._
import io.suggest.jd.render.JdRenderModule
import io.suggest.sc.styl.{GetScCssF, ScCssFactory}
import io.suggest.sc.v._
import io.suggest.sc.v.grid.{GridCoreR, GridLoaderR, GridR}
import io.suggest.sc.v.hdr._
import io.suggest.sc.v.inx.WelcomeR
import io.suggest.sc.v.search._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.07.17 23:03
  * Description: DI-модуль для compile-time связывания разных классов в граф.
  *
  * Используется macwire, т.к. прост и лёгок: никаких runtime-зависимостей,
  * весь банальнейший код генерится во время компиляции.
  */

/** DI-модуль линковки самого верхнего уровня sc3.
  * Конструктор для линковки тут не используется, чтобы можно было быстро дёрнуть инстанс.
  * Все аргументы-зависимости объявлены и линкуются внутри тела модуля.
  */
class Sc3Module {

  lazy val jdRenderModule = wire[JdRenderModule]

  import jdRenderModule._

  // sc css
  /** Функция-геттер для получения текущего инстанса ScCss. */
  val getScCssF: GetScCssF = { () =>
    sc3Circuit.scCss()
  }
  lazy val scCssFactoryModule = wire[ScCssFactory]
  lazy val scCssR  = wire[ScCssR]


  // header
  lazy val headerR = wire[HeaderR]
  lazy val logoR = wire[LogoR]
  lazy val menuBtnR = wire[MenuBtnR]
  lazy val nodeNameR = wire[NodeNameR]
  lazy val leftR = wire[LeftR]
  lazy val rightR = wire[RightR]
  lazy val searchBtnR = wire[SearchBtnR]


  // index
  lazy val welcomeR = wire[WelcomeR]


  // grid
  lazy val gridLoaderR = wire[GridLoaderR]
  lazy val gridCoreR = wire[GridCoreR]
  lazy val gridR   = wire[GridR]


  // search
  lazy val sTextR = wire[STextR]
  lazy val tabsR = wire[TabsR]
  lazy val searchMapR = wire[SearchMapR]
  lazy val tagsSearchR = wire[TagsSearchR]
  lazy val searchR = wire[SearchR]


  // sc3 top level
  lazy val scRootR = wire[ScRootR]

  lazy val sc3Router = wire[Sc3Router]

  /** Для удобного доступа к контроллеру роутера из view'ов (НЕ через props),
    * нам нужна защита от циклических зависимостей.
    * Эта функция решает все проблемы с циклической зависимостью во время DI-линковки.
    */
  lazy val getRouterCtlF: GetRouterCtlF = { () =>
    sc3Router.routerCtl
  }


  lazy val sc3Api = wire[Sc3ApiXhrImpl]
  lazy val sc3Circuit = wire[Sc3Circuit]

}
