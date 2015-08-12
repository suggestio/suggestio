package io.suggest.sc.sjs.m.mfoc

import io.suggest.sc.sjs.vm.foc.FocAd

/** Интерфейс контейнера данных по focused-выдаче. */
trait IFocSd {

  /** Данные по текущей позиции в focused-выдаче. */
  def currIndex   : Int

  /** id первой карточки, используются только при инициализации focused-выдачи. */
  def firstAdId   : Option[String]

  /** Аккамулятор уже загруженных с сервера focused-карточек. */
  def ads         : Map[Int, FocAd]

  /** Кол-во уже загруженных карточек. */
  def loadedCount : Int

  /**
   * Общее кол-во карточек со всех возможных выборов в рамках задачи.
   * Если None, значит точное кол-во пока не известно.
   */
  def totalCount  : Option[Int]

  /** Текущая блоковая длина карусели. */
  def carLen : Int

  /** Сдвиг div'а карусели по X в px.
    * Используется как поправка при дальнейшей анимации по X.
    * Выставляется при открытии начальной карточки с непервым порядковым номером. */
  def carLeftPx: Int

}


/**
 * Реализация контейнера данных состояния FSM Focused-выдачи.
 * Контейнер собирается ещё до открытия focused-выдачи для передачи начальных данных.
 */
case class MFocSd(
  override val currIndex   : Int,
  override val firstAdId   : Option[String],
  override val carLeftPx   : Int                 = 0,
  override val ads         : Map[Int, FocAd]     = Map.empty,
  override val loadedCount : Int                 = 0,
  override val totalCount  : Option[Int]         = None,
  override val carLen      : Int                 = 0
)
  extends IFocSd
