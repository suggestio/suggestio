package io.suggest.sc.sjs.m.msrv.foc.find

import io.suggest.sc.focus.FocAdProto._
import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.06.15 17:36
 * Description: Wrap-модель одной focused-карточки вместе с метаданными.
 */

case class MFocAd(json: Dictionary[Any]) extends IMFocAd {

  /** Вспомогательная функция для быстрого чтения примитивных типов данных из полей raw JSON. */
  private def _get[T](key: String): T = {
    json(key).asInstanceOf[T]
  }

  override def madId        = _get[String](MAD_ID_FN)
  override def bodyHtml     = _get[String](BODY_HTML_FN)
  override def controlsHtml = _get[String](CONTROLS_HTML_FN)
  override def producerId   = _get[String](PRODUCER_ID_FN)
  override def index        = _get[Int](INDEX_FN)

}

/** Интерфейс экземпляров модели. */
trait IMFocAd {

  /** id рекламной карточки. */
  def madId: String

  /** Отрендеренная рекламная карточка. */
  def bodyHtml: String

  /** Внешние элементы рекламной карточки. */
  def controlsHtml: String

  /** id продьюсера рекламной карточки. */
  def producerId: String

  /** Человеческий порядковый номер. */
  def index: Int

}

