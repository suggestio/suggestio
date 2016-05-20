package io.suggest.sc.sjs.m.msrv.foc.find

import io.suggest.sc.focus.FocAdProto._
import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.06.15 17:36
 * Description: Wrap-модель одной focused-карточки вместе с метаданными.
 */
case class MFocAd(json: Dictionary[Any]) extends IFocAd {

  /** Вспомогательная функция для быстрого чтения примитивных типов данных из полей raw JSON. */
  private def _get[T](key: String): T = {
    json(key).asInstanceOf[T]
  }

  override def madId        = _get[String](MAD_ID_FN)
  override def bodyHtml     = _get[String](BODY_HTML_FN)
  override def controlsHtml = _get[String](CONTROLS_HTML_FN)
  override def producerId   = _get[String](PRODUCER_ID_FN)
  override def index        = _get[Int](INDEX_FN)

  /** Порядковый номер, отображаемый юзеру. */
  def humanIndex            = _get[Int](HUMAN_INDEX_FN)

  override def toString = super.toString

}


/** Интерфейс метаданных экземпляра focused-карточки. */
trait IFocAdMeta {

  /** id рекламной карточки. */
  def madId: String

  /** id продьюсера рекламной карточки. */
  def producerId: String

  /** Порядковый номер. */
  def index: Int

  override def toString: String = {
    getClass.getSimpleName + "(" +
      madId + "," +
      producerId + "," +
      index +
      ")"
  }

}


/** Интерфейс экземпляров модели. */
trait IFocAd extends IFocAdMeta {

  /** Отрендеренная рекламная карточка. */
  def bodyHtml: String

  /** Внешние элементы рекламной карточки. */
  def controlsHtml: String

  /** Копирование данных модели [[IFocAd]] в экземпляр [[MFocAdImpl]]. */
  def mFocAdImpl: MFocAdImpl = {
    MFocAdImpl(
      madId         = madId,
      bodyHtml      = bodyHtml,
      controlsHtml  = controlsHtml,
      producerId    = producerId,
      index         = index
    )
  }

}


/** Дефолтовая реализация [[IFocAd]] без лишних данных. */
case class MFocAdImpl(
  override val madId        : String,
  override val bodyHtml     : String,
  override val controlsHtml : String,
  override val producerId   : String,
  override val index        : Int
)
  extends IFocAd
{
  override def mFocAdImpl = this
  override def toString = super.toString
}
