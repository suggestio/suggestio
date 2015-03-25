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

  override def fromJson(raw: js.Any): T = {
    val d = raw.asInstanceOf[js.Dictionary[js.Dynamic]] : WrappedDictionary[js.Dynamic]
    MAdPictureCtx(
      size    = d.get(SIZE_FN).map(MSize2D.fromJson),
      upload  = d.get(UPLOAD_FN).map(MPictureUploadCtx.fromJson),
      url     = d.get(SIO_URL_FN).map(_.toString),
      saved   = d.get(SAVED_FN).map(_.toString)
    )
  }
}


case class MAdPictureCtx(
  size    : Option[IMSize2D]        = None,
  upload  : Option[MPicUploadCtxT]  = None,
  url     : Option[String]          = None,
  saved   : Option[String]          = None
) extends IToJsonDict {

  override def toJson = {
    val d = js.Dictionary.empty[js.Any]
    if (size.isDefined)
      d.update(SIZE_FN, size.get.toJson)
    if (upload.isDefined)
      d.update(UPLOAD_FN, upload.get.toJson)
    if (url.isDefined)
      d.update(SIO_URL_FN, url.get)
    if (saved.isDefined)
      d.update(SAVED_FN, saved.get)
    d
  }
}

