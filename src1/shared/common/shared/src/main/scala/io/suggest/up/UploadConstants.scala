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


  /** Константы умной заливки файлов второго поколения. */
  object CleverUp {

    /**
      * Какими алгоритмами требуется хэшировать файл, загружаемый на сервер?
      * И на клиенте, и на сервере.
      */
    final def UPLOAD_FILE_HASHES: Set[MHash] =
      Set[MHash](MHashes.Sha1, MHashes.Sha256)

  }

}
