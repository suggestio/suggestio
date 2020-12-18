package io.suggest.maps.m

import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.2020 13:14
  * Description: Модель данных состояния данных размещения.
  */
object MAdvGeoS {

  implicit final class AdvGeoMapExt(private val agm: MAdvGeoS) extends AnyVal {

    def radEnabled: Option[MRad] =
      agm.rad.filter(_.enabled)

  }

  val mmap = GenLens[MAdvGeoS]( _.mmap )
  val rad = GenLens[MAdvGeoS]( _.rad )
  val existAdv = GenLens[MAdvGeoS]( _.existAdv )
  val radPopup = GenLens[MAdvGeoS]( _.radPopup )

  @inline implicit def univEq: UnivEq[MAdvGeoS] = UnivEq.derive

}


/** Контейнер данных состояния, связанных с географической картой и её наполнения.
  *
  * @param mmap Сама карта.
  * @param rad Круг размещения.
  * @param existAdv Данные по текущим рекламным размещениям на карте.
  * @param radPopup Состояние отображения попапа над кружком размещения.
  */
final case class MAdvGeoS(
                           mmap          : MMapS,
                           rad           : Option[MRad],
                           existAdv      : MExistGeoS              = MExistGeoS(),
                           radPopup      : Boolean                 = false,
                         )
