package util.up.ctx

import java.nio.file.Path
import io.suggest.common.geom.d2.MSize2di
import io.suggest.file.MimeUtilJvm
import play.api.inject.Injector

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.02.2020 17:42
  * Description: Интерфейс для различных Upload-контекстов.
  */
trait IUploadCtx {

  def injector: Injector
  def mimeUtilJvm = injector.instanceOf[MimeUtilJvm]

  def path: Path
  def file = path.toFile
  lazy val fileLength = file.length()
  lazy val detectedMimeTypeOpt: Option[String] =
    mimeUtilJvm.probeContentType( path )

  def imageWh: Option[MSize2di]

  def validateFileContentEarly(): Boolean

  def validateFileFut(): Future[Boolean]

  def imageHasTransparentColors(): Option[Future[Boolean]]

}
