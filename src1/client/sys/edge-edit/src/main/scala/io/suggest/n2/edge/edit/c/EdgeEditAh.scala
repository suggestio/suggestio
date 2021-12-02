package io.suggest.n2.edge.edit.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.n2.edge.{MEdge, MEdgeInfo}
import io.suggest.n2.edge.edit.m.{DeleteCancel, DeleteEdge, DeleteResp, UpdateWithLens, EdgeUpdateWithTraverse, FileHashEdit, FileHashFlagSet, FlagSet, MDeleteDiaS, MEdgeEditRoot, MEdgeEditS, NodeIdAdd, NodeIdChange, Save, SaveResp}
import io.suggest.n2.edge.edit.u.IEdgeEditApi
import io.suggest.n2.media.storage.{MStorageInfo, MStorageInfoData}
import io.suggest.n2.media.{MEdgeMedia, MFileMeta, MFileMetaHash}
import io.suggest.routes.routes
import japgolly.univeq._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing
import monocle.Traversal
import scalaz.std.option._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 9:58
  * Description: Контроллер заливки файла в форме.
  */
class EdgeEditAh[M](
                     edgeEditApi    : IEdgeEditApi,
                     modelRW        : ModelRW[M, MEdgeEditRoot],
                   )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Редактирования id узла из списка узлов.
    case m: NodeIdChange =>
      val v0 = value

      val lens = EdgeEditAh.root_edit_nodeIds_LENS
      val nodeIds0 = lens.get( v0 )
      val nodeId0 = nodeIds0( m.i )
      val nodeId2 = m.nodeId.trim

      if (nodeId0 ==* nodeId2) {
        // Не изменилось ничего.
        noChange

      } else {
        val nodeIds2 = nodeIds0.updated(m.i, nodeId2)

        val v2 = (lens replace nodeIds2)( v0 )
        updated(v2)
      }


    // Нажатие кнопки добавления узла.
    case NodeIdAdd =>
      val v0 = value

      val lens = EdgeEditAh.root_edit_nodeIds_LENS
      val nodeIds0 = lens.get( v0 )

      val emptyStr = ""
      if (nodeIds0.lastOption.map(_.trim) contains emptyStr) {
        noChange

      } else {
        val nodeIds2 = nodeIds0 :+ emptyStr
        val v2 = (lens replace nodeIds2)(v0)
        updated(v2)
      }


    // Изменение legacy-флага:
    case m: FlagSet =>
      val v0 = value

      val lens = MEdgeEditRoot.edge
        .andThen( MEdge.info )
        .andThen( MEdgeInfo.flag )

      val flag0 = lens.get(v0)

      val flag2 =
        if ((flag0 contains false) && (m.flag2 contains true)) None
        else m.flag2

      if ( flag0 ==* flag2 ) {
        noChange
      } else {
        val v2 = (lens replace flag2)(v0)
        updated(v2)
      }


    case m: UpdateWithLens[_] =>
      val v0 = value

      if (m.isNewValueEqualsTo( m.lens.get(v0) ) ) {
        noChange
      } else {
        val v2 = m.lens.replace( m.newValue )(v0)
        updated( v2 )
      }


    case m: EdgeUpdateWithTraverse[_] =>
      val v0 = value
      val trav = MEdgeEditRoot.edge
        .andThen( m.traverse )

      if (trav.exist( m.isNewValueEqualsTo )(v0)) {
        noChange
      } else {
        val v2 = trav.replace( m.newValue )(v0)
        updated( v2 )
      }


    // Запуск сохранения эджа.
    case Save =>
      val v0 = value

      val lens = EdgeEditAh.root_edit_saveReq

      val req0 = lens.get( v0 )
      if (req0.isPending) {
        noChange

      } else {
        // Запуск запроса сохранения на сервер.
        val startTimeMs = System.currentTimeMillis()
        val fx = Effect {
          edgeEditApi
            .saveEdge( v0.conf, edge = v0.toEdge )
            .transform { tryResp =>
              Success( SaveResp(startTimeMs, tryResp) )
            }
        }
        val v2 = lens.replace( req0.pending(startTimeMs) )(v0)
        updated(v2, fx)
      }

    case m: SaveResp =>
      val v0 = value

      val lens = EdgeEditAh.root_edit_saveReq
      val req0 = lens.get( v0 )
      if ( !req0.isPendingWithStartTime(m.startTimeMs) ) {
        noChange

      } else {
        val req2 = req0.withTry( m.tryResp )
        val v2 = lens.replace( req2 )(v0)

        if (m.tryResp.isSuccess) {
          // Всё сохранено, отредиректить юзера
          val fx = EdgeEditAh.rdrToSysNodeFx(v2)
          updated( v2, fx )
        } else {
          // Ошибка. Не редиректить юзера никуда.
          updated(v2)
        }
      }


    case m: DeleteEdge =>
      val v0 = value
      if (v0.edit.saveReq.isPending || v0.conf.edgeId.isEmpty) {
        noChange

      } else if (m.isDelete) {
        // Отправка запроса удаления на сервер
        val startTimeMs = System.currentTimeMillis()
        val fx = Effect {
          edgeEditApi
            .deleteEdge( v0.conf )
            .transform { tryRes =>
              Success( DeleteResp(startTimeMs, tryRes) )
            }
        }
        val lens = EdgeEditAh.root_edit_deleteDia_req_LENS
        val v2 = lens.modify( _.pending(startTimeMs) )( v0 )

        updated(v2, fx)

      } else {
        // Отрендерить диалог удаления.
        val lens = EdgeEditAh.root_edit_deleteDia_LENS
        if ( lens.get(v0) ) {
          noChange
        } else {
          val v2 = (lens replace true)(v0)
          updated( v2 )
        }
      }

    // Ответ сервера
    case m: DeleteResp =>
      val v0 = value
      val lens = EdgeEditAh.root_edit_deleteDia_req_LENS
      val req0 = lens.get( v0 )
      if (!req0.isPendingWithStartTime( m.startTimeMs )) {
        noChange

      } else {
        val req2 = req0.withTry( m.tryResp )

        if (m.tryResp.isSuccess) {
          // Всё ок, то скрыть диалог и отредиректить.
          val v2 = (
            MEdgeEditRoot.edit
              .andThen( MEdgeEditS.deleteDia )
              .modify(
                MDeleteDiaS.deleteReq.replace( req2 ) andThen
                MDeleteDiaS.opened.replace( false )
              )
          )(v0)
          val fx = EdgeEditAh.rdrToSysNodeFx(v2)

          updated(v2, fx)

        } else {
          // Ошибка. Не скрывать диалог, отрендерить ошибку.
          val v2 = (lens replace req2)(v0)
          updated(v2)
        }
      }


    // Отмена удаления эджа.
    case DeleteCancel =>
      val v0 = value
      val lens = EdgeEditAh.root_edit_deleteDia_LENS

      if ( !lens.get(v0) ) {
        // Диалог удаления уже скрыт.
        noChange

      } else {
        val v2 = (lens replace false)(v0)
        updated(v2)
      }


    // Редактор хэша.
    case m: FileHashEdit =>
      val v0 = value
      val lens = EdgeEditAh
        .root_edge_media_file_LENS
        .andThen( MFileMeta.hashesHex )

      val hashes0 = lens
        .getAll(v0)
        .flatten
      val hashOldOpt = hashes0
        .find(_.hType ==* m.mhash)

      if (
        hashOldOpt.exists { h =>
          m.hash ==* h.hexValue
        }
      ) {
        noChange
      } else {
        // Удалить из списка хэши текущего типа
        val hashes1 = hashes0
          .filterNot( _.hType ==* m.mhash )
        // Если задан обновлённое значение хэша, добавить его в список.
        val hashes2 = if (m.hash.isEmpty) {
          hashes1
        } else {
          val fmHash2 = hashOldOpt.fold[MFileMetaHash] {
            MFileMetaHash(m.mhash, m.hash, Set.empty)
          }( MFileMetaHash.hexValue.replace(m.hash) )
          fmHash2 :: hashes1
        }
        val v2 = lens.replace( hashes2 )(v0)
        updated(v2)
      }


    // Редактирование флагов хэша.
    case m: FileHashFlagSet =>
      val v0 = value
      val lens = EdgeEditAh
        .root_edge_media_file_LENS
        .andThen( MFileMeta.hashesHex )

      val hashes0 = lens
        .getAll(v0)
        .flatten

      hashes0
        .find(_.hType ==* m.mhash)
        .filterNot { h =>
          // Проверить, изменится ли хоть что-нибудь от смены флага? По идее, тут всегда true.
          val hasFlag = h.flags contains m.flag
          if (m.checked) hasFlag
          else !hasFlag
        }
        .fold(noChange) { h0 =>
          // Удалить из списка хэши текущего типа
          val hashes1 = hashes0
            .filterNot( _.hType ==* m.mhash )
          // Обновить старый хэш.
          val h2 = MFileMetaHash.flags.replace {
            if (m.checked) h0.flags + m.flag
            else h0.flags - m.flag
          }(h0)

          val v2 = lens.replace( h2 :: hashes1 )(v0)
          updated(v2)
        }

  }

}


