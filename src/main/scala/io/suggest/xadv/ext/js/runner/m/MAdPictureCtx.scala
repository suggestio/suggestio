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
      sizeId  = d.get(SIZE_FN).map(_.toString),
      upload  = d.get(UPLOAD_FN).map(MPictureUploadCtx.fromJson),
      url     = d.get(SIO_URL_FN).map(_.toString),
      saved   = d.get(SAVED_FN).map(_.toString)
    )
  }
}

/**
 * Экземпляр распарсенного контекста картинки.
 * @param sizeId id размера в рамках размерной линейки сервиса.
 * @param upload Инфа по загрузке картинки в хранилище сервиса, если есть.
 * @param url Ссылка.
 * @param saved Инфа по сохраненной загруженной картинке в хранилище сервиса.
 */
case class MAdPictureCtx(
  sizeId  : Option[String]          = None,
  upload  : Option[MPicUploadCtxT]  = None,
  url     : Option[String]          = None,
  saved   : Option[String]          = None
) extends IToJsonDict {

  override def toJson = {
    val d = js.Dictionary.empty[js.Any]
    if (sizeId.isDefined)
      d.update(SIZE_FN, sizeId.get)
    if (upload.isDefined)
      d.update(UPLOAD_FN, upload.get.toJson)
    if (url.isDefined)
      d.update(SIO_URL_FN, url.get)
    if (saved.isDefined)
      d.update(SAVED_FN, saved.get)
    d
  }
}

