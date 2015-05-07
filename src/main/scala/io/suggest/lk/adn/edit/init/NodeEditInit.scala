package io.suggest.lk.adn.edit.init

import io.suggest.lk.img.JsRemoveImgInitT
import io.suggest.lk.upload._
import io.suggest.sjs.common.controller.{IInitDummy, InitRouter}
import io.suggest.sjs.common.util.{SafeSyncVoid, SjsLogger}
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import io.suggest.adn.edit.NodeEditConstants._
import org.scalajs.jquery.{JQuery, jQuery}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 15:16
 * Description: JS для формы редактирования узла.
 */

trait NodeEditInitRouter extends InitRouter {

  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.LkNodeEditForm) {
      Future {
        new LkNodeEditFormEvents()
          .init()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }

}


/** Поддержка node для gallery. */
class LkNodeEditFormEvents
  extends SjsLogger
  with SafeSyncVoid
  with IInitDummy
  with NodeGalleryInit
  with JsRemoveImgInitT
  with InitInputFileUploadOnChange with AjaxFileUpload with ImgUploadRenderOverlay with MultiUpload
{

  /** Контейнеры, в рамках которых идет изолированная обработка элементов. */
  override protected lazy val _imgInputContainers: Seq[JQuery] = {
    Iterator(
      NODE_GALLERY_DIV_ID,
      NODE_LOGO_DIV_ID,
      NODE_WELCOME_DIV_ID
    ).map { id =>
      jQuery("#" + id)
    }
    .toStream
  }

}


