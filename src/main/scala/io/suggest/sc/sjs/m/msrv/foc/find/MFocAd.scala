package io.suggest.sc.sjs.m.msrv.foc.find

import io.suggest.sc.focus.FocusedRenderNames._
import scala.scalajs.js.{Dictionary, Any}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.06.15 17:36
 * Description: Wrap-модель одной focused-карточки вместе с метаданными.
 */

case class MFocAd(json: Dictionary[Any]) extends IMFocAd {

  override def html: String = json(HTML_FN).toString

  override val mode: MFocRenderMode = {
    val modeId = json(MODE_FN).toString
    MFocRenderModes.withName(modeId)
  }

  override def index: Int = {
    json(INDEX_FN).asInstanceOf[Int]
  }
}

/** Интерфейс экземпляров модели. */
trait IMFocAd {

  /** Отрендеренная карточка. */
  def html: String

  /** Режим рендера сервером. */
  def mode: MFocRenderMode

  /** Человеческий порядковый номер. */
  def index: Int
}

