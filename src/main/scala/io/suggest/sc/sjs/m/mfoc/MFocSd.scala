package io.suggest.sc.sjs.m.mfoc

import io.suggest.sc.sjs.vm.grid.GBlock

/** Интерфейс контейнера данных по focused-выдаче. */
trait IFocSd {

  /** Данные по текущей позиции в focused-выдаче. */
  def currIndex   : Option[Int]

  /** id первой карточки, используются только при инициализации focused-выдачи. */
  def currAdId   : Option[String]

  /** grid block, относящийся к текущей карточке. */
  def gblock     : Option[GBlock]

  // TODO Добавить сюда аккамуляторы для уже полученных prev и next cells, т.е. уже распарсенных focused-карточек готовых к добавлению.

  /** Кол-во уже загруженных карточек. */
  def loadedCount : Int

  /**
   * Общее кол-во карточек со всех возможных выборов в рамках задачи.
   * Если None, значит точное кол-во пока не известно.
   */
  def totalCount  : Option[Int]

  /** Текущая блоковая длина карусели. */
  def carLen : Int

}


/**
 * Реализация контейнера данных состояния FSM Focused-выдачи.
 * Контейнер собирается ещё до открытия focused-выдачи для передачи начальных данных.
 */
case class MFocSd(
  override val currIndex   : Option[Int]          = None,
  override val currAdId    : Option[String]       = None,
  override val gblock      : Option[GBlock]       = None,
  override val loadedCount : Int                  = 0,
  override val totalCount  : Option[Int]          = None,
  override val carLen      : Int                  = 0
)
  extends IFocSd
