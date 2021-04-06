package util.up.ctx

import java.nio.file.Path
import com.google.inject.assistedinject.Assisted
import play.api.inject.Injector

import javax.inject.Inject
import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.02.2020 18:17
  * Description: Контекст, разрешающий любой файл.
  */
final case class AnyFileUploadCtx @Inject() (
                                              @Assisted override val path   : Path,
                                              override val injector         : Injector,
                                            )
  extends IUploadCtx
{

  override def imageWh = None

  override def validateFileContentEarly(): Boolean = {
    // Лимиты
    true
  }

  override def validateFileFut(): Future[Boolean] = {
    Future.successful(true)
  }

  override def imageHasTransparentColors() = None

}


trait IAnyFileUploadCtxFactory {
  def make(path: Path): AnyFileUploadCtx
}
