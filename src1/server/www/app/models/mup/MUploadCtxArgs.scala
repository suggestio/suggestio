package models.mup

import io.suggest.file.MimeUtilJvm
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.02.2020 17:32
  * Description: Модель-контейнер основных данных для контекстов аплоада.
  */
case class MUploadCtxArgs(
                           filePart     : MultipartFormData.FilePart[TemporaryFile],
                           uploadArgs   : MUploadTargetQs,
                           uploadBpRes  : MUploadBpRes,
                         ) {

  val path = filePart.ref.path

  val file = path.toFile

  lazy val fileLength = file.length()

  def declaredMime = uploadArgs.fileProps.mimeType

  lazy val detectedMimeTypeOpt: Option[String] = {
    MimeUtilJvm.probeContentType(path)
  }

}

