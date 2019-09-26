package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.grid.build.MGridBuildResult
import io.suggest.jd.MJdTagId
import io.suggest.jd.tags.JdTag
import io.suggest.ueq.UnivEqUtil._
import io.suggest.scalaz.ZTreeUtil._
import japgolly.univeq.UnivEq
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
        (a.tagId          ===* b.tagId) &&
        (a.jdArgs         ===* b.jdArgs) &&
        (a.parent         ===* b.parent) &&
        (a.gridBuildRes   ===* b.gridBuildRes)
      }
    }
  }

  @inline implicit def univEq: UnivEq[MJdRrrProps] = UnivEq.derive

  val gridBuildResult = GenLens[MJdRrrProps](_.gridBuildRes)

}


case class MJdRrrProps(
                        subTree         : Tree[JdTag],
                        tagId           : MJdTagId,
                        jdArgs          : MJdArgs,
                        parent          : Option[JdTag]    = None,
                        gridBuildRes    : Option[MGridBuildResult]  = None,
                      ) {

  lazy val isCurrentSelected: Boolean =
    jdArgs.selJdt.treeLocOpt containsLabel subTree.rootLabel

}

