package models

import play.api.libs.Files.TemporaryFileCreator

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.07.2020 11:01
  */
package object mup {

  /** Расширенное API для TemporaryFileCreator, получаемого через _getFileCreator(). */
  implicit final class FileHandlerExt( private val fileCreator: TemporaryFileCreator ) {

    /** Извлечь LocalImg Args. */
    def localImgArgsOpt: Option[MLocalImgFileCreatorArgs] = {
      fileCreator match {
        case lifc: LocalImgFileCreator => Some( lifc.liArgs )
        case _ => None
      }
    }


    /** Если TemporaryFileCreator содержит id, то вернуть его. */
    def customNodeIdOpt: Option[String] = {
      localImgArgsOpt
        .map( _.mLocalImg.dynImgId.mediaId )
    }

  }

}
