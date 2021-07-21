package io.suggest.sys.mdr.m

import diode.FastEq
import diode.data.Pot
import io.suggest.grid.build.MGridBuildResult
import io.suggest.jd.render.m.MJdArgs
import io.suggest.sys.mdr.MMdrActionInfo
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

  def empty = apply()

  implicit object MMdrNodeSFastEq extends FastEq[MMdrNodeS] {
    override def eqv(a: MMdrNodeS, b: MMdrNodeS): Boolean = {
      (a.jdArgsOpt    ===* b.jdArgsOpt) &&
      (a.info         ===* b.info) &&
      (a.mdrPots      ===* b.mdrPots) &&
      (a.fixNodePots  ===* b.fixNodePots) &&
      (a.nodeOffset    ==* b.nodeOffset)
    }
  }

  @inline implicit def univEq: UnivEq[MMdrNodeS] = UnivEq.derive


  val jdArgsOpt = GenLens[MMdrNodeS](_.jdArgsOpt)
  def gridBuild = GenLens[MMdrNodeS](_.gridBuild)
  def info = GenLens[MMdrNodeS](_.info)
  def mdrPots = GenLens[MMdrNodeS](_.mdrPots)
  def fixNodePots = GenLens[MMdrNodeS](_.fixNodePots)

}

/** Состояние модерации текущего узла.
  *
  * @param jdArgsOpt Rendering info for current ad node.
  * @param info Ответ сервера с данными для модерации.
  * @param mdrPots Состояния элементов модерации.
  * @param nodeOffset Сдвиг среди модерируемых узлов.
  *                   Позволяет пропустить узел, или вернуться назад к пропущенному узлу.
  *                   В норме - ноль.
  *                   Списки ошибкок узлов с сервера должны инкрементить это значение.
  * @param fixNodePots Запросы ремонта.
  */
case class MMdrNodeS(
                      jdArgsOpt       : Option[MJdArgs]                       = None,
                      gridBuild       : Option[MGridBuildResult]              = None,
                      info            : Pot[MMdrNextRespJs]                   = Pot.empty,
                      mdrPots         : Map[MMdrActionInfo, Pot[None.type]]   = Map.empty,
                      fixNodePots     : Map[String, Pot[None.type]]           = Map.empty,
                      nodeOffset      : Int                                   = 0,
                    )
