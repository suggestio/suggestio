package io.suggest.sc.m.grid

import diode.Effect
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.05.18 15:55
  * Description: Состояние плитки, включающее в себя неотображаемые части плитки.
  */
object MGridS {

  @inline implicit def univEq: UnivEq[MGridS] = UnivEq.force

  def core = GenLens[MGridS](_.core)
  def hasMoreAds = GenLens[MGridS](_.hasMoreAds)
  def afterUpdate = GenLens[MGridS](_.afterUpdate)
  def gNotify = GenLens[MGridS](_.gNotify)

}


/** Общее состояние плитки: данные вне рендера.
  *
  * @param core Отображаемые данные плитки для рендера.
  * @param afterUpdate После ближайшего обновления карточек плитки, исполнить указанные экшены, обнулив список.
  * @param gNotify Состояние уведомлений по карточкам плитки.
  */
case class MGridS(
                   core             : MGridCoreS,
                   hasMoreAds       : Boolean               = true,
                   afterUpdate      : List[Effect]          = List.empty,
                   gNotify          : MGridNotifyS          = MGridNotifyS.empty,
                 ) {

}
