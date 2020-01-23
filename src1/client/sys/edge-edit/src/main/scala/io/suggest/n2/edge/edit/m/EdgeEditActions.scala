package io.suggest.n2.edge.edit.m

import io.suggest.crypto.hash.MHash
import io.suggest.n2.edge.MPredicate
import io.suggest.n2.media.MFileMetaHashFlag
import io.suggest.n2.media.storage.MStorage
import io.suggest.spa.DAction
import org.scalajs.dom.File

import scala.util.Try

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

case class DeleteResp( startTimeMs: Long, tryResp: Try[None.type] ) extends IEdgeAction

/** Сокрытие диалога удаления эджа. */
case object DeleteCancel extends IEdgeAction

/** Сохранение эджа. */
case object Save extends IEdgeAction

case class SaveResp( startTimeMs: Long, tryResp: Try[None.type] ) extends IEdgeAction


/** Выставлен файл. */
case class FileSet( file: File ) extends IEdgeAction

case class FileMimeSet( fileMime: Option[String] ) extends IEdgeAction
case class FileSizeSet( sizeB: Option[Long] ) extends IEdgeAction
case class FileIsOriginalSet( isOriginal: Boolean ) extends IEdgeAction
case class FileHashEdit( mhash: MHash, hash: String ) extends IEdgeAction
case class FileHashFlagSet( mhash: MHash, flag: MFileMetaHashFlag, checked: Boolean ) extends IEdgeAction
case class FileStorageTypeSet( storageType: MStorage ) extends IEdgeAction
case class FileStorageMetaDataSet( storageData: String ) extends IEdgeAction
