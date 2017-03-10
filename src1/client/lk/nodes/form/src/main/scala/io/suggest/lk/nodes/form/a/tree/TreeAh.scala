package io.suggest.lk.nodes.form.a.tree

import diode._
import diode.data.{Pending, Pot}
import io.suggest.adn.edit.NodeEditConstants
import io.suggest.adv.rcvr.IRcvrKey
import io.suggest.common.radio.BeaconUtil
import io.suggest.common.text.StringUtil
import io.suggest.lk.nodes.MLknNodeReq
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.{ErrorMsgs, WarnMsgs}

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 21:50
  * Description: Diode action-handler
  */
class TreeAh[M](
                 api          : ILkNodesApi,
                 modelRW      : ModelRW[M, MTree]
               )
  extends ActionHandler(modelRW)
  with Log
{

  private def _updateAddState(m: LkNodesAction with IRcvrKey)(f: MAddSubNodeState => MAddSubNodeState) = {
    val v0 = value
    val s0 = v0.addStates( m.rcvrKey )
    val s2 = f(s0)
    val v2 = v0.withAddStates(
      v0.addStates + (m.rcvrKey -> s2)
    )
    updated(v2)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал о клике юзера по кнопке добавления под-узла.
    case m: AddSubNodeClick =>
      val v0 = value
      val addState0 = MAddSubNodeState()
      val v2 = v0.withAddStates(
        v0.addStates + (m.rcvrKey -> addState0)
      )
      updated(v2)


    // Сигнал о вводе имени узла в форме добавления узла.
    case m: AddSubNodeNameChange =>
      _updateAddState(m) { addState0 =>
        val name2 = StringUtil.strLimitLen(
          str     = m.name.trim,
          maxLen  = NodeEditConstants.Name.LEN_MAX,
          ellipsis = ""
        )
        addState0.copy(
          name      = name2,
          nameValid = name2.length >= NodeEditConstants.Name.LEN_MIN
        )
      }


    // Сигнал о вводе id узла в форме добавления узла.
    case m: AddSubNodeIdChange =>
      _updateAddState(m) { addState0 =>
        val ed = BeaconUtil.EddyStone
        // Сопоставить с паттерном маячка.
        val id2 = StringUtil.strLimitLen(
          str       = m.id.toLowerCase,
          maxLen    = ed.NODE_ID_LEN,
          ellipsis  = ""
        )
        addState0.copy(
          idValid   = id2.matches( ed.EDDY_STONE_NODE_ID_RE_LC ),
          id        = Some(id2)
        )
      }


    // Сигнал о нажатии на кнопку "отмена" в форме добавления узла.
    case m: AddSubNodeCancelClick =>
      val v0 = value
      val v2 = v0.withAddStates(
        v0.addStates - m.rcvrKey
      )
      updated(v2)


    // Сигнал создания нового узла на сервере.
    case m: AddSubNodeSaveClick =>
      val v0 = value
      val rcvrKey = m.rcvrKey
      val addState0 = v0.addStates( rcvrKey )

      if (addState0.isValid) {

        // Огранизовать запрос на сервер.
        val fx = Effect {
          val parentNodeId = rcvrKey.last
          val req = MLknNodeReq(
            name = addState0.name,
            id   = addState0.id
          )
          api
            .createSubNodeSubmit(parentNodeId, req)
            .transform { tryResp =>
              val action = AddSubNodeResp(
                tryResp = tryResp,
                rcvrKey = rcvrKey
              )
              Success(action)
            }
        }

        // Выставить в addState флаг текущего запроса.
        val addState2 = addState0.withSaving(
          addState0.saving.pending()
        )
        val v2 = v0.withAddStates(
          addStates2 = v0.addStates + (rcvrKey -> addState2)
        )
        updated(v2, fx)

      } else {
        // Игнорить нажатие, пусть юзер введёт все данные.
        noChange
      }


    // Положительный ответ сервера по поводу добавления нового узла.
    case m: AddSubNodeResp =>
      m.tryResp.fold(
        {ex =>
          // Вернуть addState назад.
          _updateAddState(m) { addState0 =>
            addState0.withSaving(
              addState0.saving.fail(ex)
            )
          }
        },
        {resp =>
          // Залить обновления в дерево, удалить addState для текущего rcvrKey
          val v0 = value
          val v2 = v0.copy(
            addStates = v0.addStates - m.rcvrKey,
            nodes = MNodeState
              .updateChildren(m.rcvrKey, v0.nodes) { children0 =>
                val mns0 = MNodeState(
                  info = resp
                )
                children0.toIterator ++ (mns0 :: Nil)
              }
              .toSeq
          )
          updated(v2)
        }
      )


    // Сигнал о необходимости показать какой-то узел подробнее.
    case nnc: NodeNameClick =>
      val rcvrKey = nnc.rcvrKey

      val v0 = value
      MNodeState
        .findSubNode(rcvrKey, v0.nodes)
        .fold {
          LOG.log( ErrorMsgs.NODE_NOT_FOUND, msg = nnc )
          noChange

        } { n =>
          if (n.children.isPending) {
            // Происходит запрос к серверу за данными.
            LOG.log( WarnMsgs.REQUEST_IN_PROGRESS, msg = nnc )
            noChange

          } else if (n.children.exists(_.nonEmpty)) {
            // Есть под-элементы. Свернуть подсписок, забыв их всех.
            val v2 = v0.withNodes(
              MNodeState
                .flatMapSubNode(rcvrKey, v0.nodes) { mns0 =>
                  val mns1 = mns0.withChildren( Pot.empty )
                  mns1 :: Nil
                }
                .toList
            )
            updated(v2)

          } else {
            // children.isEmpty, значит нужно запросить их с сервера
            val nodeId = nnc.rcvrKey.last

            // Собрать эффект запроса к серверу за подробностями по узлу.
            val fx = Effect {
              // Отправить запрос к серверу за данными по выбранному узлу, выставить ожидание ответа в состояние.
              api.nodeInfo(nodeId).transform { tryRes =>
                Success( HandleSubNodesOf(rcvrKey, tryRes) )
              }
            }

            // Произвести обновление модели.
            val v2 = v0.withNodes(
              MNodeState
                .flatMapSubNode(rcvrKey, v0.nodes) { mns0 =>
                  val mns1 = mns0.withChildren(
                    mns0.children.pending()
                  )
                  mns1 :: Nil
                }
                .toList
            )

            updated(v2, fx)
          }
        }


    // Ответ сервера на тему под-узлов.
    case snr: HandleSubNodesOf =>
      val v0 = value
      val v2 = value.withNodes(
        MNodeState
          .flatMapSubNode(snr.rcvrKey, v0.nodes) { mns0 =>
            val children2 = snr.subNodesRespTry.fold(
              mns0.children.fail,
              {resp =>
                val mnsChildren = for (node <- resp.children) yield {
                  MNodeState(node)
                }
                mns0.children.ready( mnsChildren )
              }
            )
            val mns2 = mns0.withChildren( children2 )
            mns2 :: Nil
          }
          .toList
      )
      updated(v2)


    // Сигнал о клике юзером по галочке управления флагом isEnabled у какого-то узла.
    case m: NodeIsEnabledChanged =>
      val v0 = value
      val nodeOpt = MNodeState.findSubNode(m.rcvrKey, v0.nodes)
      nodeOpt
        .filter( _.info.canChangeAvailability.contains(true) )
        .fold {
          // Пришёл сигнал управления галочкой, но у юзера нет прав влиять на эту галочку.
          val errCode = if (nodeOpt.isDefined)
            ErrorMsgs.ACTION_WILL_BE_FORBIDDEN_BY_SERVER
          else
            ErrorMsgs.NODE_NOT_FOUND
          LOG.warn( errCode, msg = m )

          // Без изменений, т.к. любые действия в данной ситуации бессмыслены.
          noChange

        } { _ =>
          // Выставить новое значение галочки в состояние узла, организовать реквест на сервер с апдейтом.
          val v2 = v0.withNodes(
            MNodeState
              .flatMapSubNode( m.rcvrKey, v0.nodes ) { n0 =>
                val n2 = n0.withNodeEnabledUpd(
                  Some( MNodeEnabledUpdateState(
                    newIsEnabled  = m.isEnabled,
                    request       = Pending()
                  ) )
                )
                n2 :: Nil
              }
              .toList
          )

          // Эффект апдейта на сервере:
          val fx = Effect {
            val rcvrKey = m.rcvrKey
            api.setNodeEnabled(rcvrKey.last, m.isEnabled).transform { tryRes =>
              val r = NodeIsEnabledUpdateResp(rcvrKey, tryRes)
              Success(r)
            }
          }

          // Вернуть итог наверх...
          updated(v2, fx)
        }


    // Ответ сервер по поводу флага isEnabled для какого-то узла.
    case m: NodeIsEnabledUpdateResp =>
      val v0 = value
      val nodes2 = MNodeState
        .flatMapSubNode(m.rcvrKey, v0.nodes) { nodeState =>
          val nodeState2 = m.resp.fold [MNodeState] (
            // При ошибке запроса: сохранить ошибку в состояние
            {ex =>
              nodeState.withNodeEnabledUpd(
                nodeState.isEnabledUpd.map { nodeEnabledState =>
                  nodeEnabledState.copy(
                    newIsEnabled  = nodeState.info.isEnabled,
                    request       = nodeEnabledState.request.fail(ex)
                  )
                }
              )
            },

            // Если всё ок, то обновить состояние текущего узла.
            {newNodeInfo =>
              nodeState.copy(
                info            = newNodeInfo,
                isEnabledUpd  = None
              )
            }
          )
          nodeState2 :: Nil
        }
        .toList

      val v2 = v0.withNodes(nodes2)
      updated(v2)

  }

}
