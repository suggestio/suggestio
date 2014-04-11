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

trait MAdT[T <: MAdT[T]]
  extends EMProducerId[T]
  with EMReceivers[T]
  with EMPrioOpt[T]
  with EMUserCatId[T]
  with EMDateCreated[T]
  with EMAdOffers[T]
  with EMImg[T]
  with EMAdPanelSettings[T]
  with EMLogoImg[T]
  with EMTextAlign[T]
{

  def producerId : String
  def receivers  : Receivers_t
  def prio       : Option[Int]
  def userCatId  : Option[String]
  def dateCreated : DateTime

  // Визуально-отображаемые юзеру поля.
  def offers     : List[AdOfferT]
  def img        : MImgInfo
  def panel      : Option[AdPanelSettings]
  def logoImgOpt : Option[MImgInfo]
  def textAlign  : Option[TextAlign]

}


/** trait для рантаймового враппинга экземпляра абстрактной рекламной карточки.
  * Следует помнить, что это только враппинг интерфейса, который на json никак не отражается.
  * Для всего остального функционала надо подмешивать соотв. не-Mut трейты в добавок к этому.
  * @tparam T Тип оборачивамой карточки
  * @tparam MyT Тип этой (оборачивающей) карточки.
  */
trait MAdWrapperT[T <: MAdT[T], MyT <: MAdWrapperT[T,MyT]] extends MAdT[MyT] {
  def wrappedAd: MAdT[T]

  override def userCatId = wrappedAd.userCatId
  override def prio = wrappedAd.prio
  override def panel = wrappedAd.panel
  override def producerId = wrappedAd.producerId
  override def textAlign = wrappedAd.textAlign
  override def offers = wrappedAd.offers
  override def receivers = wrappedAd.receivers
  override def id = wrappedAd.id
  override def dateCreated = wrappedAd.dateCreated
  override def img = wrappedAd.img
  override def logoImgOpt = wrappedAd.logoImgOpt

  /** Перед сохранением надо также проверять состояние исходного экземпляра. */
  override def isFieldsValid: Boolean = {
    wrappedAd.isFieldsValid && super.isFieldsValid
  }

}
