package io.suggest.sys.mdr.m

import diode.FastEq
import diode.data.Pot
import io.suggest.jd.render.v.JdCss
import io.suggest.sys.mdr.{MMdrActionInfo, MMdrNextResp}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.10.18 18:01
  * Description:
  */
object MMdrNodeS {

  implicit object MMdrNodeSFastEq extends FastEq[MMdrNodeS] {
    override def eqv(a: MMdrNodeS, b: MMdrNodeS): Boolean = {
      (a.jdCss        ===* b.jdCss) &&
      (a.info         ===* b.info) &&
      (a.mdrPots      ===* b.mdrPots) &&
      (a.fixNodePots  ===* b.fixNodePots) &&
      (a.nodeOffset    ==* b.nodeOffset)
    }
  }

  @inline implicit def univEq: UnivEq[MMdrNodeS] = UnivEq.derive

}

/** Состояние модерации текущего узла.
  *
  * @param jdCss Стили рендера рекламной карточки.
  * @param info Ответ сервера с данными для модерации.
  * @param dialogs Состояния диалогов.
  * @param mdrPots Состояния элементов модерации.
  * @param nodeOffset Сдвиг среди модерируемых узлов.
  *                   Позволяет пропустить узел, или вернуться назад к пропущенному узлу.
  *                   В норме - ноль.
  *                   Списки ошибкок узлов с сервера должны инкрементить это значение.
  * @param fixNodePots Запросы ремонта.
  */
case class MMdrNodeS(
                      jdCss           : JdCss,
                      info            : Pot[MMdrNextResp]                     = Pot.empty,
                      mdrPots         : Map[MMdrActionInfo, Pot[None.type]]   = Map.empty,
                      fixNodePots     : Map[String, Pot[None.type]]           = Map.empty,
                      nodeOffset      : Int                                   = 0,
                    ) {

  def withJdCss( jdCss: JdCss ) = copy(jdCss = jdCss)
  def withInfo( info: Pot[MMdrNextResp] ) = copy(info = info)
  def withMdrPots( mdrPots: Map[MMdrActionInfo, Pot[None.type]] ) = copy(mdrPots = mdrPots)
  def withNodeOffset(nodeOffset: Int) = copy(nodeOffset = nodeOffset)
  def withFixNodePots( fixNodePots: Map[String, Pot[None.type]] ) = copy(fixNodePots = fixNodePots)

}
