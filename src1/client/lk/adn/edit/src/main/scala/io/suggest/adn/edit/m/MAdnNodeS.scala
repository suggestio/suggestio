package io.suggest.adn.edit.m

import diode.FastEq
import io.suggest.lk.m.frk.{MFormResourceKey, MFrkTypes}
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.model.n2.node.meta.MMetaPub
import io.suggest.n2.edge.MEdgeDataJs
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 17:37
  * Description: Модель состояния редактирования данных узла.
  */
object MAdnNodeS {

  implicit object MAdnNodeSFastEq extends FastEq[MAdnNodeS] {
    override def eqv(a: MAdnNodeS, b: MAdnNodeS): Boolean = {
      (a.meta ===* b.meta) &&
      (a.edges ===* b.edges) &&
      (a.resView ===* b.resView) &&
      (a.errors ===* b.errors)
    }
  }

  @inline implicit def univEq: UnivEq[MAdnNodeS] = UnivEq.derive

  val meta = GenLens[MAdnNodeS](_.meta)
  val edges = GenLens[MAdnNodeS](_.edges)
  val resView = GenLens[MAdnNodeS](_.resView)
  val errors = GenLens[MAdnNodeS](_.errors)

}


/** Модель состояния редактирования узла.
  *
  * @param meta Текстовые метаданные узла.
  */
case class MAdnNodeS(
                      meta          : MMetaPub,
                      edges         : Map[EdgeUid_t, MEdgeDataJs],
                      resView       : MAdnResView,
                      errors        : MAdnEditErrors                = MAdnEditErrors.empty,
                    ) {

  def withMeta(meta: MMetaPub)                                    = copy(meta = meta)
  def withEdges(edges: Map[EdgeUid_t, MEdgeDataJs])               = copy(edges = edges)
  def withResView(resView: MAdnResView)                           = copy(resView = resView)
  def withErrors(errors: MAdnEditErrors)                          = copy(errors = errors)

  def toForm: MAdnEditForm = {
    MAdnEditForm(
      meta = meta,
      edges = edges.values
        .iterator
        .map(_.jdEdge.withUrl())
        .toSeq,
      resView = resView
    )
  }

  lazy val logoFrk = MFormResourceKey(
    edgeUid  = resView.logo.map(_.edgeUid),
    frkType  = MFrkTypes.somes.LogoSome,
  )

  lazy val wcFgFrk = {
    MFormResourceKey(
      edgeUid  = resView.wcFg.map(_.edgeUid),
      frkType  = MFrkTypes.somes.WcFgSome,
    )
  }

}
