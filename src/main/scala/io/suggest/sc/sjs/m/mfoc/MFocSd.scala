package io.suggest.sc.sjs.m.mfoc

import io.suggest.sc.sjs.vm.grid.GBlock
import io.suggest.sjs.common.model.MHand

import scala.collection.immutable.Queue


/** Интерфейс контейнера данных по focused-выдаче. */
trait IFocSd {

  /** Данные по текущей позиции в focused-выдаче. */
  def currIndex   : Option[Int]

  /** id первой карточки, используются только при инициализации focused-выдачи. */
  def currAdId   : Option[String]

  /** Кол-во уже загруженных карточек. */
  def loadedCount : Int

  /** Общее кол-во карточек со всех возможных выборов в рамках задачи.
    * Если None, значит точное кол-во пока не известно. */
  def totalCount  : Option[Int]

  /** Очередь для последующих карточек в текущей focused-выдачи. */
  def nexts: FAdQueue

  /** Карточки, уже залитые в карусель. */
  def carState: CarState

  /** Очередь для предшествующих карточек в текущей focused-выдаче. */
  def prevs: FAdQueue

  /** Текущее выставленное направление стрелки, которая рядом с курсором мыши бегает по экрану.
    * Название поля -- сокращение от "arrow direction". */
  def arrDir: Option[MHand]

  /** Опциональные данные состояния touch-навигации внутри focused-выдачи. */
  def touch: Option[MFocTouchSd]


  def shownFadWithIndex(index: Int): Option[FAdShown] = {
    carState.find(_.index == index)
  }

  /** Приведение указанного direction к соответствующему аккамулятору. */
  def dir2Fadq(dir: MHand): FAdQueue = {
    if (dir.isLeft)
      prevs
    else
      nexts
  }

  /** Приведение текущего direction к соответствующему аккамулятору. */
  def currDirFadq: Option[FAdQueue] = {
    arrDir.map { dir2Fadq }
  }

}


/**
 * Реализация контейнера данных состояния FSM Focused-выдачи.
 * Контейнер собирается ещё до открытия focused-выдачи для передачи начальных данных.
 */
case class MFocSd(
  override val currIndex    : Option[Int]           = None,
  override val currAdId     : Option[String]        = None,
  override val loadedCount  : Int                   = 0,
  override val totalCount   : Option[Int]           = None,
  override val nexts        : FAdQueue              = Queue.empty,
  override val carState     : CarState              = Nil,
  override val prevs        : FAdQueue              = Queue.empty,
  override val arrDir       : Option[MHand]         = None,
  override val touch        : Option[MFocTouchSd]   = None
)
  extends IFocSd
