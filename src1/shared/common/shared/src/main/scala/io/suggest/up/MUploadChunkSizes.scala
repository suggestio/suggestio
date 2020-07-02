package io.suggest.up

import enumeratum.values._
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.06.2020 23:44
  * Description: Допустимые для системы chunk size'ы.
  */
object MUploadChunkSizes extends IntEnum[MUploadChunkSize] {

  // TODO Для сверх-медленных каналов следует потом организовать совсем малый chunk size.
  //case object S256k extends MUploadChunkSize( 256 * 1024 )

//  case object S_1mib extends MUploadChunkSize( 1048576 )
  case object S_1mib extends MUploadChunkSize( 512512 )

  def default: MUploadChunkSize = S_1mib
  def max = values.last

  override def values = findValues

}


sealed abstract class MUploadChunkSize(override val value: Int) extends IntEnumEntry
object MUploadChunkSize {

  @inline implicit def univEq: UnivEq[MUploadChunkSize] = UnivEq.derive

  implicit def uploadChunkSizeJson: Format[MUploadChunkSize] =
    EnumeratumUtil.valueEnumEntryFormat( MUploadChunkSizes )

}