object EdgeEditAh {

  private def root_edge_media_LENS = {
    MEdgeEditRoot.edge
      .andThen( MEdge.media )
      .andThen( Traversal.fromTraverse[Option, MEdgeMedia] )
  }

  private def root_edge_media_file_LENS = {
    root_edge_media_LENS
      .andThen( MEdgeMedia.file )
  }

  private def root_edit_nodeIds_LENS = {
    MEdgeEditRoot.edit
      .andThen( MEdgeEditS.nodeIds )
  }

  private def root_edit_deleteDia_LENS =
    MEdgeEditRoot.edit
      .andThen( MEdgeEditS.deleteDia )
      .andThen( MDeleteDiaS.opened )

  private def root_edit_deleteDia_req_LENS =
    MEdgeEditRoot.edit
      .andThen( MEdgeEditS.deleteDia )
      .andThen( MDeleteDiaS.deleteReq )

  private def root_edit_saveReq =
    MEdgeEditRoot.edit
      .andThen( MEdgeEditS.saveReq )


  private def rdrToSysNodeFx(v0: MEdgeEditRoot): Effect = {
    Effect.action {
      DomQuick.goToLocation(
        routes.controllers.SysMarket
          .showAdnNode( nodeId = v0.conf.nodeId )
          .url
      )
      DoNothing
    }
  }

}
