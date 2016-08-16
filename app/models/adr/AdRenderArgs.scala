package models.adr

import models.MImgSizeT
import models.im._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.03.15 17:34
 * Description: Модель и модельная утиль для доступа к отрендеренным в картинки карточкам.
 */


/** Абстрактные параметры для рендера. Даже wkhtml этого обычно достаточно. */
trait IAdRenderArgs {

  /** Ссылка на страницу, которую надо отрендерить. */
  def src     : String

  /** 2015.03.06 ЭТО 100% !!ОБЯЗАТЕЛЬНЫЙ!! размер окна браузера и картинки (если кроп не задан) и не обсуждается.
    * В доках wkhtml обязательность этого параметра не отражена толком, а --height в man вообще не упоминается. */
  def scrSz   : MImgSizeT

  /** Качество сжатия результирующей картинки. */
  def quality : Option[Int]

  /** Формат сохраняемой картинки. */
  def outFmt  : OutImgFmt

}


/** Трейт для реализации wrapping-логики над полями какого-либо инстанса [[IAdRenderArgs]]. */
trait IAdRenderArgsWrapper extends IAdRenderArgs {
  def _underlying: IAdRenderArgs

  override def src      = _underlying.src
  override def scrSz    = _underlying.scrSz
  override def quality  = _underlying.quality
  override def outFmt   = _underlying.outFmt

}


case class MAdRenderArgs(
  src         : String,
  scrSz       : MImgSizeT,
  outFmt      : OutImgFmt,
  quality     : Option[Int]
)
  extends IAdRenderArgs



/** Интерфейс компаньона-генератора параметров. Полезен для доступа к абстрактному рендереру. */
trait IAdRendererCompanion {

  /** Дефолтовое значение quality, если не задано. */
  def qualityDflt(scrSz: MImgSizeT, fmt: OutImgFmt): Option[Int] = None

}

