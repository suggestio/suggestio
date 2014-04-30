package io.suggest.ym.model.ad

import io.suggest.ym.model._
import io.suggest.ym.model.common._
import org.joda.time.DateTime
import io.suggest.model._
import io.suggest.ym.model.common.EMReceivers.Receivers_t
import io.suggest.model.common.{EMDateCreated, EMPrioOpt}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.04.14 16:05
 * Description: Интерфейсы для рекламных карточек. Бывают разные карточки, все как бы в чем-то
 * рекламные, но все отличаются по своим целям и возможностям. Для всех них нужен общий интерфейс.
 */

trait MAdT
  extends EMProducerId
  with EMReceivers
  with EMPrioOpt
  with EMUserCatId
  with EMDateCreated
  with EMAdOffers
  with EMImg
  with EMBlockMeta
  with EMLogoImg
{
  override type T <: MAdT
}


/**
 * trait для рантаймового враппинга экземпляра абстрактной рекламной карточки.
 * Следует помнить, что это только враппинг интерфейса, который на json никак не отражается.
 * Для всего остального функционала надо подмешивать соотв. не-Mut трейты в добавок к этому.
 */
trait MAdWrapperT extends MAdT {
  def wrappedAd: MAdT

  override def userCatId = wrappedAd.userCatId
  override def prio = wrappedAd.prio
  override def producerId = wrappedAd.producerId
  override def offers = wrappedAd.offers
  override def receivers = wrappedAd.receivers
  override def id = wrappedAd.id
  override def dateCreated = wrappedAd.dateCreated
  override def imgOpt = wrappedAd.imgOpt
  override def logoImgOpt = wrappedAd.logoImgOpt
  override def blockMeta = wrappedAd.blockMeta

  /** Перед сохранением надо также проверять состояние исходного экземпляра. */
  override def isFieldsValid: Boolean = {
    wrappedAd.isFieldsValid && super.isFieldsValid
  }

}
