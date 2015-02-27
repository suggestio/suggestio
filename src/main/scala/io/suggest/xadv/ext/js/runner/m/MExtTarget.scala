package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.MExtTargetT

import scala.scalajs.js
import scala.scalajs.js.WrappedDictionary

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.02.15 15:23
 * Description: Модель содеримого поля mctx.target. Там ссылка и прочая инфа по текущей цели.
 */
object MExtTarget extends MExtTargetT {

  /**
   * Десериализация из JSON.
   * @param raw Сорец.
   * @return Экземпляр MExtTarget.
   */
  def fromJson(raw: js.Any): MExtTarget = {
    val d = raw.asInstanceOf[js.Dictionary[String]] : WrappedDictionary[String]
    MExtTarget(
      tgUrl       = d(URL_FN),
      onClickUrl  = d(ON_CLICK_URL_FN)
    )
  }

}


import MExtTarget._


case class MExtTarget(
  tgUrl       : String,
  onClickUrl  : String
) {

  /** Сериализация в JSON. */
  def toJson: js.Dictionary[String] = {
    js.Dictionary[String] (
      URL_FN            -> tgUrl,
      ON_CLICK_URL_FN   -> onClickUrl
    )
  }
}


