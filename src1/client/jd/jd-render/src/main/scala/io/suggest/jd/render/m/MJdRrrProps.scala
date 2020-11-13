package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.grid.build.MGridBuildResult
import io.suggest.jd.MJdTagId
import io.suggest.jd.tags.JdTag
import io.suggest.ueq.UnivEqUtil._
import io.suggest.scalaz.ZTreeUtil._
import japgolly.univeq._
import monocle.macros.GenLens
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.2019 10:33
  * Description: Пропертисы для рендеринга внутри JdR.
  */
object MJdRrrProps {

  implicit object MJdRrrPropsFastEq extends FastEq[MJdRrrProps] {
    override def eqv(a: MJdRrrProps, b: MJdRrrProps): Boolean = {
      (a ===* b) || {
        (a.subTree        ===* b.subTree) &&
        (a.tagId          ==* b.tagId) &&
        MJdArgs.MJdArgsFastEq.eqv(a.jdArgs, b.jdArgs) &&
        //(a.parents        ===* b.parents) &&
        (a.gridBuildRes   ===* b.gridBuildRes) &&
        ((a.renderArgs ===* b.renderArgs) || MJdRenderArgs.MJdRenderArgsFastEq.eqv(a.renderArgs, b.renderArgs))
      }
    }
  }

  @inline implicit def univEq: UnivEq[MJdRrrProps] = UnivEq.derive

  def gridBuildResult = GenLens[MJdRrrProps](_.gridBuildRes)

}


case class MJdRrrProps(
                        subTree         : Tree[JdTag],
                        tagId           : MJdTagId,
                        jdArgs          : MJdArgs,
                        parents         : List[JdTag]               = Nil,
                        gridBuildRes    : Option[MGridBuildResult]  = None,
                        renderArgs      : MJdRenderArgs             = MJdRenderArgs.empty,
                      ) {

  lazy val isCurrentSelected: Boolean =
    jdArgs.selJdt.treeLocOpt containsLabel subTree.rootLabel

  /** Данные qd-blockless. */
  lazy val qdBlOpt = jdArgs.jdRuntime.data.qdBlockLess.get( tagId )

}

