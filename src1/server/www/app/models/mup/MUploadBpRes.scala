package models.mup

import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}
import play.api.mvc.MultipartFormData

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.02.2020 13:33
  * Description: Контейнер результата работы BodyParser'а при аплоаде.
  * Вынесен за пределы контроллера из-за проблем с компиляцией routes, если это inner-class.
  *
  * @param isDeleteFileOnComplete Надо ли удалять залитый файл?
  *                              Да, если файл не подхвачен какой-либо файловой моделью (MLocalImg, например).
  */

final case class MUploadBpRes(
                               data                     : MultipartFormData[TemporaryFile],
                               fileCreator              : TemporaryFileCreator,
                               isDeleteFileOnComplete    : Boolean,
                             ) {

  def localImgArgs = fileCreator.localImgArgsOpt

}
