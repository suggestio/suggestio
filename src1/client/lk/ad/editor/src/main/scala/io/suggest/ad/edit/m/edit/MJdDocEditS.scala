package io.suggest.ad.edit.m.edit

import diode.FastEq
import io.suggest.grid.build.MGridBuildResult
import io.suggest.jd.render.m.{MJdArgs, MJdRrrProps}
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.09.2019 11:56
  * Description: Состояние редактируемого jd-документа и производных данных.
  */
object MJdDocEditS {

  implicit object MJdDocEditSFastEq extends FastEq[MJdDocEditS] {
    override def eqv(a: MJdDocEditS, b: MJdDocEditS): Boolean = {
      (a.jdArgs       ===* b.jdArgs) &&
      (a.gridBuild    ===* b.gridBuild)
    }
  }

  @inline implicit def univEq: UnivEq[MJdDocEditS] = UnivEq.derive

  val jdArgs      = GenLens[MJdDocEditS](_.jdArgs)
  def gridBuild   = GenLens[MJdDocEditS](_.gridBuild)

}


/** Контейнер данных jd-документа.
  *
  * @param jdArgs Данные jd-документа.
  * @param gridBuild Плитка для jd-document-тега.
  */
case class MJdDocEditS(
                        jdArgs        : MJdArgs,
                        gridBuild     : MGridBuildResult,
                      ) {

  lazy val toRrrProps: MJdRrrProps = {
    MJdRrrProps(
      subTree = jdArgs.data.doc.template,
      tagId   = jdArgs.data.doc.tagId,
      jdArgs  = jdArgs,
      gridBuildRes = Some(gridBuild),
    )
  }

}
