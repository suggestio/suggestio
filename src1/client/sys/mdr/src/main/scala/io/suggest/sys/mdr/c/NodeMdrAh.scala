package io.suggest.sys.mdr.c

import diode.data.{PendingBase, Pot}
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.common.empty.OptionUtil
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sys.mdr.{MMdrActionInfo, MMdrConf, MMdrNextResp, MMdrResolution, MNodeMdrInfo, MdrSearchArgs}
import io.suggest.sys.mdr.m._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.sys.mdr.u.SysMdrUtil
import japgolly.univeq._
import monocle.Traversal
import scalaz.std.option._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.10.18 22:30
  * Description: Контроллер формы модерации.
  */
object NodeMdrAh {

  /** Вернуть конфиг для реквестов на сервер. */
  def getReqMdrConf(v0: MSysMdrRootS): MMdrConf = {
    if (v0.form.forceAllRcrvs && v0.conf.onNodeKey.nonEmpty) {
      v0.conf.withNodeKey( None )
    } else {
      v0.conf
    }
  }

}


/** Класс контроллера формы модерации. */
class NodeMdrAh[M](
                    api       : ISysMdrApi,
                    modelRW   : ModelRW[M, MSysMdrRootS]
                  )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  /** Эффект отправки mdr-команды на сервер с апдейтом состояния. pure-функция. */
  def _doMdrFx(info: MMdrActionInfo, reasonOpt: Option[String], v0: MSysMdrRootS): (MSysMdrRootS, Effect) = {
    val pot0 = v0.node.mdrPots.getOrElse(info, Pot.empty)
    val pot2 = pot0.pending()
    val fx = Effect {
      val startTimestampMs = pot2.asInstanceOf[PendingBase].startTime
      api.doMdr(
        MMdrResolution(
          nodeId  = v0.node.info.get.nodeOpt.get.info.nodeId,
          info    = info,
          reason  = reasonOpt,
          conf    = NodeMdrAh.getReqMdrConf( v0 ),
        )
      ).transform { tryRes =>
        val act = DoMdrResp(
          timestampMs = startTimestampMs,
          info        = info,
          tryResp     = tryRes
        )
        Success(act)
      }
    }

    val mdrPots2 = v0.node.mdrPots + (info -> pot2)
    val v2 = MSysMdrRootS.node
      .composeLens( MMdrNodeS.mdrPots )
      .set( mdrPots2 )(v0)

    (v2, fx)
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Ввод текста причины отказа в размещении.
    case m: SetDismissReason =>
      val v0 = value
      if (v0.dialogs.refuse.reason ==* m.reason) {
        // Текст причины не изменился.
        noChange
      } else {
        val v2 = MSysMdrRootS.dialogs
          .composeLens( MMdrDialogs.refuse )
          .composeLens( MMdrRefuseDialogS.reason )
          .set( m.reason )(v0)
        updated(v2)
      }


    // Результат запроса с командой модерации
    case m: DoMdrResp =>
      val v0 = value
      v0.node.mdrPots
        .get( m.info )
        .fold(noChange) { pot0 =>
          if (pot0 isPendingWithStartTime m.timestampMs) {
            // Это ожидаемый ответ сервера. Обработать его:
            val mdrPots2 = m.tryResp.fold(
              {ex =>
                v0.node.mdrPots + (m.info -> pot0.fail(ex))
              },
              {_ =>
                v0.node.mdrPots - m.info
              }
            )

            // Если открыт refuse-диалог, то закрыть его.
            val dia2 = if (m.tryResp.isSuccess && (v0.dialogs.refuse.actionInfo contains m.info)) {
              // Закрыть диалог, т.к. всё ок.
              MMdrDialogs.refuse
                .composeLens( MMdrRefuseDialogS.actionInfo )
                .set( None )(v0.dialogs)
            } else {
              v0.dialogs
            }

            // Надо почистить item'ы, которые осталось промодерировать.
            val mdrRespPot2 = if (m.tryResp.isSuccess) {
              // Если всё ок, то надо удалить уже-промодерированные-item'ы из состояния.
              v0.node.info.map {
                MMdrNextRespJs.resp
                  .composeLens( MMdrNextResp.nodeOpt )
                  .composeTraversal( Traversal.fromTraverse[Option, MNodeMdrInfo] )
                  .modify { nodeInfo =>
                    val isActionInfoEmpty = m.info.isEmpty

                    val items2 = if (isActionInfoEmpty) {
                      List.empty
                    } else if ( m.info.itemType.isEmpty && m.info.itemId.isEmpty ) {
                      nodeInfo.items
                    } else {
                      nodeInfo
                        .items
                        .filterNot { mitem =>
                          (m.info.itemType contains mitem.iType) ||
                            (mitem.id ==* m.info.itemId)
                        }
                    }

                    val directSelfNodeIds2 = if (isActionInfoEmpty || m.info.directSelfAll) {
                      Set.empty[String]
                    } else if (m.info.directSelfId.isEmpty) {
                      nodeInfo.directSelfNodeIds
                    } else {
                      nodeInfo.directSelfNodeIds -- m.info.directSelfId
                    }

                    nodeInfo.copy(
                      items             = items2,
                      directSelfNodeIds = directSelfNodeIds2
                    )
                  }
              }

            } else {
              // При ошибках не надо чистить данные ноды
              v0.node.info
            }

            val v2 = v0.copy(
              dialogs = dia2,
              node = v0.node.copy(
                mdrPots = mdrPots2,
                info    = mdrRespPot2
              )
            )

            // Если больше не осталось элементов для модерации, то надо запросить новый элемент для модерации.
            val nextNodeFxOpt = OptionUtil.maybe {
              mdrRespPot2.exists {_.resp.nodeOpt.exists { resp =>
                resp.items.isEmpty  &&  resp.directSelfNodeIds.isEmpty
              }}
            } {
              MdrNextNode( skipCurrentNode = true ).toEffectPure
            }

            ah.updatedMaybeEffect(v2, nextNodeFxOpt)

          } else {
            // Есть ещё какой-то запущенный запрос, который более актуален.
            LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
            noChange
          }
        }


    // Нажимание кнопок аппрува или отказа в списке размещений.
    case m: ApproveOrDismiss =>
      val v0 = value
      if (m.isApprove) {
        // Аппрув - немедленный эффект запроса на сервер.
        val (v2, fx) = _doMdrFx(m.info, reasonOpt = None, v0)
        updated(v2, fx)

      } else {
        // Отказ - нужен диалог отказа с указанием причины отказа.
        val v2 = root_dialogs_refuse_actionInfo_LENS
          .set( Some(m.info) )(v0)

        updated(v2)
      }


    // В диалоге отказа нажато подтверждение
    case DismissOkClick =>
      val v0 = value
      val refuse = v0.dialogs.refuse
      refuse.actionInfo.fold(noChange) { ai =>
        val (v2, fx) = _doMdrFx(ai, Some(refuse.reason), v0)
        updated(v2, fx)
      }


    // В диалоге отказа нажата кнопка отмены:
    case DismissCancelClick =>
      val v0 = value
      v0.dialogs.refuse.actionInfo.fold {
        // Дублирующееся событие, диалог уже закрыт.
        noChange
      } { _ =>
        val v2 = root_dialogs_refuse_actionInfo_LENS.set( None )(v0)
        updated(v2)
      }


    // Переход к следующему узлу, который требует модерации.
    case m: MdrNextNode =>
      val v0 = value
      if (v0.node.info.isPending) {
        noChange

      } else {
        val infoReq2 = v0.node.info.pending()

        // Организовать запрос на сервер:
        val fx = Effect {
          // Шагание обновляет offset:
          var offset2 = v0.node.nodeOffset

          // Если происходит переключение между модерируемыми узлами, то вычислить новый offset:
          if (m.offsetDelta !=* 0) {
            offset2 += m.offsetDelta
            // При шагании назад надо автоматом "пропустить" ошибочные узлы:
            if (m.offsetDelta < 0) for {
              info <- infoReq2.toOption
              if info.resp.errorNodeIds.nonEmpty
            } {
              offset2 -= info.resp.errorNodeIds.size
            }
          }

          val args = MdrSearchArgs(
            // TODO Добавить поддержку rcvrKey.
            // Пропустить текущую карточку, если требуется экшеном:
            hideAdIdOpt = OptionUtil.maybeOpt(m.skipCurrentNode) {
              for {
                resp  <- v0.node.info.toOption
                mnode <- resp.nodeOpt
              } yield {
                mnode.info.nodeId
              }
            },
            // Сдвиг соответствует запрашиваемому.
            offsetOpt = Some( offset2 ),
            conf = NodeMdrAh.getReqMdrConf( v0 )
          )
          api.nextMdrInfo(args)
            .transform { tryResp =>
              val r = MdrNextNodeResp(
                timestampMs   = infoReq2.asInstanceOf[PendingBase].startTime,
                tryResp       = tryResp,
                reqOffset     = offset2
              )
              Success(r)
            }
        }

        // И результат экшена...
        val v2 = MSysMdrRootS.node
          .composeLens(MMdrNodeS.info)
          .set(infoReq2)(v0)

        updated(v2 , fx)
      }


    // Поступил результат реквеста к серверу за новыми данными для модерации.
    case m: MdrNextNodeResp =>
      val v0 = value
      if (v0.node.info isPendingWithStartTime m.timestampMs) {
        // Это ожидаемый ответ сервера. Обработать его:
        val infoRespPot2 = v0.node.info
          .withTry( m.tryResp.map(MMdrNextRespJs.apply) )
        val jdRuntime2 = SysMdrUtil.mkJdRuntime(
          jdRuntimeOpt = Some(v0.node.jdRuntime),
          docs = (for {
            infoResp2 <- infoRespPot2.iterator
            node      <- infoResp2.nodeOpt
            nodeAd    <- node.info.ad
          } yield {
            nodeAd.doc
          })
            .toStream
        )

        val v2 = MSysMdrRootS.node.modify(
          _.copy(
            info        = infoRespPot2,
            jdRuntime   = jdRuntime2,
            mdrPots     = Map.empty,
            nodeOffset = {
              // Если успешный ответ содержит список ошибок узлов, то значит сервер перешагнул какие-то узлы автоматом. Надо тут их тоже прошагать с помощью offset:
              val errOffset = m.tryResp
                .toOption
                .fold(0)(_.errorNodeIds.size)
              Math.max( 0, m.reqOffset + errOffset )
            },
            fixNodePots = Map.empty
            // TODO Закрыть все открытые диалоги
          )
        )(v0)

        updated( v2 )

      } else {
        // Левый ответ какой-то, уже другой запрос запущен.
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }


    // Юзер кликнул по кнопке запуска авто-ремонта узла.
    case m: FixNode =>
      val v0 = value
      val potOpt0 = v0.node.fixNodePots.get(m.nodeId)
      val pot0 = potOpt0 getOrElse Pot.empty

      if (pot0.isPending) {
        noChange

      } else {
        val timestamp = System.currentTimeMillis()
        val fx = Effect {
          api
            .fixNode( m.nodeId )
            .transform { tryResp =>
              Success( FixNodeResp(m, timestamp, tryResp) )
            }
        }
        val pot2 = pot0.pending( timestamp )

        val v2 = MSysMdrRootS.node
          .composeLens( MMdrNodeS.fixNodePots )
          .modify { _ + (m.nodeId -> pot2) }(v0)

        updated(v2, fx)
      }


    // Запрос к серверу за ремонтом узла завершился.
    case m: FixNodeResp =>
      val v0 = value
      v0.node
        .fixNodePots
        .get( m.reason.nodeId )
        .fold {
          LOG.warn( ErrorMsgs.EXPECTED_FILE_MISSING, msg = m )
          noChange
        } { pot0 =>
          if (pot0 isPendingWithStartTime m.timeStampMs) {
            val pot2 = m.tryResp.fold(
              pot0.fail,
              { _ => pot0.ready(None) }
            )
            val v2 = MSysMdrRootS.node
              .composeLens( MMdrNodeS.fixNodePots )
              .modify( _ + (m.reason.nodeId -> pot2) )(v0)
            updated(v2)

          } else {
            LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
            noChange
          }
        }


    // Изменение состояния галочки форсирования поиска со всех узлов.
    case m: SetForceAllNodes =>
      val v0 = value
      if (m.forceAllNodes ==* v0.form.forceAllRcrvs) {
        noChange

      } else {
        val v2 = MSysMdrRootS.form
          .composeLens( MMdrFormS.forceAllRcvrs )
          .set( m.forceAllNodes )(v0)

        // Запустить пересборку текущего view с перемоткой в начало:
        val fx = MdrNextNode( offsetDelta = -v0.node.nodeOffset )
          .toEffectPure

        updated( v2, fx )
      }

  }


  private def root_dialogs_refuse_actionInfo_LENS = {
    MSysMdrRootS.dialogs
      .composeLens( MMdrDialogs.refuse )
      .composeLens( MMdrRefuseDialogS.actionInfo )
  }

}
