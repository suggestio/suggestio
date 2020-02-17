package util.up.ctx

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.02.2020 18:17
  * Description: Контекст, разрешающий любой файл.
  */
object AnyFileUploadCtx extends IUploadCtx {

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
