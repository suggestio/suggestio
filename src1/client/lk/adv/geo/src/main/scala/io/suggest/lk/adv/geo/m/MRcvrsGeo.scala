package io.suggest.lk.adv.geo.m

import diode.FastEq
import io.suggest.maps.nodes.MAdvGeoMapNodeProps
import io.suggest.sjs.common.geo.json.BooGjFeature

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.06.17 17:48
  * Description: Модель данных по географии ресиверов.
  */
object MRcvrsGeo {

  implicit object MRcvrsGeoFastEq extends FastEq[MRcvrsGeo] {
    override def eqv(a: MRcvrsGeo, b: MRcvrsGeo): Boolean = {
      a.features eq b.features
    }
  }

}


/** Класс-контейнер отображаемых юзеру гео-данных по ресиверам.
  *
  * @param features Распарсенный ответ сервера с разнотипными элементами карты.
  */
case class MRcvrsGeo(
                      features    : Seq[BooGjFeature[MAdvGeoMapNodeProps]]
                    )
