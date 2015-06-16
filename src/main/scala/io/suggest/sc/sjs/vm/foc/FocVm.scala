package io.suggest.sc.sjs.vm.foc

import io.suggest.sc.sjs.m.msrv.foc.find.IMFocAds

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 12:07
 * Description: ViewModel для focused-выдачи. Модель проецирует свои вызовы на состояние DOM,
 * а так же является конечным автоматом со своим внутренним состоянием.
 */
object FocVm {

  /** Контейнер с данными внутреннего FSM-состояния focused-выдачи. */
  private var _state: Option[FocVmState] = None

  /** Очистка состояния FSM. */
  def clear(): Unit = {
    for (state0 <- _state) {
      state0.rootCont.clear()
      _state = None
    }
  }

  /** Убедиться, что состояние инициализировано. */
  def ensureStateReady(): Unit = {
    if (_state.isEmpty) {
      _state = Some(FocVmState())
    }
  }

  /** Доступны карточки для focused-выдачи. */
  def insertNewFads(offset: Int, fads: IMFocAds): Unit = {
    val state0 = _state.get
    val fadsIter2 = fads.focusedAdsIter.map { fad =>
      fad.index -> FocAdVm(fad)
    }
    val state1 = state0.copy(
      ads           = state0.ads ++ fadsIter2,
      loadedCount   = state0.loadedCount + fads.fadsCount,
      totalCount    = fads.totalCount
    )
    _state = Some(state1)
    // TODO Если уже требуется рендер полученных карточек, то сделать это надо бы.
  }

  /**
   * Перемотка focused-выдачи на указанный index.
   * Если карточка не загружена, то запустить запрос.
   * Запустить горизонтальную анимацию пролистывания и сделать всё прочее.
   * @param index2 На карточку с каким индексом перемотка?
   */
  def goToFadWithIndex(index2: Int): Unit = {
    ???
  }

}


/**
 * Экземпляр контейнера данных состояния FSM Focused ViewModel.
 * @param rootCont Корневой контейнер focused-выдачи.
 * @param ads Аккамулятор уже загруженных с сервера focused-карточек.
 * @param totalCount Общее кол-во карточек со всех возможных выборов в рамках задачи.
 */
case class FocVmState(
  rootCont    : FocRootContainerVm = FocRootContainerVm(),
  ads         : Map[Int, FocAdVm] = Map.empty,
  currIndex   : Option[Int] = None,
  loadedCount : Int = 0,
  totalCount  : Int = 0
)
