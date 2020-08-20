package io.suggest.lk.nodes.form.a.pop

import diode.{ActionHandler, ActionResult, Effect, ModelRO, ModelRW}
import io.suggest.adn.edit.NodeEditConstants
import io.suggest.lk.nodes.MLknNodeReq
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.lk.nodes.form.m.{MEditNodeState, MNodeState, NodeEditCancelClick, NodeEditClick, NodeEditNameChange, NodeEditOkClick, NodeEditSaveResp}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.text.StringUtil
import io.suggest.spa.DiodeUtil.Implicits._
import japgolly.univeq._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.08.2020 8:24
  * Description: Контроллер окна редактирования названия узла.
  */
class NameEditAh[M](
                     api              : ILkNodesApi,
                     modelRW          : ModelRW[M, Option[MEditNodeState]],
                     currNodeRO       : ModelRO[Option[MNodeState]],
                   )
  extends ActionHandler( modelRW )
  with Log
{

  // Нормализация, не срезая пробелы по краям.
  def earlyNormalizeName(name: String): String = {
    StringUtil.strLimitLen(
      str     = name,
      maxLen  = NodeEditConstants.Name.LEN_MAX,
      ellipsis = ""
    )
  }

  // Полная нормализация названия узла.
  def normalizeName(name: String): String = {
    StringUtil.trimRight(
      earlyNormalizeName(
        StringUtil.trimLeft(name) ) )
  }


  def isNameValid(name: String): Boolean =
    (name.length >= NodeEditConstants.Name.LEN_MIN)


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case NodeEditClick =>
      val v0 = value
      if (v0.nonEmpty) {
        // Повторный клик по кнопке редактирования.
        noChange

      } else {
        // Раскрыть диалог:
        val name = currNodeRO.value.get.info.name
        val v2 = Some( MEditNodeState(
          name        = name,
          nameValid   = isNameValid( name ),
        ))
        updated( v2 )
      }


    case m: NodeEditNameChange =>
      (for {
        edit0 <- value
        name2 = earlyNormalizeName( m.name )
        nameValid2 = isNameValid( name2 )
        if (name2 !=* edit0.name) || (nameValid2 !=* edit0.nameValid)
      } yield {
        val edit2 = edit0.copy(
          name        = name2,
          nameValid   = nameValid2,
        )
        val v2 = Some( edit2 )
        updated(v2)
      })
        .getOrElse( noChange )


    case NodeEditCancelClick =>
      (for {
        _ <- value
      } yield {
        updated( None )
      })
        .getOrElse( noChange )


    // Сигнал подтверждения редактирования узла.
    case NodeEditOkClick =>
      (for {
        edit0 <- value
        if !edit0.saving.isPending
        name2 = normalizeName( edit0.name )
        if isNameValid(name2)
        currNode <- currNodeRO.value
      } yield {
        // Эффект обновления данных узла на сервере.
        val fx = Effect {
          val req = MLknNodeReq(
            name  = name2,
            id    = None
          )
          val nodeId = currNode.info.id
          api
            .editNode( nodeId, req )
            .transform { tryResp =>
              val r = NodeEditSaveResp( nodeId, tryResp )
              Success(r)
            }
        }

        val edit2 = edit0.copy(
          name = name2,
          nameValid = true,
          saving = edit0.saving.pending(),
        )
        val v2 = Some(edit2)
        updated(v2, fx)
      })
        .getOrElse( noChange )


    // Сигнал завершения запроса сохранения с сервера.
    case m: NodeEditSaveResp =>
      val currNodeIdOpt = currNodeRO
        .value
        .map(_.info.id)

      (for {
        currNodeId <- currNodeIdOpt
        if currNodeId ==* m.nodeId
        edit0 <- value
      } yield {
        // Отработать ошибку или успех:
        m.tryResp.fold(
          {ex =>
            val edit2 = MEditNodeState.saving.modify( _.fail(ex) )(edit0)
            val v2 = Some(edit2)
            updated(v2)
          },
          {_ =>
            updated( None )
          }
        )
      })
        .getOrElse {
          logger.warn( ErrorMsgs.XHR_UNEXPECTED_RESP, msg = (m, currNodeIdOpt.orNull) )
          noChange
        }

  }

}
