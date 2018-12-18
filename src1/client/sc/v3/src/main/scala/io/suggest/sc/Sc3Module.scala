package io.suggest.sc

import com.softwaremill.macwire._
import io.suggest.jd.render.JdRenderModule
import io.suggest.sc.c.{IRespActionHandler, IRespHandler, IRespWithActionHandler}
import io.suggest.sc.c.grid.{GridFocusRespHandler, GridRespHandler}
import io.suggest.sc.c.inx.{ConfUpdateRah, IndexRah}
import io.suggest.sc.c.search.NodesSearchRespHandler
import io.suggest.sc.styl.{GetScCssF, ScCssStatic}
import io.suggest.sc.v._
import io.suggest.sc.v.grid._
import io.suggest.sc.v.hdr._
import io.suggest.sc.v.inx._
import io.suggest.sc.v.menu._
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
  val getScCssF: GetScCssF = sc3Circuit.scCssRO.apply

  lazy val scCssFactory = wire[ScCssFactory]


  // header
  lazy val headerR = wire[HeaderR]
  lazy val logoR = wire[LogoR]
  lazy val menuBtnR = wire[MenuBtnR]
  lazy val nodeNameR = wire[NodeNameR]
  lazy val leftR = wire[LeftR]
  lazy val rightR = wire[RightR]
  lazy val searchBtnR = wire[SearchBtnR]
  lazy val hdrProgressR = wire[HdrProgressR]
  lazy val goBackR = wire[GoBackR]


  // index
  lazy val welcomeR = wire[WelcomeR]
  lazy val indexRespHandler = wire[IndexRah]
  lazy val confUpdateRah = wire[ConfUpdateRah]
  lazy val indexSwitchAskR = wire[IndexSwitchAskR]


  // grid
  lazy val gridCoreR = wire[GridCoreR]
  lazy val gridR   = wire[GridR]
  lazy val gridRespHandler = wire[GridRespHandler]
  lazy val gridFocusRespHandler = wire[GridFocusRespHandler]


  // search
  lazy val sTextR = wire[STextR]
  lazy val searchMapR = wire[SearchMapR]
  lazy val searchR = wire[SearchR]
  lazy val geoSearchRespHandler = wire[NodesSearchRespHandler]
  lazy val nodesFoundR = wire[NodesFoundR]
  lazy val nodeFoundR = wire[NodeFoundR]
  lazy val geoMapOuterR = wire[GeoMapOuterR]
  lazy val nodesSearchR = wire[NodesSearchContR]


  // menu
  lazy val menuR = wire[MenuR]
  lazy val enterLkRowR = wire[EnterLkRowR]
  lazy val aboutSioR = wire[AboutSioR]
  lazy val editAdR = wire[EditAdR]
  lazy val blueToothR = wire[BlueToothR]
  lazy val unsafeScreenAreaOffsetR = wire[UnsafeScreenAreaOffsetR]
  lazy val slideMenuItemR = wire[SlideMenuItemR]
  lazy val geoLocR = wire[GeoLocR]


  // sc3 top level
  lazy val scRootR = wire[ScRootR]

  lazy val sc3SpaRouter = wire[Sc3SpaRouter]

  /** Для удобного доступа к контроллеру роутера из view'ов (НЕ через props),
    * нам нужна защита от циклических зависимостей.
    * Эта функция решает все проблемы с циклической зависимостью во время DI-линковки.
    */
  lazy val getRouterCtlF: GetRouterCtlF = { () =>
    sc3SpaRouter.routerCtl
  }


  lazy val sc3Api = wire[Sc3ApiXhrImpl]
  lazy val sc3Circuit = wire[Sc3Circuit]


  /** Списки обработчиков ответов сервера и resp-action в этих ответах. */
  lazy val (respHandlers, respActionHandlers) = {
    // Часть модулей является универсальной, поэтому шарим хвост списка между обоими списками:
    val mixed = List[IRespWithActionHandler](
      gridRespHandler,
      gridFocusRespHandler,
      indexRespHandler,
      geoSearchRespHandler
    )

    val rahs: List[IRespActionHandler] =
      confUpdateRah ::
      mixed

    val rhs: List[IRespHandler] =
      mixed

    (rhs, rahs)
  }

}
