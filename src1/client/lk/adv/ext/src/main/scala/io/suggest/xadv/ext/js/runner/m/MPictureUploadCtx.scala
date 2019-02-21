package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.{MPictureUploadMode, MPictureUploadModes}
import io.suggest.sjs.common.model.{FromStringT, IToJsonDict}

import scala.scalajs.js
import scala.scalajs.js.WrappedDictionary
import io.suggest.adv.ext.model.ctx.PicUploadCtx._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 14:17
 * Description: Модель представления данных для аплоада картинки в хранилище сервиса.
 */
object MPictureUploadCtx extends FromStringT {
  override type T = MPicUploadCtxT

  override def fromJson(raw: js.Any): T = {
    val d = raw.asInstanceOf[js.Dictionary[js.Dynamic]] : WrappedDictionary[js.Dynamic]
    val modeOpt = d.get(MODE_FN)
      .map(_.toString)
      .flatMap(MPictureUploadModes.withValueOpt)
    if (modeOpt contains MPictureUploadModes.S2s) {
      MPicS2sUploadCtx(
        url       = d(URL_FN).toString,
        partName  = d(PART_NAME_FN).toString
      )
    } else {
      throw new IllegalArgumentException("Unknown upload mode: " + modeOpt)
    }
  }

}


/** Общий код всех возможных типов аплода. */
trait MPicUploadCtxT extends IToJsonDict {
  def mode: MPictureUploadMode
  override def toJson = js.Dictionary[js.Any](
    MODE_FN -> mode.value
  )
}

case class MPicS2sUploadCtx(url: String, partName: String) extends MPicUploadCtxT {
  override def mode = MPictureUploadModes.S2s

  override def toJson = {
    val d = super.toJson
    d.update(URL_FN, url)
    d.update(PART_NAME_FN, partName)
    d
  }
}

