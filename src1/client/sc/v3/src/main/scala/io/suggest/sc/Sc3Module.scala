package io.suggest.sc

import com.softwaremill.macwire._
import io.suggest.jd.render.JdRenderModule
import io.suggest.sc.m.MScReactCtx
import io.suggest.sc.sc3.Sc3Pages
import io.suggest.sc.v._
import io.suggest.sc.v.dia.first.WzFirstR
import io.suggest.sc.v.grid._
import io.suggest.sc.v.hdr._
import io.suggest.sc.v.inx._
import io.suggest.sc.v.menu._
import io.suggest.sc.v.search._
import japgolly.scalajs.react.React
import japgolly.scalajs.react.extra.router.RouterCtl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.07.17 23:03
  * Description: DI-модуль линковки самого верхнего уровня sc3.
  * Все аргументы-зависимости объявлены и линкуются внутри тела модуля.
  */
class Sc3Module {

  lazy val jdRenderModule = wire[JdRenderModule]

  import jdRenderModule._
  import io.suggest.ReactCommonModule._


  // Костыли для js-роутера.
  // Без костылей вероятна проблема курицы и яйца в виде циклической зависимости инстансов:
  // - Шаблонам нужен routerCtl (react-контекст) для рендера ссылок и прочего.
  // - Роутеру (уже во время роутинга) нужны инстансы шаблонов для рендера интерфейса.
  //
  // Однако, цикл неявный: все инстансы нужны НЕодновременно:
  // - инстанс роутера, который дёргает шаблоны только при необходимости.
  // - Аналогично с шаблонами: дёргают роутер только после монтирования в VDOM.
  //
  // Для явной разводки доступа к инстансам,
  // используются 0-arg функции, которые скрывают за собой lazy-инстансы.
  // Костыль для инжекции ленивого доступа к инстансу ScRootR.
  private def _sc3CircuitF(routerCtl: RouterCtl[Sc3Pages]) = wire[Sc3Circuit]

  lazy val sc3SpaRouter: Sc3SpaRouter = {
    new Sc3SpaRouter(
      scReactCtxContF = () => scReactCtx,
      sc3CircuitF     = _sc3CircuitF,
      scRootR         = () => scRootR,
    )
  }

  import sc3SpaRouter.routerCtl

  /** Сборка контейнера контекста, который будет распихан по sc-шаблонам. */
  lazy val scReactCtx: React.Context[MScReactCtx] =
    React.createContext( sc3SpaRouter.mkScReactCtx )


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
  lazy val indexSwitchAskR = wire[IndexSwitchAskR]


  // grid
  lazy val gridCoreR = wire[GridCoreR]
  lazy val gridR   = wire[GridR]


  // search
  lazy val sTextR = wire[STextR]
  lazy val searchMapR = wire[SearchMapR]
  lazy val searchR = wire[SearchR]
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


  // wizard
  lazy val wzFirstR = wire[WzFirstR]


  // sc3
  lazy val scRootR = wire[ScRootR]
  lazy val sc3Api = wire[Sc3ApiXhrImpl]
  def sc3Circuit(routerCtl: RouterCtl[Sc3Pages]) = wire[Sc3Circuit]

}
