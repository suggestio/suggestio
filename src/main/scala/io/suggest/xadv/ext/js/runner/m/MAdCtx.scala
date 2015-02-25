package io.suggest.xadv.ext.js.runner.m

import scala.scalajs.js
import io.suggest.adv.ext.model.ctx.MAdCtx._

import scala.scalajs.js.WrappedDictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.02.15 10:51
 * Description: JSON-инфа по одной размещаемой рекламной карточке.
 */
object MAdCtx extends FromStringT {
  override type T = MAdCtx

  override def fromDyn(dyn: js.Dynamic): T = {
    val d = dyn.asInstanceOf[js.Dictionary[js.Dynamic]] : WrappedDictionary[js.Dynamic]
    MAdCtx(
      madId   = d(ID_FN).toString,
      content = MAdContentCtx.fromDyn( d(CONTENT_FN) ),
      picture = d.get(PICTURE_FN)
        .map { MAdPictureCtx.fromDyn },
      scUrl   = d.get(SC_URL_FN)
        .map(_.toString)
    )
  }

}

case class MAdCtx(
  madId   : String,
  content : MAdContentCtx,
  picture : Option[MAdPictureCtx],
  scUrl   : Option[String]
) {

  def toJson: js.Dynamic = {
    val lit = js.Dynamic.literal()
    lit.updateDynamic(ID_FN)(madId)
    lit.updateDynamic(CONTENT_FN)(content.toJson)
    if (picture.nonEmpty)
      lit.updateDynamic(PICTURE_FN)(picture.get.toJson)
    if (scUrl.isDefined)
      lit.updateDynamic(SC_URL_FN)(scUrl.get)
    lit
  }
}
