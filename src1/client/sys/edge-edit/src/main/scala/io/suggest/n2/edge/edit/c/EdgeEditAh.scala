package io.suggest.n2.edge.edit.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.n2.edge.{MEdge, MEdgeInfo}
import io.suggest.n2.edge.edit.m.{DeleteCancel, DeleteEdge, DeleteResp, FlagSet, MDeleteDiaS, MEdgeEditRoot, MEdgeEditS, NodeIdAdd, NodeIdChange, OrderSet, PredicateChanged, Save, SaveResp, TextNiSet}
import io.suggest.n2.edge.edit.u.IEdgeEditApi
import io.suggest.routes.routes
import japgolly.univeq._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom.DomQuick
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 9:58
  * Description: Контроллер заливки файла в форме.
  */
class EdgeEditAh[M](
                     edgeEditApi: IEdgeEditApi,
                     modelRW: ModelRW[M, MEdgeEditRoot],
                   )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Смена предиката.
    case m: PredicateChanged =>
      val v0 = value

      val lens = MEdgeEditRoot.edge
        .composeLens( MEdge.predicate )

      if ( m.pred2 ==* lens.get(v0) ) {
        noChange

      } else {
        val v2 = (lens set m.pred2)(v0)
        updated(v2)
      }


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

        val v2 = (lens set nodeIds2)( v0 )
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
        val v2 = (lens set nodeIds2)(v0)
        updated(v2)
      }


    // Изменение legacy-флага:
    case m: FlagSet =>
      val v0 = value

      val lens = MEdgeEditRoot.edge
        .composeLens( MEdge.info )
        .composeLens( MEdgeInfo.flag )

      val flag0 = lens.get(v0)

      val flag2 =
        if ((flag0 contains false) && (m.flag2 contains true)) None
        else m.flag2

      if ( flag0 ==* flag2 ) {
        noChange
      } else {
        val v2 = (lens set flag2)(v0)
        updated(v2)
      }


    // Сигнал выставления порядка эджей.
    case m: OrderSet =>
      val v0 = value

      val lens = MEdgeEditRoot.edge
        .composeLens( MEdge.order )

      if (lens.get(v0) ==* m.order) {
        noChange
      } else {
        val v2 = (lens set m.order)(v0)
        updated( v2 )
      }


    // Редактирование неиндексируемого текста.
    case m: TextNiSet =>
      val v0 = value

      val lens = MEdgeEditRoot.edge
        .composeLens( MEdge.info )
        .composeLens( MEdgeInfo.textNi )

      if (lens.get(v0) ==* m.commentNi) {
        noChange
      } else {
        val v2 = (lens set m.commentNi)(v0)
        updated(v2)
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
        val v2 = lens.set( req0.pending(startTimeMs) )(v0)
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
        val v2 = lens.set( req2 )(v0)

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
      if (v0.edit.saveReq.isPending) {
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
          val v2 = (lens set true)(v0)
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
              .composeLens( MEdgeEditS.deleteDia )
              .modify(
                MDeleteDiaS.deleteReq.set( req2 ) andThen
                MDeleteDiaS.opened.set( false )
              )
          )(v0)
          val fx = EdgeEditAh.rdrToSysNodeFx(v2)

          updated(v2, fx)

        } else {
          // Ошибка. Не скрывать диалог, отрендерить ошибку.
          val v2 = lens.set( req2 )(v0)
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
        val v2 = (lens set false)(v0)
        updated(v2)
      }

  }

}


object EdgeEditAh {

  private def root_edit_nodeIds_LENS = {
    MEdgeEditRoot.edit
      .composeLens( MEdgeEditS.nodeIds )
  }

  private def root_edit_deleteDia_LENS =
    MEdgeEditRoot.edit
      .composeLens( MEdgeEditS.deleteDia )
      .composeLens( MDeleteDiaS.opened )

  private def root_edit_deleteDia_req_LENS =
    MEdgeEditRoot.edit
      .composeLens( MEdgeEditS.deleteDia )
      .composeLens( MDeleteDiaS.deleteReq )

  private def root_edit_saveReq =
    MEdgeEditRoot.edit
      .composeLens( MEdgeEditS.saveReq )


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
