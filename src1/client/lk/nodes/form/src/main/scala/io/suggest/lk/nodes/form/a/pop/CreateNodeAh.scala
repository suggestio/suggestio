package io.suggest.lk.nodes.form.a.pop

import diode._
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.lk.nodes.{LkNodesConst, MLknNodeReq}
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

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
                       treeRO       : ModelRO[MTree],
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


    // Выбор родительского узла из списка.
    case m: CreateNodeParentChange =>
      (for {
        v0 <- value
      } yield {
        val v2 = (MCreateNodeS.parentPath set Some(m.nodePath))(v0)
        updated( Some(v2) )
      })
        .getOrElse( noChange )


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
          // Путь до родительского узла:
          parentPath = m.parentPath.filter { parentPath =>
            parentPath.nonEmpty && {
              val mtree = treeRO.value
              // Переданный путь должен содержать только реальные узлы допустимого типа и роли:
              (for {
                nodeIdsTree <- mtree.idsTree.toOption
                loc0 <- nodeIdsTree
                  .loc
                  .pathToNode( parentPath )
                mnsPath = loc0
                  .path
                  .iterator
                  .flatMap( mtree.nodesMap.get )
                  .to( LazyList )
                mns <- mnsPath.headOption
              } yield {
                mns
                  .infoPot
                  .exists { info =>
                    info.ntype.exists(_.userCanCreateSubNodes) &&
                    (info.isAdmin contains[Boolean] true)
                  } &&
                  // Цепочка узлов до указанного узла должна содержать нормальные узлы, не виртуальные категории.
                  mnsPath.forall { mns =>
                    (mns.role ==* MTreeRoles.Normal) ||
                    (mns.role ==* MTreeRoles.Root)
                  }
              })
                // Эмулируем сложный exists() через for-yield + getOrElse false:
                .getOrElseFalse
            }
            // Отфильтровываем возможные виртуальные узлы и пути до них, т.к. на выходе нужен только реальный RcvrKey.
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
        parentPath <- cs.parentPath
        mtree = treeRO.value
        nodeIdsTree <- mtree.idsTree.toOption
        parentNodeLoc <- nodeIdsTree
          .loc
          .pathToNode( parentPath )
        parentRk = mtree.nodesMap
          .mnsPath( parentNodeLoc )
          .rcvrKey
        if parentRk.nonEmpty
      } yield {
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
            .createSubNodeSubmit( parentRk, req )
            .transform { tryResp =>
              val action = CreateNodeResp( parentPath, parentRk, req, tryResp )
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
          logger.warn( ErrorMsgs.SRV_REQUEST_FAILED, ex, m )
          val v2 = for (cs <- value) yield {
            val invalidateF = MTextFieldS.isValid.set(false)
            cs.copy(
              name = invalidateF(cs.name),
              id   = if (cs.id.isEnabled) invalidateF(cs.id) else cs.id,
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
