package io.suggest.lk.nodes.form.a.tree

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.adv.rcvr.IRcvrKey
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 21:50
  * Description: Diode action-handler
  */
class TreeAh[M](
                 api      : ILkNodesApi,
                 modelRW  : ModelRW[M, MTree]
               )
  extends ActionHandler(modelRW)
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
      val v2 = v0.withAddStates(
        v0.addStates + (m.rcvrKey -> MAddSubNodeState())
      )
      updated(v2)

    // Сигнал о вводе имени узла в форме добавления узла.
    case m: AddSubNodeNameChange =>
      _updateAddState(m) { _.withName( m.name ) }

    // Сигнал о вводе id узла в форме добавления узла.
    case m: AddSubNodeIdChange =>
      _updateAddState(m) { _.withId( Some(m.id) ) }

    // Сигнал о нажатии на кнопку "отмена" в форме добавления узла.
    case m: AddSubNodeCancelClick =>
      val v0 = value
      val v2 = v0.withAddStates(
        v0.addStates - m.rcvrKey
      )
      updated(v2)

    // Сигнал о необходимости показать какой-то узел подробнее.
    case nnc: NodeNameClick =>
      val rcvrKey = nnc.rcvrKey

      val v0 = value
      MNodeState
        .findSubNode(rcvrKey, v0.nodes)
        .filter { n =>
          val c = n.children
          c.isEmpty && !c.isPending
        }
        .fold(noChange) { _ =>
          val nodeId = nnc.rcvrKey.head

          // Собрать эффект запроса к серверу за подробностями по узлу.
          val fx = Effect {
            // Отправить запрос к серверу за данными по выбранному узлу, выставить ожидание ответа в состояние.
            api.subNodesOf(nodeId).transform { tryRes =>
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


    // Ответ сервера на тему под-узлов.
    case snr: HandleSubNodesOf =>
      val v0 = value
      val v2 = value.withNodes(
        MNodeState
          .flatMapSubNode(snr.rcvrKey, v0.nodes) { mns0 =>
            val children2 = snr.subNodesRespTry.fold(
              mns0.children.fail,
              {resp =>
                val mnsChildren = for (node <- resp.nodes) yield {
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

  }

}
