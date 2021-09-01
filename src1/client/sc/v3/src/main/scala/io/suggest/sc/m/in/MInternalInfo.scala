package io.suggest.sc.m.in

import diode.FastEq
import diode.data.Pot
import io.suggest.proto.http.model.MCsrfToken
import io.suggest.sc.index.MScIndexInfo
import io.suggest.sc.m.inx.save.MIndexesRecentOuter
import io.suggest.ueq.UnivEqUtil._
import io.suggest.spa.SioPages.Sc3
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.ueq.JsUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.19 11:21
  * Description: Контейнер данных для различных internal-полей.
  */
object MInternalInfo {

  implicit object MInternalInfoFastEq extends FastEq[MInternalInfo] {
    override def eqv(a: MInternalInfo, b: MInternalInfo): Boolean = {
      (a.geoLockTimer ===* b.geoLockTimer) &&
      (a.currRoute ===* b.currRoute) &&
      (a.reactCtx ===* b.reactCtx) &&
      (a.indexesRecents ===* b.indexesRecents)
    }
  }

  @inline implicit def univEq: UnivEq[MInternalInfo] = UnivEq.derive

  def geoLockTimer  = GenLens[MInternalInfo]( _.geoLockTimer )
  def currRoute     = GenLens[MInternalInfo]( _.currRoute )
  def reactCtx      = GenLens[MInternalInfo]( _.reactCtx )
  def inxRecents    = GenLens[MInternalInfo]( _.indexesRecents )
  def csrfToken     = GenLens[MInternalInfo]( _.csrfToken )

}


/** Контейнер различных внутренних данных.
  *
  * @param geoLockTimer Timer for waiting geolocation data.
  *                     Pot.empty - never started.
  *                     Ready+Pending - timer is active.
  *                     Unavailable - already started and finished.
  * @param currRoute Текущая роута.
  * @param reactCtx State of common react context and language switching.
  * @param indexesRecents Предпоследнее состояние списка недавних посещённых узлов.
  * @param csrfToken Токен CSRF от сервера.
  */
final case class MInternalInfo(
                                geoLockTimer      : Pot[Int]                = Pot.empty,
                                currRoute         : Option[Sc3]             = None,
                                reactCtx          : MScReactCtx             = MScReactCtx.default,
                                indexesRecents    : MIndexesRecentOuter,
                                csrfToken         : Pot[MCsrfToken]         = Pot.empty,
                              ) {

  lazy val inxRecentsClean: List[MScIndexInfo] = {
    (for {
      indexesRecent <- indexesRecents.saved.toOption
      if indexesRecent.indexes.nonEmpty

      // Убрать первый элемент, если это текущий узел
      forDisplay = indexesRecent.indexes
        .headOption
        .fold( indexesRecent.indexes ) { firstInx =>
          if (currRoute.exists( _ isSamePlaceAs firstInx.state )) {
            indexesRecent.indexes.tail
          } else {
            indexesRecent.indexes
          }
        }

      if forDisplay.nonEmpty
    } yield {
      forDisplay
    })
      .getOrElse(Nil)
  }

}
