package io.suggest.xadv.ext.js.runner.m

import io.suggest.adv.ext.model.ctx.MExtTargetT

import scala.scalajs.js.{WrappedDictionary, Dictionary, Any}

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
  def fromJson(raw: Any): MExtTarget = {
    val d = raw.asInstanceOf[Dictionary[Any]] : WrappedDictionary[Any]
    MExtTarget(
      id          = d(ID_FN).toString,
      tgUrl       = d(URL_FN).toString,
      onClickUrl  = d(ON_CLICK_URL_FN).toString,
      name        = d.get(NAME_FN).map(_.toString),
      customRaw   = d.get(CUSTOM_FN)
    )
  }

}


import MExtTarget._


/** Логика динамической стороны модели вынесена в этот трейт. */
trait IMExtTarget extends IToJsonDict {
  /** Неизменяемый id цели на стороне backend'а suggest.io. */
  def id          : String

  /** URL страницы-цели. */
  def tgUrl       : String

  /** URL перехода при клике по размещенной карточки. */
  def onClickUrl  : String

  /** Название цели, если есть. */
  def name        : Option[String]

  /** Сырой кастомный контекст в виде JSON. */
  def customRaw   : Option[Any]

  /** Сериализация в JSON.
    * final чтобы был стабильный формат передачи независимо от реализаций этого трейта. */
  override final def toJson: Dictionary[Any] = {
    val d = Dictionary[Any] (
      ID_FN             -> id,
      URL_FN            -> tgUrl,
      ON_CLICK_URL_FN   -> onClickUrl
    )
    if (name.isDefined)
      d.update(NAME_FN, name.get)
    if (customRaw.isDefined)
      d.update(CUSTOM_FN, customRaw.get)
    d
  }
}

/** Трейт-враппер для wrap-реализаций [[IMExtTarget]]. */
trait IMExtTargetWrapper extends IMExtTarget {
  /** Заврапанный экземпляр [[IMExtTarget]] */
  def tgUnderlying: IMExtTarget

  override def id         = tgUnderlying.id
  override def customRaw  = tgUnderlying.customRaw
  override def onClickUrl = tgUnderlying.onClickUrl
  override def name       = tgUnderlying.name
  override def tgUrl      = tgUnderlying.tgUrl
}


/** Реализация модели. */
case class MExtTarget(
  id          : String,
  tgUrl       : String,
  onClickUrl  : String,
  name        : Option[String],
  customRaw   : Option[Any]
) extends IMExtTarget
