package io.suggest.lk.adv.geo

import com.softwaremill.macwire._
import io.suggest.lk.adv.geo.r._
import io.suggest.lk.adv.geo.r.oms._
import io.suggest.lk.adv.geo.r.pop._
import io.suggest.lk.adv.geo.r.rcvr._
import io.suggest.lk.tags.edit.TagsEditModule

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2020 17:50
  * Description: Поддержка compile-time DI для формы георазмещения карточки.
  */
final class LkAdvGeoFormModule {

  import io.suggest.lk.LkCommonModule._

  val tagsEditModule = wire[TagsEditModule]
  import tagsEditModule._

  lazy val lkAdvGeoFormCircuit = wire[LkAdvGeoFormCircuit]

  lazy val advGeoFormR = wire[AdvGeoFormR]

  lazy val advGeoPopupsR = wire[AdvGeoPopupsR]

  lazy val advGeoNodeInfoPopR = wire[AdvGeoNodeInfoPopR]

  lazy val rcvrPopupR = wire[RcvrPopupR]

  lazy val onMainScreenR = wire[OnMainScreenR]

  lazy val docR = wire[DocR]

}
