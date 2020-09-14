package io.suggest.lk.nodes.form.a.pop

import diode._
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.lk.nodes.{LkNodesConst, MLknNodeReq}
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
                       currNodeRO   : ModelRO[Option[RcvrKey]]
                     )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал о вводе имени узла в форме добавления узла.
    case m: CreateNodeNameChange =>
      val name2 = LkNodesConst.normalizeNodeName( m.name )
      val nameValid2 = LkNodesConst.isNameValid( name2 )

      val v2 = for (cs0 <- value) yield {
        MCreateNodeS.name.modify(_.copy(
          value = name2,
          isValid = nameValid2,
        ))(cs0)
      }
      updated(v2)


    // Сигнал о вводе id узла в форме добавления узла.
    case m: CreateNodeIdChange =>
      // Сопоставить с паттерном маячка.
      val id2 = LkNodesConst.normalizeBeaconId( m.id )
      val isIdValid = LkNodesConst.isBeaconIdValid( id2 )

      val v2 = for (cs0 <- value) yield {
        MCreateNodeS.id.modify(_.copy(
          value = id2,
          isValid = isIdValid,
        ))(cs0)
      }
      updated(v2)


    // Сигнал о клике юзера по кнопке добавления под-узла.
    case m: CreateNodeClick =>
      val v0 = value
      v0.fold {
        lazy val mtfEmpty = MTextFieldS.empty

        val v2 = MCreateNodeS(
          // Выставить дефолтовое имя, если передано:
          name = m.nameDflt.fold(mtfEmpty) { name =>
            MTextFieldS(
              value = name,
              isValid = true,
              isEnabled = true,
            )
          },
          // Зафиксировать id, если указан:
          id = m.id.fold(mtfEmpty) { fixedId =>
            MTextFieldS(
              value   = fixedId,
              isValid = true,
              isEnabled = false,
            )
          },
        )
        updated( Some(v2) )

      } { existing =>
        // should never happen
        logger.log( ErrorMsgs.EVENT_ALREADY_LISTENED_BY, msg = existing )
        noChange
      }


    // Сигнал о нажатии на кнопку "Закрыть" в форме добавления узла.
    case CreateNodeCloseClick =>
      updated( None )


    // Сигнал клика по кнопке запуска создания нового узла.
    case CreateNodeSaveClick =>
      (for {
        cs <- value
        if cs.isValid && !cs.saving.isPending
      } yield {
        val parentNodeRcvrKey = currNodeRO().get

        // Огранизовать запрос на сервер.
        val fx = Effect {
          val req = MLknNodeReq(
            name = cs.name.value.trim,
            id = {
              val id = cs.id.value
              Option.when( id.nonEmpty )(id)
            }
          )
          api
            .createSubNodeSubmit(parentNodeRcvrKey, req)
            .transform { tryResp =>
              val action = CreateNodeResp(
                tryResp = tryResp
              )
              Success(action)
            }
        }

        // Выставить в addState флаг текущего запроса.
        val cs2 = MCreateNodeS.saving.modify(_.pending())(cs)
        updated(Some(cs2), fx)
      })
        .getOrElse( noChange )


    // Положительный ответ сервера по поводу добавления нового узла.
    case m: CreateNodeResp =>
      m.tryResp.fold(
        // Ошибка. Вернуть addState назад.
        {ex =>
          val v2 = for (cs <- value) yield {
            val invalidateF = MTextFieldS.isValid.set(false)
            cs.copy(
              name = invalidateF(cs.name),
              id   = invalidateF(cs.id),
              saving = cs.saving.fail(LknException(ex)),
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
