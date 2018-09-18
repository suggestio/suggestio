package io.suggest.grid.build

import io.suggest.ad.blk.BlockHeight
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.12.17 17:37
  * Description: Модели для представления широких строк в плитке.
  *
  * Модели ориентированы на двух-фазное построение плитки:
  * - Начальная сборка плитки. Собирается плитка без учёта wide-блоков: MWideLines().push().
  * - Новая сборка плитки, но уже с использованием широких блоков: MWideLines().extract()
  */


/** Инфа об одной или нескольких строках, занятых под широкие карточки.
  *
  * @param startLine Номер строки, с которого можно начинать резервирование строк под wide-карточку.
  * @param height Высота (кол-во занятых строк плитки).
  */
case class MWideLine(
                      startLine : Int,
                      height    : BlockHeight
                    ) {

  /** Индекс последней занимаемой строки. */
  def lastLine = nextLine - 1

  /** Номер строки, которая идёт следующей после этого wide-резерва. */
  def nextLine = startLine + height.relSz

  def withStartLine(startLine: Int) = copy(startLine = startLine)

  def range = startLine until nextLine

  /** Узнать, пересекается ли этот отрезок с указанным.
    *
    * @see [[https://stackoverflow.com/a/3269471]]
    */
  def overlaps(other: MWideLine): Boolean = {
    startLine <= other.lastLine &&
      other.startLine <= lastLine
  }

}
object MWideLine {
  @inline implicit def univEq: UnivEq[MWideLine] = UnivEq.derive
}

