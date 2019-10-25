package io.suggest.ad.blk

import enumeratum.values.{IntEnum, IntEnumEntry}
import io.suggest.common.empty.OptionUtil
import io.suggest.enum2.EnumeratumUtil
import play.api.libs.json.Format
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.11.17 11:34
  * Description: Модель допустимых пустых пространств вокруг одного блока.
  *
  * Изначально было так: два блока рядом -- 20 пикселей между ними, т.е. по 10 с каждого блока.
  * Потом возникла необходимость убрать межблоковые расстояния.
  */
object BlockPaddings extends IntEnum[BlockPadding] {

  /** Плитка без интервалов между блоками. */
  //case object Bp0 extends BlockPadding(0)

  /** Исторический интервал между блоками. */
  case object Bp20 extends BlockPadding(20)


  override val values = findValues

  def min = values.head
  def max = values.last

  /** Исторический базовый паддинг.
    * Через это значение выражены все [[BlockWidths]] и [[BlockHeights]].
    * Должен ВСЕГДА указывать на 20 пикселей.
    */
  final def base = Bp20

  /** Дефолтовое значение, когда padding не задан. */
  def default = base


  /** Вычислить разницу между базовым размером и указанным.
    *
    * @param bp Относительно какого паддинга считать.
    * @return Some() с пиксельной разностью внутри.
    *         None Если разницы нет, то и результата нет.
    */
  def diffToBasePx(bp: BlockPadding): Option[Int] = {
    val _base = base
    OptionUtil.maybe( bp !=* _base) {
      _base.value - bp.value
    }
  }

}


/** Класс одного варианта block padding'а. */
sealed abstract class BlockPadding(override val value: Int) extends IntEnumEntry


object BlockPadding {

  /** Поддержка play-json. */
  implicit def BLOCK_PADDING_FORMAT: Format[BlockPadding] = {
    EnumeratumUtil.valueEnumEntryFormat( BlockPaddings )
  }

  @inline implicit def univEq: UnivEq[BlockPadding] = UnivEq.derive

  implicit class BlockPaddingOps(val bp: BlockPadding) extends AnyVal {
    def fullBetweenBlocksPx: Int = bp.value
    def outlinePx: Int = bp.value / 2
  }

  /** Утиль для опциональных [[BlockPadding]]. */
  implicit class BlockPaddingOptOps(val opt: Option[BlockPadding]) extends AnyVal {

    def getOrDefault = opt getOrElse BlockPaddings.default

  }

}


