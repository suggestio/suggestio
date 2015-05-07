package io.suggest.lk.ad.form.init

import io.suggest.ad.form.AdFormConstants
import io.suggest.lk.img.JsRemoveImgInitT
import io.suggest.lk.upload.{AjaxFileUpload, ImgUploadRenderOverlay, InitInputFileUploadOnChange}
import io.suggest.sjs.common.controller.{IInitDummy, InitRouter}
import io.suggest.sjs.common.util.SjsLogger
import org.scalajs.jquery.{JQuery, jQuery}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.05.15 18:31
 * Description: Система инициализации scala-js для формы создания/редактирования рекламных карточек.
 */
trait AdFormInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.AdForm) {
      Future {
        new AdFormInit()
          .init()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }

}


/** Инициализатор поддержки формы создания/редактирования карточек. */
class AdFormInit
  extends IInitDummy
  with SjsLogger
  with JsRemoveImgInitT
  with InitInputFileUploadOnChange with AjaxFileUpload with ImgUploadRenderOverlay
{
  /** Контейнеры, которые будут отработаны. */
  override protected def _imgInputContainers: TraversableOnce[JQuery] = {
    Seq(
      jQuery("#" + AdFormConstants.BG_IMG_CONTAINER_ID)
    )
  }
}
