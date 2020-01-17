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

/** Изменение edge.predicate. */
case class PredicateChanged( pred2: MPredicate ) extends IEdgeEditAction

/** Редактирование edge.nodeIds[i]. */
case class NodeIdChange(i: Int, nodeId: String) extends IEdgeEditAction

/** Создание edge.nodeIds[n+1]. */
case object NodeIdAdd extends IEdgeEditAction


/** Выставление edge.info.flag. */
case class FlagSet( flag2: Option[Boolean] ) extends IEdgeEditAction

/** Выставление edge.order. */
case class OrderSet( order: Option[Int] ) extends IEdgeEditAction

/** Редактирование неиндексируемого текста. */
case class TextNiSet(commentNi: Option[String] ) extends IEdgeEditAction



sealed trait IEdgeAction extends DAction

/** @param isDelete false - запрос диалога удаления.
  *                 true - подтверждение удаления.
  */
case class DeleteEdge(isDelete: Boolean ) extends IEdgeAction

/** Сокрытие диалога удаления эджа. */
case object DeleteCancel extends IEdgeAction

/** Сохранение эджа. */
case object Save extends IEdgeAction
