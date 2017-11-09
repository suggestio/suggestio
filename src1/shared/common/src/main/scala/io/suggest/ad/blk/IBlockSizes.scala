package io.suggest.ad.blk

import enumeratum.values.{IntEnum, IntEnumEntry}
import io.suggest.dev.MSzMult
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.08.17 21:40
  * Description: marker-trait для моделей размера блока.
  *
  * Product форсирует использование case object'ов.
  * Это нужно в react-редакторе карточек для унификации сообщений по ширинам и длинам.
  */
trait IBlockSizes[T <: IBlockSize] extends IntEnum[T] with Product {

  /** Нижнее значение модели. */
  def min: T

  /** Верхнее значение модели. */
  def max: T


  /** Найти предшествующий элемент модели. */
  def previousOf(x: T): Option[T] = {
    val i0 = values.indexOf( x )
    if (i0 <= 0) {
      None
    } else {
      Some( values(i0 - 1) )
    }
  }

  /** Найти следующий элемент модели. */
  def nextOf(x: T): Option[T] = {
    val i0 = values.indexOf( x )
    val i2 = i0 + 1
    if (values.isDefinedAt(i2)) {
      Some( values(i2) )
    } else {
      None
    }
  }

}


/** Статическая утиль для поддержки подсистемы размеров блока. */
object IBlockSize {

  /** Пересчитать размер стороны блока для случаев, когда используется настраиваемый межблоковый интервал.
    *
    * Для изначальных (нерастянутых) сторон блоков можно вывести соотношение:
    * 140 + 20 = 160 = 160 * 1
    * 300 + 20 = 320 = 160 * 2
    * 460 + 20 = 480 = 160 * 3
    * ...
    * x + padding = 160 * relSz
    *
    * x = 160 * relSz - padding
    *
    * @param side Исходный размер стороны блока.
    * @param padding Кол-во пикселей между двумя блоками (по вертикали или горизонтали).
    *                Логичнее использовать padding каждого блока (10), но он по факту нигде не используется.
    *                Везде используется цельный паддинг (20), который суть есть удвоенное расстояние между блоками.
    * @return Новый размер стороны, который надо использовать вместо side.value.
    */
  def rePadSizePx(side: IBlockSize, padding: BlockPadding): Int = {
    val basePadding = BlockPaddings.base
    if (padding ==* basePadding) {
      side.value
    } else {
      (BlockWidths.min.value + basePadding.value) * side.relSz - padding.value
    }
  }


  /** Модифицировать szMult путём пересчёта на указанный padding.
    * Так можно аккуратно растянуть блоки плитки, чтобы занять пространство между ними.
    *
    * @param szMult Исходный мультипликатор размера для плитки с BlockPaddings.base.
    * @param padding Выставленный padding плитки.
    * @return Обновлённый мультипликатор размера для применения внутри блока.
    *         None, если обновления не требуется.
    */
  def szMultPaddedOpt(szMult: MSzMult, padding: BlockPadding): Option[MSzMult] = {
    for {
      diffToBasePx <- BlockPaddings.diffToBasePx( padding )
    } yield {
      // Базовый размер -- всегда один и тот же.
      // TODO Отвязаться от width к более абстрактным константам.
      val baseBlockSzMin = BlockWidths.min.value
      // Увеличенный размер минимального блока:
      val newBlockSzMin = baseBlockSzMin + diffToBasePx
      val szMultD = szMult.toDouble * newBlockSzMin / baseBlockSzMin
      MSzMult.fromDouble( szMultD )
    }
  }

}

/** Трейт-маркер какого-то стабильного размера блока. */
trait IBlockSize
  extends IntEnumEntry
  with Product
{

  /** Нормированный размер в единицах размера. Для ширины по сути - 1 или 2. */
  def relSz: Int

}
