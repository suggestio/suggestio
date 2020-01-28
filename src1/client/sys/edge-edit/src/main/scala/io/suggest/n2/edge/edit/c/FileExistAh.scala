package io.suggest.n2.edge.edit.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.n2.edge.edit.m.{FileExistAppendToNodeIds, FileExistCancel, FileExistReplaceNodeIds, MEdgeEditRoot, MEdgeEditS}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.01.2020 18:34
  * Description: Контроллер диалога для файла, который уже существует.
  */
class FileExistAh[M](
                      modelRW: ModelRW[M, MEdgeEditS]
                    )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case FileExistReplaceNodeIds =>
      val v0 = value

      v0.fileExistNodeId
        .fold(noChange) { nodeId =>
          val v2 = (
            MEdgeEditS.nodeIds
              .set( nodeId :: Nil ) andThen
            MEdgeEditS.fileExistNodeId
              .set( None )
          )(v0)
          updated( v2 )
        }


    case FileExistAppendToNodeIds =>
      val v0 = value

      v0.fileExistNodeId
        .fold( noChange ) { nodeId =>
          val v2 = (
            MEdgeEditS.nodeIds
              .modify( _ :+ nodeId ) andThen
            MEdgeEditS.fileExistNodeId
              .set( None )
          )(v0)
          updated( v2 )
        }


    case FileExistCancel =>
      val v0 = value

      if (v0.fileExistNodeId.isEmpty) {
        noChange
      } else {
        val v2 = MEdgeEditS.fileExistNodeId
          .set( None )(v0)
        updated(v2)
      }

  }

}
