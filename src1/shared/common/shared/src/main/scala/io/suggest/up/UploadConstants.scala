package io.suggest.up

import io.suggest.crypto.hash.{MHash, MHashes}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.04.15 15:49
 * Description: Константы для загрузки файлов: js и формы.
 */
object UploadConstants {

  /** Поле файла,  */
  def MPART_FILE_FN               = "uf"

  /** Байтовая длина файла, ниже которой НЕ используется flow.js.
    * Для очень малых файлов быстрее использовать прямую заливку, а не flow.js.
    */
  final def FLOWJS_DONT_USE_BELOW_BYTELEN = 32768

  /** Константы умной заливки файлов второго поколения. */
  object CleverUp {

    /**
      * Какими алгоритмами требуется хэшировать файл, загружаемый на сервер?
      * И на клиенте, и на сервере.
      */
    final def UPLOAD_FILE_HASHES: Set[MHash] =
      Set.empty[MHash] + MHashes.Sha1 + MHashes.Sha256

    final def UPLOAD_CHUNK_HASH: MHash =
      MHashes.Sha1

  }

  def MIN_FILE_SIZE_BYTES = 64

  /** Ограничение максимального размера закачки в байтах. */
  def TOTAL_SIZE_LIMIT_BYTES = 50 * 1024*1024

}
