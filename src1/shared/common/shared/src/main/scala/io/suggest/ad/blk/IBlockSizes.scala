package io.suggest.ad.blk

import enumeratum.values.{IntEnum, IntEnumEntry}

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


/** Трейт-маркер какого-то стабильного размера блока. */
trait IBlockSize
  extends IntEnumEntry
  with Product
{

  /** Нормированный размер в единицах размера. Для ширины по сути - 1 или 2. */
  def relSz: Int

}
