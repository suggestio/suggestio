package io.suggest.adv.geo

import io.suggest.geo.IGeoPoint

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.12.16 16:53
  * Description: Модели состояния формы георазмещения, расшаренные между клиентом и сервером.
  * Из-за различия конкретных под-моделей моделей на клиенте и на сервере, тут лишь только интерфейсы.
  */

/** Корневая модель состояния формы circuit. */
trait IRoot {

  /** id георазмещаемой рекламной карточки. */
  def adId: String

  /** Состояние карты. */
  def mapState: IMapState

  //val adv4free        : UndefOr[IAdv4FreeProps]   = js.native

  /** состояние круга размещения, если есть. */
  //val circle          : UndefOr[ICircleJson]      = js.native
}


/** Интерфейс модели состояния карты. */
trait IMapState {

  /** Текущий центр карты. */
  def center: IGeoPoint

  /** Текущий зум карты. */
  def zoom: Int

}


/** Интерфейс модели описания круга на географической карте. */
trait ICircleJson {

  /** Координаты центра круга. */
  def center: IGeoPoint

  /** Радиус круга. */
  def radius: Double

}
