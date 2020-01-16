package io.suggest.n2.edge.edit.m

import io.suggest.n2.edge.MPredicate
import io.suggest.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 12:26
  * Description: Экшены для формы редактирования эджа.
  */
sealed trait IEdgeEditAction extends DAction

case class PredicateChanged( pred2: MPredicate ) extends IEdgeEditAction


case class NodeIdChange(i: Int, nodeId: String) extends IEdgeEditAction

case object NodeIdAdd extends IEdgeEditAction


case class FlagSet( flag2: Option[Boolean] ) extends IEdgeEditAction
