package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.MAdPictureCtx._

import scala.scalajs.js
import scala.scalajs.js.WrappedDictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 13:51
 * Description: Инфа по картинке.
 */

object MAdPictureCtx extends FromStringT {
  override type T = MAdPictureCtx

  override def fromDyn(raw: js.Dynamic): T = {
    val d = raw.asInstanceOf[js.Dictionary[js.Dynamic]] : WrappedDictionary[js.Dynamic]
    MAdPictureCtx(
      size    = d.get(SIZE_FN).map(MSize2D.fromDyn),
      upload  = d.get(UPLOAD_FN).map(MPictureUploadCtx.fromDyn),
      url     = d.get(SIO_URL_FN).map(_.toString),
      saved   = d.get(SAVED_FN).map(_.toString)
    )
  }
}


case class MAdPictureCtx(
  size    : Option[MSize2D],
  upload  : Option[MPicUploadCtxT],
  url     : Option[String],
  saved   : Option[String]
) {

  def toJson: js.Dynamic = {
    val lit = js.Dynamic.literal()
    if (size.isDefined)
      lit.updateDynamic(SIZE_FN)(size.get.toJson)
    if (upload.isDefined)
      lit.updateDynamic(UPLOAD_FN)(upload.get.toJson)
    if (url.isDefined)
      lit.updateDynamic(SIO_URL_FN)(url.get)
    if (saved.isDefined)
      lit.updateDynamic(SAVED_FN)(saved.get)
    lit
  }

}

