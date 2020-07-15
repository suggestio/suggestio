package io.suggest.sc.m.in

import diode.FastEq
import io.suggest.i18n.MCommonReactCtx
import io.suggest.sc.m.inx.save.{MIndexInfo, MIndexesRecentOuter}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.sc.sc3.Sc3Pages.MainScreen
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
      (a.commonReactCtx ===* b.commonReactCtx) &&
      (a.indexesRecents ===* b.indexesRecents)
    }
  }

  @inline implicit def univEq: UnivEq[MInternalInfo] = UnivEq.derive

  def geoLockTimer  = GenLens[MInternalInfo]( _.geoLockTimer )
  def currRoute     = GenLens[MInternalInfo]( _.currRoute )
  def messages      = GenLens[MInternalInfo]( _.commonReactCtx )
  def inxRecents    = GenLens[MInternalInfo]( _.indexesRecents )

}


/** Контейнер различных внутренних данных.
  *
  * @param geoLockTimer Таймер ожидания геолокации.
  * @param currRoute Текущая роута.
  * @param commonReactCtx Инстанс с сообщениями.
  * @param indexesRecents Предпоследнее состояние списка недавних посещённых узлов.
  */
final case class MInternalInfo(
                                geoLockTimer      : Option[Int]             = None,
                                currRoute         : Option[MainScreen]      = None,
                                commonReactCtx    : MCommonReactCtx         = MCommonReactCtx.default,
                                indexesRecents    : MIndexesRecentOuter,
                              ) {

  lazy val inxRecentsClean: List[MIndexInfo] = {
    (for {
      indexesRecent <- indexesRecents.saved.toOption
      if indexesRecent.recents.nonEmpty

      // Убрать первый элемент, если это текущий узел
      forDisplay = indexesRecent.recents
        .headOption
        .fold( indexesRecent.recents ) { firstInx =>
          if (currRoute.exists( _ isSamePlaceAs firstInx.state )) {
            indexesRecent.recents.tail
          } else {
            indexesRecent.recents
          }
        }

      if forDisplay.nonEmpty
    } yield {
      forDisplay
    })
      .getOrElse(Nil)
  }

}
