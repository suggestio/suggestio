package io.suggest.lk.nodes.form.a.tree

import diode._
import diode.data.{Pending, Pot}
import io.suggest.lk.nodes.{MLknAdv, MLknConf, MLknNode}
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.log.Log
import io.suggest.scalaz.NodePath_t
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.sjs.dom2.DomQuick
import monocle.Traversal
import scalaz.std.option._
import japgolly.univeq._
import scalaz.{Tree, TreeLoc}

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 21:50
  * Description: Diode action-handler
  */
class TreeAh[M](
                 api          : ILkNodesApi,
                 modelRW      : ModelRW[M, MTree],
                 confRO       : ModelRO[MLknConf]
               )
  extends ActionHandler(modelRW)
  with Log
{

  private def _maybeUpdateNode[T](m: Any = null, nodePathOpt: Option[NodePath_t] = None)
                                 (f: MNodeState => Option[MNodeState]): ActionResult[M] = {
    val v0 = value
    (for {
      loc0 <- {
        val locOpt0 = nodePathOpt.fold(v0.openedLoc) {
          v0.nodes
            .loc
            .pathToNode
        }
        if (locOpt0.isEmpty)
          logger.warn( ErrorMsgs.NODE_PATH_MISSING_INVALID, msg = (nodePathOpt.orElse(v0.opened).orNull, m) )
        locOpt0
      }
      // noChange, если вернули None, т.е. когда ничего менять не надо.
      n2 <- f( loc0.getLabel )
    } yield {
      val t2 = loc0
        .setLabel( n2 )
        .toTree
      val v2 = (MTree.nodes set t2)(v0)
      updated(v2)
    })
      .getOrElse( noChange )
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал о необходимости показать какой-то узел подробнее.
    case m: NodeNameClick =>
      val v0 = value
      val locOpt0 = v0.nodes
        .loc
        .pathToNode( m.nodePath )
      if (locOpt0.isEmpty)
        logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )

      (for {
        loc0 <- locOpt0
        mns0 = loc0.getLabel
      } yield {
        if (mns0.info.isDetailed) {
          // Дочерние элементы уже получены с сервера. Даже если их нет. Сфокусироваться на текущий узел либо свернуть его.
          // Узнать, является ли текущий элемент сфокусированным?
          val v2 = MTree.opened.modify { opened0 =>
            // Окликнутый узел не является сфокусированным, но для него уже загружены children. Выставить текущий узел как сфокусированный.
            val isCurrentNodeClicked = opened0 contains[NodePath_t] m.nodePath
            if (isCurrentNodeClicked) {
              // Клик по заголовку текущего узла. Свернуть текущий узел, но оставив родительский элемент раскрытым.
              Option.when(m.nodePath.nonEmpty) {
                // Убрать последний элемент из пути до текущего узла:
                m.nodePath.slice(0, m.nodePath.length - 1)
              }
            } else {
              Some( m.nodePath )
            }
          }(v0)
          updated(v2)

        } else {
          // !isDetailed: Для текущего узла не загружено children. Запустить в фоне загрузку, выставить её в состояние.
          val nodeId = mns0.info.id
          val tstampMs = System.currentTimeMillis()

          // Собрать эффект запроса к серверу за подробностями по узлу.
          val fx = Effect {
            // Отправить запрос к серверу за данными по выбранному узлу, выставить ожидание ответа в состояние.
            api
              .nodeInfo(nodeId)
              .transform { tryRes =>
                Success( HandleSubNodesOf(m.nodePath, tryRes, tstampMs) )
              }
          }

          val v2 = MTree.nodes.set {
            loc0
              .modifyLabel( MNodeState.infoPot.modify( _.pending(tstampMs) ) )
              .toTree
          }(v0)

          updated(v2, fx)
        }
      })
        .getOrElse( noChange )


    // Ответ сервера на тему под-узлов.
    case snr: HandleSubNodesOf =>
      val v0 = value

      val loc0Opt = v0.nodes
        .loc
        .pathToNode( snr.nodePath )
      if (loc0Opt.isEmpty)
        logger.error( ErrorMsgs.NODE_PATH_MISSING_INVALID, msg = snr )

      (for {
        loc0 <- loc0Opt
        mns0 = loc0.getLabel
        if {
          val r = mns0.infoPot isPendingWithStartTime snr.tstampMs
          if (!r) logger.log( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = (snr, mns0.infoPot.state) )
          r
        }
      } yield {
        val loc2 = snr.subNodesRespTry.fold(
          // Ошибка запроса. Сохранить её в состояние.
          {ex =>
            loc0.modifyLabel(
              MNodeState.infoPot.modify( _.fail(ex) )
            )
          },

          // Положительный ответ сервера, обновить данные по текущему узлу, замёржив поддерево в текущий loc.
          {resp =>
            // Самоконтроль: получено поддерево для правильного узла:
            require( resp.subTree.rootLabel.id ==* mns0.info.id )

            val tree2 = for (lkNode <- resp.subTree) yield {
              MNodeState(
                infoPot = Pot.empty.ready( lkNode ),
              )
            }
            val subTree2 = Tree.Node(
              // Текущий узел - просто патчим, сохраняя рантаймовые под-состояния нетронутыми.
              root   = (MNodeState.infoPot set tree2.rootLabel.infoPot)(mns0),
              // Под-узлы - берём, что сервер прислал:
              forest = tree2.subForest,
            )

            // собрать поддерево MNodeState на основе текущего состояния и полученного с сервера поддерева.
            loc0.setTree( subTree2 )
          }
        )

        val v2 = v0.copy(
          nodes  = loc2.toTree,
          opened = Some( snr.nodePath ),
        )
        updated(v2)
      })
        .getOrElse(noChange)


    // Юзер ткнул галочку размещения карточки на узле. Отправить запрос апдейта на сервер, обновив состояние узла.
    case m: AdvOnNodeChanged =>
      val conf = confRO()

      if (conf.adIdOpt.isEmpty)
        // adId не задан в конфиге формы. Should never happen.
        logger.error( ErrorMsgs.AD_ID_IS_EMPTY, msg = (m, conf) )

      (for {
        adId <- conf.adIdOpt
        v0 = value
        locOpt0 = v0.nodes
          .loc
          .pathToNode( m.nodePath )
        loc0 <- {
          if (locOpt0.isEmpty)
            logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          locOpt0
        }
        mns0 = loc0.getLabel
        if {
          val r = mns0.adv.isEmpty
          if (!r) logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, mns0) )
          r
        }
      } yield {
        // Всё ок, можно обновлять текущий узел и запускать реквест на сервер.
        val fx = Effect {
          // реквест на сервер:
          api
            .setAdv(
              adId      = adId,
              isEnabled = m.isEnabled,
              onNode    = loc0.rcvrKey,
            )
            .transform { tryResp =>
              Success( AdvOnNodeResp(m.nodePath, tryResp) )
            }
        }

        val tree2 = loc0
          .modifyLabel( MNodeState.adv.set(
            Some( MNodeAdvState(
              newIsEnabledPot = Pot.empty[Boolean]
                .ready(m.isEnabled)
                .pending()
            ))
          ))
          .toTree

        val v2 = (MTree.nodes set tree2)(v0)
        updated(v2, fx)
      })
        .getOrElse( noChange )


    // Сигнал завершения реквеста изменения состояния размещения карточки на узле.
    case m: AdvOnNodeResp =>
      val v0 = value
      val locOpt0 = v0.nodes
        .loc
        .pathToNode( m.nodePath )

      if (locOpt0.isEmpty)
        logger.warn( ErrorMsgs.NODE_PATH_MISSING_INVALID, msg = m )

      (for {
        loc0 <- locOpt0
      } yield {
        val v2 = MTree.nodes.set(
          loc0
            .modifyLabel(
              m.tryResp.fold(
                {ex =>
                  logger.error(ErrorMsgs.SRV_REQUEST_FAILED, ex, msg = m)
                  MNodeState.adv
                    .composeTraversal( Traversal.fromTraverse[Option, MNodeAdvState] )
                    .composeLens( MNodeAdvState.newIsEnabledPot )
                    .modify( _.fail(ex) )
                },
                {info2 =>
                  MNodeState.infoPot.modify( _.ready(info2) ) andThen
                  MNodeState.adv.set( None )
                }
              )
            )
            .toTree
        )(v0)

        updated(v2)
      })
        .getOrElse(noChange)


    // Положительный ответ сервера по поводу добавления нового узла.
    case m: CreateNodeResp =>
      val v0 = value
      (for {
        loc0 <- v0.openedLoc
        // Ошибки с сервера отрабатываются в CreateNodeAh:
        resp <- m.tryResp.toOption
        newChild = Tree.Leaf(
          MNodeState(
            infoPot = Pot.empty.ready( resp ),
          )
        )
      } yield {
        // Залить обновления в дерево, а CreateNodeAh удалит addState для текущего rcvrKey.
        val v2 = MTree.nodes.set {
          loc0
            .insertDownLast( newChild )
            .toTree
        }(v0)
        updated(v2)
      })
        .getOrElse( noChange )


    // Сигнал о клике юзером по галочке управления флагом isEnabled у какого-то узла.
    case m: NodeIsEnabledChanged =>
      val v0 = value
      val locOpt0 = v0.openedLoc
      if (locOpt0.isEmpty)
        logger.warn( ErrorMsgs.NODE_PATH_MISSING_INVALID, msg = m )

      (for {
        loc0 <- locOpt0
        mns0 = loc0.getLabel
        if {
          val r = mns0.infoPot.exists(_.canChangeAvailability contains[Boolean] true)
          // TODO Нужно рендерить что-то на экран юзеру. Сейчас тут возвращается noChange, т.е. просто сбросом галочки.
          if (!r)
            // Пришёл сигнал управления галочкой, но у юзера нет прав влиять на эту галочку.
            logger.warn( ErrorMsgs.ACTION_WILL_BE_FORBIDDEN_BY_SERVER, msg = m )
          r
        }
        opened <- v0.opened
      } yield {
        // Выставить новое значение галочки в состояние узла, организовать реквест на сервер с апдейтом.
        val fx = Effect {
          api
            .setNodeEnabled( mns0.info.id, m.isEnabled )
            .transform { tryRes =>
              val r = NodeIsEnabledUpdateResp(opened, tryRes)
              Success(r)
            }
        }

        val v2 = MTree.nodes.set {
          loc0
            .modifyLabel {
              MNodeState.isEnableUpd.set(
                Some( MNodeEnabledUpdateState(
                  newIsEnabled  = m.isEnabled,
                  request       = Pending()
                ) )
              )
            }
            .toTree
        }(v0)

        updated(v2, fx)
      })
        .getOrElse( noChange )


    // Ответ сервер по поводу флага isEnabled для какого-то узла.
    case m: NodeIsEnabledUpdateResp =>
      val v0 = value

      (for {
        loc0 <- v0.nodes
          .loc
          .pathToNode( m.nodePath )
        mns0 = loc0.getLabel
      } yield {
        val v2 = MTree.nodes.set {
          loc0
            .modifyLabel(
              m.resp.fold(
                // При ошибке запроса: сохранить ошибку в состояние
                {ex =>
                  MNodeState.isEnableUpd
                    .composeTraversal( Traversal.fromTraverse[Option, MNodeEnabledUpdateState] )
                    .modify { nodeEnabledState =>
                      nodeEnabledState.copy(
                        newIsEnabled  = mns0.info.isEnabled,
                        request       = nodeEnabledState.request.fail(ex)
                      )
                    }
                },

                // Если всё ок, то обновить состояние текущего узла.
                {newNodeInfo =>
                  MNodeState.infoPot.modify(_.ready( newNodeInfo )) andThen
                  MNodeState.isEnableUpd.set( None )
                }
              )
            )
            .toTree
        }(v0)

        updated(v2)
      })
        .getOrElse( noChange )


    // Сигнал о завершении запроса к серверу по поводу удаления узла.
    case m: NodeDeleteResp =>
      val v0 = value

      (for {
        loc0 <- v0.nodes
          .loc
          .pathToNode( m.nodePath )
      } yield {
        val loc2 = m.resp.fold(
          {ex =>
            loc0
              .modifyLabel(
                MNodeState.infoPot.modify(_.fail(ex))
              )
          },
          {_ =>
            // isDeleted=false может ознаачть, что узел уже был удалён ранее, параллельным запросом. Считаем, что всегда true.
            loc0
              .delete
              // В корне пирамиды находится юзер. Удалить сам себя он наверное не может...
              .getOrElse {
                // Но если смог, то надо перезагружать страницу:
                DomQuick.reloadPage()
                throw new IllegalStateException()
              }
          }
        )

        val v2 = (MTree.nodes set loc2.toTree)(v0)
        updated(v2)
      })
        .getOrElse( noChange )


    // Сигнал завершения запроса сохранения с сервера.
    case m: NodeEditSaveResp =>
      val v0 = value

      (for {
        loc0 <- {
          // Нужно найти узел, который обновился. Теоретически возможно, что это не текущий узел, хоть и маловероятно.
          def findF(treeLoc: TreeLoc[MNodeState]): Boolean =
            treeLoc.getLabel.info.id ==* m.nodeId
          v0.openedLoc
            .filter(findF)
            .orElse {
              logger.log( ErrorMsgs.NODE_PATH_MISSING_INVALID, msg = (m, v0.opened) )
              v0.nodes.loc.find(findF)
            }
        }
        nodeInfo2 <- m.tryResp.toOption
      } yield {
        val v2 = MTree.nodes.set {
          loc0
            .modifyLabel(
              MNodeState.infoPot.modify( _.ready(nodeInfo2) )
            )
            .toTree
        }(v0)
        updated(v2)
      })
        .getOrElse( noChange )


    // Юзер почитать развернуть подробности по тарифу.
    case TfDailyShowDetails =>
      val v0 = value

      (for {
        loc0 <- v0.openedLoc
        mns0 = loc0.getLabel
        lens = MNodeState.tfInfoWide
        if !lens.get(mns0)
      } yield {
        val v2 = MTree.nodes.set {
          loc0
            .modifyLabel(lens set true)
            .toTree
        }(v0)
        updated(v2)
      })
        .getOrElse( noChange )


    // Сигнал о завершении запроса к серверу на тему редактирования тарифа размещения.
    case m: TfDailySavedResp =>
      val v0 = value

      (for {
        loc0 <- v0.openedLoc
        // Ошибка должна быть отработана EditTfDailyAh:
        nodeInfo2 <- m.tryResp.toOption
      } yield {
        val v2 = MTree.nodes.set {
          loc0
            .modifyLabel( MNodeState.infoPot.modify(_.ready(nodeInfo2)) )
            .toTree
        }(v0)

        updated(v2)
      })
        .getOrElse( noChange )


    // Реакция на изменение галочки showOpened напротив узла.
    case m: AdvShowOpenedChange =>
      val conf = confRO()

      // adId не задан в конфиге ad-режима формы. Should never happen.
      if (conf.adIdOpt.isEmpty)
        logger.error( ErrorMsgs.AD_ID_IS_EMPTY, msg = (m, conf) )

      (for {
        adId <- conf.adIdOpt
        v0 = value
        locOpt0 = v0.openedLoc
        loc0 <- {
          if (locOpt0.isEmpty)
            logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          locOpt0
        }
        mns0 = loc0.getLabel
        if {
          val r = mns0.adv.isEmpty
          // Нельзя тыкать галочку, когда уже идёт обновление состояния на сервере.
          if (!r)
            logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, mns0) )
          r
        }
        openedPath <- v0.opened
      } yield {
        // Всё ок, можно обновлять текущий узел и запускать реквест на сервер.
        // Организовать реквест на сервер.
        val fx = Effect {
          api
            .setAdvShowOpened(
              adId          = adId,
              isShowOpened  = m.isChecked,
              onNode        = loc0.rcvrKey,
            )
            .transform { tryResp =>
              Success( AdvShowOpenedChangeResp(openedPath, m, tryResp) )
            }
        }

        // Обновить состояние формы.
        val v2 = MTree.nodes.set {
          loc0
            .modifyLabel {
              MNodeState.adv.set(
                Some( MNodeAdvState(
                  isShowOpenedPot = Pot
                    .empty[Boolean]
                    .ready(m.isChecked)
                    .pending()
                ))
              )
            }
            .toTree
        }(v0)

        updated(v2, fx)
      })
        .getOrElse(noChange)


    // Ответ сервера по запросу обновления галочки раскрытого
    case m: AdvShowOpenedChangeResp =>
      val v0 = value

      (for {
        loc0 <- v0.nodes
          .loc
          .pathToNode( m.nodePath )
      } yield {
        val v2 = MTree.nodes.set {
          loc0
            .modifyLabel {
              m.tryResp.fold (
                // При ошибке запроса: сохранить ошибку в состояние
                {ex =>
                  MNodeState.adv
                    .composeTraversal( Traversal.fromTraverse[Option, MNodeAdvState] )
                    .composeLens( MNodeAdvState.isShowOpenedPot )
                    .set( Pot.empty[Boolean].fail(ex) )
                },
                // Если всё ок, то обновить состояние текущего узла.
                {_ =>
                  MNodeState.adv.set(None) andThen
                  MNodeState.infoPot.modify { _.map {
                    MLknNode.adv
                      .composeTraversal( Traversal.fromTraverse[Option, MLknAdv] )
                      .composeLens( MLknAdv.advShowOpened )
                      .set( m.reason.isChecked )
                  }}
                }
              )
            }
            .toTree
        }(v0)

        updated(v2)
      })
        .getOrElse( noChange )


    // Реакция на изменение галочки showOpened напротив узла.
    case m: AlwaysOutlinedSet =>
      val conf = confRO()
      // adId не задан в конфиге формы. Should never happen.
      if (conf.adIdOpt.isEmpty)
        logger.warn( ErrorMsgs.AD_ID_IS_EMPTY, msg = (m, conf) )

      (for {
        adId <- conf.adIdOpt
        v0 = value
        locOpt0 = v0.openedLoc
        loc0 <- {
          if (locOpt0.isEmpty)
            logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          locOpt0
        }
        mns0 = loc0.getLabel
        // Нельзя тыкать галочку, когда уже идёт обновление состояния на сервере.
        if {
          val r = mns0.adv.isEmpty
          if (!r)
            logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, mns0) )
          r
        }
        opened <- v0.opened
      } yield {
        // Организовать реквест на сервер.
        val fx = Effect {
          api
            .setAlwaysOutlined(
              adId              = adId,
              isAlwaysOutlined  = m.isChecked,
              onNode            = loc0.rcvrKey,
            )
            .transform { tryResp =>
              Success( AlwaysOutlinedResp(opened, m, tryResp) )
            }
        }

        val v2 = MTree.nodes.set {
          loc0
            .modifyLabel {
              val pot2 = Pot
                .empty[Boolean]
                .ready(m.isChecked)
                .pending()
              MNodeState.adv.modify { advOpt0 =>
                val adv1 = advOpt0.fold {
                  MNodeAdvState(alwaysOutlinedPot = pot2)
                } {
                  MNodeAdvState.alwaysOutlined set pot2
                }
                Some( adv1 )
              }
            }
            .toTree
        }(v0)

        updated(v2, fx)
      })
        .getOrElse( noChange )


    // Ответ сервера по запросу обновления галочки раскрытого
    case m: AlwaysOutlinedResp =>
      val v0 = value

      (for {
        loc0 <- v0.nodes
          .loc
          .pathToNode( m.nodePath )
      } yield {
        val v2 = MTree.nodes.set {
          loc0
            .modifyLabel {
              m.tryResp.fold(
                // При ошибке запроса: сохранить ошибку в состояние
                {ex =>
                  MNodeState.adv
                    .composeTraversal( Traversal.fromTraverse[Option, MNodeAdvState] )
                    .composeLens( MNodeAdvState.alwaysOutlined )
                    .set( Pot.empty[Boolean].fail(ex) )
                },
                // Если всё ок, то обновить состояние текущего узла.
                {_ =>
                  MNodeState.adv.set(None) andThen
                    MNodeState.infoPot.modify( _.map(
                      MLknNode.adv
                        .composeTraversal( Traversal.fromTraverse[Option, MLknAdv] )
                        .composeLens( MLknAdv.alwaysOutlined )
                        .set( m.reason.isChecked )
                    ))
                }
              )
            }
            .toTree
        }(v0)
        updated(v2)
      })
        .getOrElse( noChange )

  }

}
