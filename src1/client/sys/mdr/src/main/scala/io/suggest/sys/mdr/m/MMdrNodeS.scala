package io.suggest.sys.mdr.m

import diode.FastEq
import diode.data.Pot
import io.suggest.jd.render.m.MJdRuntime
import io.suggest.sys.mdr.{MMdrActionInfo, MMdrNextResp}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.10.18 18:01
  * Description:
  */
object MMdrNodeS {

  implicit object MMdrNodeSFastEq extends FastEq[MMdrNodeS] {
    override def eqv(a: MMdrNodeS, b: MMdrNodeS): Boolean = {
      (a.jdRuntime    ===* b.jdRuntime) &&
      (a.info         ===* b.info) &&
      (a.mdrPots      ===* b.mdrPots) &&
      (a.fixNodePots  ===* b.fixNodePots) &&
      (a.nodeOffset    ==* b.nodeOffset)
    }
  }

  @inline implicit def univEq: UnivEq[MMdrNodeS] = UnivEq.derive


  def info = GenLens[MMdrNodeS](_.info)
  def mdrPots = GenLens[MMdrNodeS](_.mdrPots)
  def fixNodePots = GenLens[MMdrNodeS](_.fixNodePots)

}

/** Состояние модерации текущего узла.
  *
  * @param jdRuntime Стили рендера рекламной карточки.
  * @param info Ответ сервера с данными для модерации.
  * @param mdrPots Состояния элементов модерации.
  * @param nodeOffset Сдвиг среди модерируемых узлов.
  *                   Позволяет пропустить узел, или вернуться назад к пропущенному узлу.
  *                   В норме - ноль.
  *                   Списки ошибкок узлов с сервера должны инкрементить это значение.
  * @param fixNodePots Запросы ремонта.
  */
case class MMdrNodeS(
                      jdRuntime       : MJdRuntime,
                      info            : Pot[MMdrNextRespJs]                   = Pot.empty,
                      mdrPots         : Map[MMdrActionInfo, Pot[None.type]]   = Map.empty,
                      fixNodePots     : Map[String, Pot[None.type]]           = Map.empty,
                      nodeOffset      : Int                                   = 0,
                    ) {

  def withMdrPots( mdrPots: Map[MMdrActionInfo, Pot[None.type]] ) = copy(mdrPots = mdrPots)
  def withFixNodePots( fixNodePots: Map[String, Pot[None.type]] ) = copy(fixNodePots = fixNodePots)

}
