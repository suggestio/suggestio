package io.suggest.n2.edge.edit.m

import io.suggest.crypto.hash.MHash
import io.suggest.n2.edge.MEdge
import io.suggest.n2.media.MFileMetaHashFlag
import io.suggest.spa.DAction
import japgolly.univeq._

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 12:26
  * Description: Экшены для формы редактирования эджа.
  */
sealed trait IEdgeEditAction extends DAction

/** Редактирование edge.nodeIds[i]. */
case class NodeIdChange(i: Int, nodeId: String) extends IEdgeEditAction

/** Создание edge.nodeIds[n+1]. */
case object NodeIdAdd extends IEdgeEditAction


/** Выставление edge.info.flag. */
case class FlagSet( flag2: Option[Boolean] ) extends IEdgeEditAction

case class UpdateWithLens[T: UnivEq](lens: monocle.Lens[MEdgeEditRoot, T], newValue: T ) extends IEdgeEditAction {
  def isNewValueEqualsTo(oldValue: T): Boolean =
    newValue ==* oldValue
}
object UpdateWithLens {
  def edge[T: UnivEq](lens: monocle.Lens[MEdge, T], newValue: T ) =
    apply( MEdgeEditRoot.edge andThen lens, newValue )
}


/** For code-deduplication purposes, this action contains Edge model update logic with new value using Traverse. */
case class EdgeUpdateWithTraverse[T: UnivEq](traverse: monocle.Traversal[MEdge, T], newValue: T ) extends IEdgeEditAction {
  def isNewValueEqualsTo(oldValue: T): Boolean =
    newValue ==* oldValue
}


sealed trait IEdgeAction extends DAction

/** @param isDelete false - запрос диалога удаления.
  *                 true - подтверждение удаления.
  */
case class DeleteEdge(isDelete: Boolean ) extends IEdgeAction

case class DeleteResp( startTimeMs: Long, tryResp: Try[None.type] ) extends IEdgeAction

/** Сокрытие диалога удаления эджа. */
case object DeleteCancel extends IEdgeAction

/** Сохранение эджа. */
case object Save extends IEdgeAction

case class SaveResp( startTimeMs: Long, tryResp: Try[None.type] ) extends IEdgeAction


case class FileHashEdit( mhash: MHash, hash: String ) extends IEdgeAction
case class FileHashFlagSet( mhash: MHash, flag: MFileMetaHashFlag, checked: Boolean ) extends IEdgeAction


sealed trait IFileExistAction extends DAction
case object FileExistReplaceNodeIds extends IFileExistAction
case object FileExistAppendToNodeIds extends IFileExistAction
case object FileExistCancel extends IFileExistAction
