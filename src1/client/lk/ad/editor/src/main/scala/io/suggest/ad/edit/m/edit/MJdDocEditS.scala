package io.suggest.ad.edit.m.edit

import diode.FastEq
import io.suggest.grid.build.MGridBuildResult
import io.suggest.jd.render.m.{MJdArgs, MJdRrrProps}
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.PLens
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

  /** Тут линза с костылём, чтобы НЕ обновлять инстанс пересчитанной плитки. */
  def gridBuildIfChanged = {
    val _gridBuild_LENS = gridBuild
    PLens[MJdDocEditS, MJdDocEditS, MGridBuildResult, MGridBuildResult](
      _gridBuild_LENS.get
    ) { gridBuild2 => v0 =>
      val gb0 = _gridBuild_LENS get v0
      if (gb0 !=* gridBuild2)
        _gridBuild_LENS.set(gridBuild2)(v0)
      else
        v0
    }
  }

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
      // someThis - костылёк, существенно ускоряющий рендер в редакторе без использования OptFastEq внутри MJdRrrProps.FastEq.
      gridBuildRes = gridBuild.someThis,
    )
  }

}
