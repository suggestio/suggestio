package io.suggest.sc.sjs.m.mfoc

import io.suggest.sc.sjs.m.msrv.foc.find.IFocAd
import io.suggest.sc.sjs.vm.grid.GBlock

import scala.collection.immutable.Queue


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

  /** Общее кол-во карточек со всех возможных выборов в рамках задачи.
    * Если None, значит точное кол-во пока не известно. */
  def totalCount  : Option[Int]

  /** Очередь для последующих карточек в текущей focused-выдачи. */
  def nexts: Queue[IFocAd]

  /** Карточки, уже залитые в карусель. */
  def carState: List[FAdShown]

  /** Очередь для предшествующих карточек в текущей focused-выдаче. */
  def prevs: Queue[IFocAd]

}


/**
 * Реализация контейнера данных состояния FSM Focused-выдачи.
 * Контейнер собирается ещё до открытия focused-выдачи для передачи начальных данных.
 */
case class MFocSd(
  override val currIndex    : Option[Int]           = None,
  override val currAdId     : Option[String]        = None,
  override val gblock       : Option[GBlock]        = None,
  override val loadedCount  : Int                   = 0,
  override val totalCount   : Option[Int]           = None,
  override val nexts        : Queue[IFocAd]         = Queue.empty,
  override val carState     : List[FAdShown]        = Nil,
  override val prevs        : Queue[IFocAd]         = Queue.empty
)
  extends IFocSd
