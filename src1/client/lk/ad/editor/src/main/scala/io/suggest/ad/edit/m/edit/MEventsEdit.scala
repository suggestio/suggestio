package io.suggest.ad.edit.m.edit

import diode.data.Pot
import io.suggest.dev.MSzMults
import io.suggest.jd.{MJdConf, MJdEdgeId}
import io.suggest.jd.render.m.{MJdDataJs, MJdRuntime}
import io.suggest.jd.tags.event.{MJdtAction, MJdtEventInfo}
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.04.2021 9:08
  * Description: Состояние данных, относящихся к редактору событий.
  */
object MEventsEdit {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MEventsEdit] = UnivEq.derive

  def adsAvail = GenLens[MEventsEdit](_.adsAvail)
  def hasMoreAds = GenLens[MEventsEdit](_.hasMoreAds)
  def jdRuntime = GenLens[MEventsEdit](_.jdRuntime)

  lazy val ADS_JD_CONF = MJdConf(
    isEdit = false,
    szMult = MSzMults.`0.25`,
    gridColumnsCount = 2,
  )


  implicit final class EvEditExt( private val evEdit: MEventsEdit ) extends AnyVal {

    /** @return true, если модель не содержит полезных данных по рекламным карточкам. */
    def isAdsNeedsInit: Boolean = {
      evEdit.adsAvail ===* Pot.empty
    }

  }

}


/** Контейнер данных состояния редактора событий.
  *
  * @param adsAvail Список карточек с сервера.
  *                 Отображается в выпадающем селекте при выборе связанной карточки.
  * @param hasMoreAds Есть ещё карточки для запрашивания с сервера?
  * @param jdRuntime Отрендеренный runtime по карточкам adsAvail.
  */
final case class MEventsEdit(
                              adsAvail                : Pot[Seq[MJdDataJs]]                   = Pot.empty,
                              hasMoreAds              : Boolean                               = true,
                              jdRuntime               : Option[MJdRuntime]                    = None,
                            )

/** Указатель на элемент среди event'ов. */
final case class MEventEditPtr(
                                eventInfo             : MJdtEventInfo,
                                action                : MJdtAction,
                                jdEdgeId              : Option[MJdEdgeId]                     = None,
                              )
