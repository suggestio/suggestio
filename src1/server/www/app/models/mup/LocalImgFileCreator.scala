package models.mup

import java.io.File
import java.nio.file.Path

import com.google.inject.assistedinject.Assisted
import javax.inject.Inject
import models.im.{MLocalImg, MLocalImgs}
import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.07.2020 10:27
  * Description: Реализация перехвата временных файлов сразу в MLocalImg-хранилище.
  */
final case class LocalImgFileCreator @Inject() (
                                                 @Assisted liArgs      : MLocalImgFileCreatorArgs,
                                                 mLocalImgs            : MLocalImgs,
                                               )
  extends TemporaryFileCreator
{ creator =>

  override def create(prefix: String, suffix: String): TemporaryFile =
    _create()

  override def create(path: Path): TemporaryFile =
    _create()

  private def _create(): TemporaryFile = {
    mLocalImgs.prepareWriteFile( liArgs.mLocalImg )
    LocalImgFile( mLocalImgs.fileOf( liArgs.mLocalImg ), creator )
  }

  override def delete(file: TemporaryFile): Try[Boolean] = {
    Try {
      mLocalImgs.deleteAllSyncFor( liArgs.mLocalImg.dynImgId.origNodeId )
      true
    }
  }

}


/** Модель аргументов для LocalImg FileCreator. */
final case class MLocalImgFileCreatorArgs(
                                           mLocalImg    : MLocalImg,
                                         )


/** Маскировка MLocalImg под TemporaryFile. */
final case class LocalImgFile(val _file: File, creator: LocalImgFileCreator) extends TemporaryFile {
  override def path: Path = _file.toPath
  override def file: File = _file
  override def temporaryFileCreator = creator
}


/** Guice DI Factory для инстансов [[LocalImgFileCreator]]. */
trait LocalImgFileCreatorFactory {
  def instance( liArgs: MLocalImgFileCreatorArgs ): LocalImgFileCreator
}
