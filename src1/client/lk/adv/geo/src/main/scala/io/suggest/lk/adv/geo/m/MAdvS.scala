package io.suggest.lk.adv.geo.m

import io.suggest.adv.free.MAdv4Free
import io.suggest.dt.MAdvPeriod
import io.suggest.lk.tags.edit.m.MTagsEditState
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.2020 14:10
  * Description:
  */
object MAdvS {

  val rcvr = GenLens[MAdvS]( _.rcvr )
  val tags = GenLens[MAdvS]( _.tags )
  val datePeriod = GenLens[MAdvS]( _.datePeriod )
  val bill = GenLens[MAdvS]( _.bill )
  val free = GenLens[MAdvS]( _.free )

  @inline implicit def univEq: UnivEq[MAdvS] = UnivEq.derive

}


/** Контейнер состояний, непосредственно связанных с размещением.
  *
  * @param tags Контейнер данных по тегам.
  * @param free Состояние бесплатного размещения, если доступно.
  * @param rcvr Состояние прямого размещения в ресиверах.
  * @param datePeriod Состояние периода на календаре.
  * @param bill Состояние биллинга.
  */
final case class MAdvS(
                        free          : Option[MAdv4Free],
                        tags          : MTagsEditState,
                        rcvr          : MRcvr,
                        datePeriod    : MAdvPeriod,
                        bill          : MBillS,
                      )
