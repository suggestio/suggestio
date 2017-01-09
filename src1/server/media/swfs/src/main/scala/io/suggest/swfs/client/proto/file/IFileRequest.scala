package io.suggest.swfs.client.proto.file

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 10:48
 * Description: Расшаренный между разными запросами трейт обращения к файлу в хранилище.
 */
object IFileRequest {

  def PROTO_DFLT = "http"

}


trait IFileRequest {
  
  /**
   * Используемый протокол.
   * @return Например "http".
   */
  def proto           : String

  /**
   * "Ссылка" на раздел (в терминологии seaweedfs).
   * @return Например "localhost:8080".
   */
  def volUrl          : String

  /** fid, т.е. раздел и id файла. */
  def fid             : String
  
  /** Сборка URL запроса. */
  def toUrl: String = {
    proto + "://" + volUrl + "/" + fid
  }

}
