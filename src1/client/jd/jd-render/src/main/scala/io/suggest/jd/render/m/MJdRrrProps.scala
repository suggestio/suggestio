package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.grid.build.MGridBuildResult
import io.suggest.jd.MJdTagId
import io.suggest.jd.tags.{JdTag, MJdTagNames}
import io.suggest.n2.edge.{EdgeUid_t, MEdgeDataJs}
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.spa.{FastEqUtil, OptFastEq}
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
    private val qdEdgesFeq = FastEqUtil.KvCollFastEq[EdgeUid_t, MEdgeDataJs, Iterable]( FastEqUtil.AnyValueEq, FastEqUtil.AnyRefFastEq )

    override def eqv(a: MJdRrrProps, b: MJdRrrProps): Boolean = {
      (a ===* b) || {
        (a.subTree        ===* b.subTree) &&
        (a.tagId          ==* b.tagId) &&
        // jdArgs НЕ сравниваем (кроме conf), вместо этого сравниваем lazy-val'ы с эджами.
        (a.jdArgs.conf ===* b.jdArgs.conf) &&
        OptFastEq.Plain.eqv(a.current_p1BgImgEdgeOpt, b.current_p1BgImgEdgeOpt) &&
        // Связанные эджи сравниваем только по keys и values через eq:
        qdEdgesFeq.eqv( a.current_qdEdges, b.current_qdEdges ) &&
        //MJdArgs.MJdArgsFastEq.eqv(a.jdArgs, b.jdArgs) &&
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

  /** Кэширование эджей, только связанных с данным qd-тегом для текущего subTree.rootLabel (обычно - нет) и под-тегов (это qd-ops),
    * чтобы FastEq мог сравнивать только связанные эджи, текущего jd-тега, подавляя в редакторе пере-рендеры всего и вся. */
  lazy val current_qdEdges: Map[EdgeUid_t, MEdgeDataJs] = {
    if (subTree.rootLabel.name ==* MJdTagNames.QD_CONTENT) {
      (for {
        qdJdTagTree <- subTree.subForest.iterator
        qdJdTag = qdJdTagTree.rootLabel
        qdProps <- qdJdTag.qdProps
        qdEi <- qdProps.edgeInfo
        qdEdge <- jdArgs.data.edges.get( qdEi.edgeUid )
      } yield {
        qdEi.edgeUid -> qdEdge
      })
        .toMap
    } else {
      Map.empty
    }
  }

  /** Кэш для bgImg-эджа. */
  lazy val current_p1BgImgEdgeOpt: Option[MEdgeDataJs] = {
    for {
      ei <- subTree.rootLabel.props1.bgImg
      bgEdge <- jdArgs.data.edges.get( ei.edgeUid )
    } yield {
      bgEdge
    }
  }

}
