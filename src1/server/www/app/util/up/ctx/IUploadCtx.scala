package util.up.ctx

import io.suggest.common.geom.d2.MSize2di

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.02.2020 17:42
  * Description: Интерфейс для различных Upload-контекстов.
  */
trait IUploadCtx {

  def imageWh: Option[MSize2di]

  def validateFileContentEarly(): Boolean

  def validateFileFut(): Future[Boolean]

  def imageHasTransparentColors(): Option[Future[Boolean]]

}
