package io.suggest.model.n2.media.storage

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.common.menum.play.EnumJsonReadsT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:04
 * Description: Модель типов используемых хранилищь для media-файлов.
 */
object MStorages extends EnumMaybeWithName with EnumJsonReadsT {

  /** Экземпляр модели. */
  protected[this] sealed class Val(val strId: String)
    extends super.Val(strId)

  override type T = Val


  /** Apache Cassandra.
    * SiO2 работал на кассандре в 2014-2015 годах. */
  val Cassandra: T = new Val("c")


  /** SeaWeedFS.
    * Хранилище на смену кассандре (oct.2015-...). */
  val SeaWeedFs: T = new Val("s")

}
