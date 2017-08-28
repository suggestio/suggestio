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
trait IBlockSizes extends Product { this: IntEnum[_ <: IBlockSize] =>
}


/** Трейт-маркер какого-то стабильного размера блока. */
trait IBlockSize
  extends IntEnumEntry
  with Product
