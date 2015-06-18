package io.suggest.sc.sjs.m.mfoc

import io.suggest.sc.sjs.vm.foc.{FocAdVm, FocRootContainerVm}

/**
 * Экземпляр контейнера данных состояния FSM Focused ViewModel.
 * @param rootCont Корневой контейнер focused-выдачи.
 * @param ads Аккамулятор уже загруженных с сервера focused-карточек.
 * @param totalCount Общее кол-во карточек со всех возможных выборов в рамках задачи.
 *                   Если None, значит точное кол-во пока не известно.
 */
case class FocVmStateData(
  rootCont    : FocRootContainerVm = FocRootContainerVm(),
  ads         : Map[Int, FocAdVm] = Map.empty,
  currIndex   : Option[Int] = None,
  loadedCount : Int = 0,
  totalCount  : Option[Int] = None
)
