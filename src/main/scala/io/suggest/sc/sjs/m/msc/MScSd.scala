package io.suggest.sc.sjs.m.msc

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.sjs.m.mfoc.MFocSd
import io.suggest.sc.sjs.m.mgeo._
import io.suggest.sc.sjs.m.mgrid.{MGridData, MGridState}
import io.suggest.sc.sjs.m.mnav.MNavState
import io.suggest.sc.sjs.m.msearch.MSearchSd

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.06.15 17:33
 * Description: Модель состояния конечного автомата интерфейса выдачи.
 */

/** Интерфейс контейнера данный полного состояния выдачи. */
trait IScSd {

  /** Контейнер для общих полей resizeOpt, screen, browser и прочих. */
  def common      : MScCommon

  /** Данные по окну, если есть. */
  def screen      : Option[IMScreen]

  /** Контейнер данных состояния плитки карточек. */
  def grid        : MGridData

  /** id текущего узла, если есть. */
  def adnIdOpt    : Option[String]

  /** Контейнер данных состояния поиска и поисковой панели. */
  def search      : MSearchSd

  /** Контейнер данных состояния панели навигации и навигации в целом. */
  def nav         : MNavState

  /** Контейнер для данных focused-выдачи.
    * None значит, что focused-выдача отключена. */
  def focused     : Option[MFocSd]

  /** Контейнер с данными геолокации. Пришел на смену MGeoLocUtil. */
  def geo         : MGeoLocSd

  /** @return true если открыта какая-то боковая панель.
    *         false -- ни одной панели не открыто. */
  def isAnySidePanelOpened: Boolean = {
    nav.panelOpened || search.opened
  }

}


/** Реализация immutable-контейнера для передачи данных Sc FSM между состояниями. */
case class MScSd(
  override val common       : MScCommon             = MScCommon.empty,
  override val screen       : Option[IMScreen]      = None,
  override val grid         : MGridData             = MGridData(),
  override val adnIdOpt     : Option[String]        = None,
  override val search       : MSearchSd             = MSearchSd(),
  override val nav          : MNavState             = MNavState(),
  override val focused      : Option[MFocSd]        = None,
  override val geo          : MGeoLocSd             = MGeoLocSd()
)
  extends IScSd
{

  /**
   * При переключении узла надо резко менять и чистить состояние. Тут логика этого обновления.
   *
   * @param adnIdOpt2 Новый id текущего узла-ресивера.
   * @return Новый экземпляр состояния.
   */
  def withNodeSwitch(adnIdOpt2: Option[String]): MScSd = {
    copy(
      adnIdOpt  = adnIdOpt2,
      nav       = MNavState(),
      search    = MSearchSd(),
      grid      = MGridData(
        state = MGridState(
          adsPerLoad = grid.state.adsPerLoad
        )
      ),
      focused   = None
    )
  }

}
