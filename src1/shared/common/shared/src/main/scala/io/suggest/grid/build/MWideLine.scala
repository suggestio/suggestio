package io.suggest.grid.build

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
  * @param heightPx Высота (кол-во занятых пикселей высоты).
  */
case class MWideLine(
                      startLine : Int,
                      heightPx  : Int,
                    ) {

  /** Индекс последней занимаемой строки. */
  def lastLine = nextLine - 1

  /** Номер строки, которая идёт следующей после этого wide-резерва. */
  def nextLine = startLine + heightPx

}
object MWideLine {

  @inline implicit def univEq: UnivEq[MWideLine] = UnivEq.derive


  implicit class WideOpsExt(val wide: MWideLine ) extends AnyVal {

    /** Узнать, пересекается ли этот отрезок с указанным.
      *
      * @see [[https://stackoverflow.com/a/3269471]]
      */
    def overlaps(other: MWideLine): Boolean = {
      wide.startLine <= other.lastLine &&
      other.startLine <= wide.lastLine
    }

  }


  implicit class WidesCollOpsExt(val wides: TraversableOnce[MWideLine] ) extends AnyVal {

    def overlaps(mwl: MWideLine): Boolean =
      wides.exists( mwl.overlaps )

  }

}

