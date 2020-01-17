package io.suggest.n2.edge.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.n2.edge.{MEdge, MEdgeInfo}
import io.suggest.n2.edge.edit.m.{DeleteCancel, DeleteEdge, FlagSet, MDeleteDiaS, MEdgeEditRoot, MEdgeEditS, NodeIdAdd, NodeIdChange, OrderSet, PredicateChanged, TextNiSet}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 9:58
  * Description: Контроллер заливки файла в форме.
  */
class EdgeEditAh[M](
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

    case m: DeleteEdge =>
      val v0 = value
      if (v0.edit.saveReq.isPending) {
        noChange

      } else if (m.isDelete) {
        // Отправка запроса удаления на сервер
        ???

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

  def root_edit_nodeIds_LENS = {
    MEdgeEditRoot.edit
      .composeLens( MEdgeEditS.nodeIds )
  }

  def root_edit_deleteDia_LENS =
    MEdgeEditRoot.edit
      .composeLens( MEdgeEditS.deleteDia )
      .composeLens( MDeleteDiaS.opened )

}
