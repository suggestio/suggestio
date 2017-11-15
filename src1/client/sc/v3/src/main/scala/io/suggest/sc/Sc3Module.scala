package io.suggest.sc

import com.softwaremill.macwire._
import io.suggest.jd.render.JdRenderModule
import io.suggest.sc.grid.GridModule
import io.suggest.sc.hdr.HeaderModule
import io.suggest.sc.inx.IndexModule
import io.suggest.sc.root.v.{ScCssR, ScRootR}
import io.suggest.sc.search.SearchModule
import io.suggest.sc.styl.{ScCssFactoryModule, ScCssModule}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.07.17 23:03
  * Description: DI-модуль для compile-time связывания разных классов в граф.
  *
  * Используется macwire, т.к. прост и лёгок: никаких runtime-зависимостей,
  * весь банальнейший код генерится во время компиляции.
  */
class Sc3CircuitModule {

  lazy val sc3Api = wire[Sc3ApiXhrImpl]

  lazy val scCssFactoryModule = wire[ScCssFactoryModule]

  lazy val sc3Circuit = wire[Sc3Circuit]

}


/** DI-модуль линковки самого верхнего уровня sc3.
  * Конструктор для линковки тут не используется, чтобы можно было быстро дёрнуть инстанс.
  * Все аргументы-зависимости объявлены и линкуются внутри тела модуля.
  */
class Sc3Modules {

  // Ручная линковка модулей, т.к. из-за @Module кажется, что есть проблемы.

  lazy val sc3CircuitModule = wire[Sc3CircuitModule]

  lazy val scCssModule = wire[ScCssModule]

  lazy val jdRenderModule = wire[JdRenderModule]


  // Deps: не используем конструктор класса, чтобы скрыть всё DI позади new/extends.

  lazy val headerModule = wire[HeaderModule]

  lazy val searchModule = wire[SearchModule]

  lazy val indexModule = wire[IndexModule]

  lazy val gridModule = wire[GridModule]

  lazy val sc3Module = wire[Sc3Module]

}


class Sc3Module(
                 scCssModule  : ScCssModule,
                 indexModule  : IndexModule,
                 gridModule   : GridModule
               ) {

  import scCssModule._
  import indexModule._
  import gridModule._

  lazy val scCssR  = wire[ScCssR]

  // sc3 data

  lazy val scRootR = wire[ScRootR]

}
