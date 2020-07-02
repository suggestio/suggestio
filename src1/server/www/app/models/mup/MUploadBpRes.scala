package models.mup

import io.suggest.img.MImgFmt
import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}
import play.api.mvc.MultipartFormData

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.02.2020 13:33
  * Description: Контейнер результата работы BodyParser'а при аплоаде.
  * Вынесен за пределы контроллера из-за проблем с компиляцией routes, если это inner-class.
  */

final case class MUploadBpRes(
                               data           : MultipartFormData[TemporaryFile],
                               fileCreator    : TemporaryFileCreator,
                             ) {

  def localImgArgs = fileCreator.localImgArgsOpt

  /** Надо ли удалять залитый файл? */
  def isDeleteFileOnSuccess: Boolean = {
    // Да, если файл не подхвачен какой-либо файловой моделью (MLocalImg, например).
    localImgArgs.isEmpty
  }

  lazy val imgFmt: Option[MImgFmt] =
    localImgArgs.map(_.mImgFmt)

}
