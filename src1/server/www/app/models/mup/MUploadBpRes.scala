package models.mup

import io.suggest.img.MImgFmt
import models.im.MLocalImg
import play.api.libs.Files.TemporaryFile
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
                               localImgArgs   : Option[MLocalImgFileCreatorArgs],
                             ) {

  /** Надо ли удалять залитый файл? */
  def isDeleteFileOnSuccess: Boolean = {
    // Да, если файл не подхвачен какой-либо файловой моделью (MLocalImg, например).
    localImgArgs.isEmpty
  }

  lazy val imgFmt: Option[MImgFmt] =
    localImgArgs.map(_.mImgFmt)

}


/** Модель аргументов для LocalImg FileCreator. */
final case class MLocalImgFileCreatorArgs(
                                           mLocalImg    : MLocalImg,
                                           mImgFmt      : MImgFmt,
                                         )
