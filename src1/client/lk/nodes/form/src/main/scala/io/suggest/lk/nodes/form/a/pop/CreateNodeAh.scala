package io.suggest.lk.nodes.form.a.pop

import diode._
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.lk.nodes.MLknNodeReq
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.nodes.form.u.LknFormUtilR
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.03.17 23:48
  * Description: Action handler для popup-формы создания узла.
  */
class CreateNodeAh[M](
                       api          : ILkNodesApi,
                       modelRW      : ModelRW[M, Option[MCreateNodeS]],
                       //confRO     : ModelRO[MLknConf],
                       currNodeRO   : ModelRO[Option[RcvrKey]]
                     )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал о клике юзера по кнопке добавления под-узла.
    case CreateNodeClick =>
      val v0 = value
      v0.fold {
        updated( Some( MCreateNodeS() ) )
      } { existing =>
        // should never happen
        logger.log( ErrorMsgs.EVENT_ALREADY_LISTENED_BY, msg = existing )
        noChange
      }


    // Сигнал о вводе имени узла в форме добавления узла.
    case m: CreateNodeNameChange =>
      val name2 = LknFormUtilR.normalizeNodeName( m.name )
      val nameValid2 = LknFormUtilR.isNameValid( name2 )

      val v2 = for (cs <- value) yield {
        cs.withName(name2, nameValid2)
      }
      updated(v2)


    // Сигнал о вводе id узла в форме добавления узла.
    case m: CreateNodeIdChange =>
      // Сопоставить с паттерном маячка.
      val id2 = LknFormUtilR.normalizeBeaconId( m.id )
      val isIdValid = LknFormUtilR.isBeaconIdValid( id2 )

      val v2 = for (cs <- value) yield {
        cs.withId( Some(id2), isIdValid )
      }
      updated(v2)


    // Сигнал о нажатии на кнопку "отмена" в форме добавления узла.
    case CreateNodeCancelClick =>
      updated( None )


    // Сигнал клика по кнопке запуска создания нового узла.
    case CreateNodeSaveClick =>
      val v0 = value
      v0.filter( _.isValid  ).fold {
        // Игнорить нажатие, пусть юзер введёт все данные.
        noChange

      } { cs =>
        val parentNodeRcvrKey = currNodeRO().get
        val parentNodeId = parentNodeRcvrKey.last

        // Огранизовать запрос на сервер.
        val fx = Effect {
          val req = MLknNodeReq(
            name = cs.name.trim,
            id   = cs.id
          )
          api
            .createSubNodeSubmit(parentNodeId, req)
            .transform { tryResp =>
              val action = CreateNodeResp(
                tryResp = tryResp
              )
              Success(action)
            }
        }

        // Выставить в addState флаг текущего запроса.
        val cs2 = cs.withSavingPending()
        updated(Some(cs2), fx)
      }


    // Положительный ответ сервера по поводу добавления нового узла.
    case m: CreateNodeResp =>
      m.tryResp.fold(
        // Ошибка. Вернуть addState назад.
        {ex =>
          val v2 = for (cs <- value) yield {
            cs.withSaving(
              cs.saving.fail( LknException(ex) )
            )
          }
          updated(v2)
        },

        // Всё ок. Удалить addState для текущего rcvrKey
        {_ =>
          updated( None )
        }
      )

  }

}
