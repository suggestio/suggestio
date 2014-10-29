package util.img

import io.suggest.ym.model.common.MImgInfoT
import models.im.MImg
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.10.14 18:37
 * Description: Утиль для работы с логотипами. Исторически, она была разбросана по всему проекту.
 */
object LogoUtil {

  type LogoOpt_t = Option[MImg]

  def updateLogo(newLogo: LogoOpt_t, oldLogo: Option[MImgInfoT]): Future[Option[MImgInfoT]] = {
    val oldImgs = oldLogo
      .map { ii => MImg(ii.filename) }
      .toIterable
    ImgFormUtil.updateOrigImgFull(needImgs = newLogo.toSeq, oldImgs = oldImgs)
      .flatMap { vs => ImgFormUtil.optImg2OptImgInfo( vs.headOption ) }
  }

}
