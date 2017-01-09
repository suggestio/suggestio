package io.suggest.fio

import play.api.libs.iteratee.Enumerator

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 23:03
 * Description: Модель данных о результате чтения.
 */

trait IReadResponse extends IContentType {

  /** Содержимое файла. */
  def data: Enumerator[Array[Byte]]

  /** Размер файла. */
  def sizeB: Long

}


case class ReadResponse(
  override val contentType  : String,
  override val data         : Enumerator[Array[Byte]],
  override val sizeB        : Long
)
  extends IReadResponse
