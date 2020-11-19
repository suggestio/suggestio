package io.suggest.sc.m.in

import diode.FastEq
import diode.data.Pot
import io.suggest.i18n.MCommonReactCtx
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
      (a.commonReactCtx ===* b.commonReactCtx) &&
      (a.indexesRecents ===* b.indexesRecents)
    }
  }

  @inline implicit def univEq: UnivEq[MInternalInfo] = UnivEq.derive

  def geoLockTimer  = GenLens[MInternalInfo]( _.geoLockTimer )
  val currRoute     = GenLens[MInternalInfo]( _.currRoute )
  def messages      = GenLens[MInternalInfo]( _.commonReactCtx )
  def inxRecents    = GenLens[MInternalInfo]( _.indexesRecents )
  val csrfToken     = GenLens[MInternalInfo]( _.csrfToken )

}


/** Контейнер различных внутренних данных.
  *
  * @param geoLockTimer Таймер ожидания геолокации.
  * @param currRoute Текущая роута.
  * @param commonReactCtx Инстанс с сообщениями.
  * @param indexesRecents Предпоследнее состояние списка недавних посещённых узлов.
  * @param csrfToken Токен CSRF от сервера.
  */
final case class MInternalInfo(
                                geoLockTimer      : Option[Int]             = None,
                                currRoute         : Option[Sc3]             = None,
                                commonReactCtx    : MCommonReactCtx         = MCommonReactCtx.default,
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
