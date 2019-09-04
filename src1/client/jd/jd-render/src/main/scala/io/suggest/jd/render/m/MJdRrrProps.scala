package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.jd.MJdTagId
import io.suggest.jd.tags.JdTag
import io.suggest.ueq.UnivEqUtil._
import io.suggest.scalaz.ZTreeUtil._
import japgolly.univeq.UnivEq
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.08.2019 10:33
  * Description: Пропертисы для рендеринга внутри JdR.
  */
object MJdRrrProps {

  implicit object MJdtRrrPropsFastEq extends FastEq[MJdRrrProps] {
    override def eqv(a: MJdRrrProps, b: MJdRrrProps): Boolean = {
      (a.subTree        ===* b.subTree) &&
      (a.tagId          ===* b.tagId) &&
      (a.jdArgs         ===* b.jdArgs) &&
      (a.parent         ===* b.parent)
    }
  }

  @inline implicit def univEq: UnivEq[MJdRrrProps] = UnivEq.derive

}


case class MJdRrrProps(
                        subTree         : Tree[JdTag],
                        tagId           : MJdTagId,
                        jdArgs          : MJdArgs,
                        parent          : Option[JdTag]    = None,
                      ) {

  lazy val isCurrentSelected: Boolean =
    jdArgs.selJdt.treeLocOpt containsLabel subTree.rootLabel

}

