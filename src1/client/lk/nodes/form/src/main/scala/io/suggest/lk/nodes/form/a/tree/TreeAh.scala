package io.suggest.lk.nodes.form.a.tree

import diode._
import diode.data.{Pending, Pot}
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.lk.nodes.MLknConf
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.log.Log
import io.suggest.primo.Keep
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.sjs.dom2.DomQuick
import monocle.Traversal
import scalaz.std.option._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import scalaz.{EphemeralStream, Tree}
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.spa.DoNothing

import scala.collection.immutable.HashMap
import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 21:50
  * Description: Контроллер управления деревом узлов.
  */
class TreeAh[M](
                 api          : ILkNodesApi,
                 modelRW      : ModelRW[M, MTree],
                 confRO       : ModelRO[MLknConf],
               )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал о необходимости показать какой-то узел подробнее.
    case m: NodeClick =>
      val v0 = value

      // Загрузка ветви инициализированного дерева. Найти начало ветви в текущем дереве:
      val locOpt0 = v0.pathToLoc( m.nodePath )
      if (locOpt0.isEmpty)
        logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )

      (for {
        loc0 <- locOpt0
        nodeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( nodeId )
      } yield {
        val isNormal = mns0.role ==* MTreeRoles.Normal
        if ( !isNormal || mns0.infoPot.exists(_.isDetailed.getOrElseTrue) ) {
          // Дочерние элементы уже получены с сервера. Даже если их нет. Сфокусироваться на текущий узел либо свернуть его.
          // Узнать, является ли текущий элемент сфокусированным?
          val v2 = MTree.opened.modify { opened0 =>
            // Сворачивать можно текущий элемент, но свернуть можно и родительский элемент, и фокус должен перейти
            // к пути, который родительский по отношению к сворачиваемому.
            val isCollapse = opened0.exists { openedNodePath =>
              openedNodePath.tails contains m.nodePath
            }
            if (isCollapse) {
              // Клик по заголовку текущего узла. Свернуть текущий узел, но оставив родительский элемент раскрытым.
              Option.when( m.nodePath.nonEmpty ) {
                // Убрать последний элемент из пути до текущего узла:
                m.nodePath.slice(0, m.nodePath.length - 1)
              }
            } else {
              Some( m.nodePath )
            }
          }(v0)
          updated(v2)

        } else if (isNormal) {
          // !isDetailed: Для текущего узла не загружено children. Запустить в фоне загрузку, выставить её в состояние.
          val tstampMs = System.currentTimeMillis()

          val mnsPath = v0.nodesMap
            .mnsPath( loc0 )
            .to( LazyList )

          // Собрать эффект запроса к серверу за подробностями по узлу.
          val fx = Effect {
            val rcvrKey = mnsPath.rcvrKey
            api
              .subTree(
                onNodeRk = Some( rcvrKey ),
                adId = confRO.value.adIdOpt
              )
              .transform { tryRes =>
                Success( HandleSubNodesOf(m.nodePath, tryRes, tstampMs) )
              }
          }

          val mns2 = MNodeState.infoPot
            .modify(_.pending(tstampMs))( mnsPath.head )
          val v2 = MTree.nodesMap
            .modify(_ + (loc0.getLabel -> mns2))(v0)

          updated(v2, fx)

        } else {
          noChange
        }
      })
        .getOrElse( noChange )


    // Ответ сервера на тему под-узлов.
    case snr: HandleSubNodesOf =>
      val v0 = value

      // Не пустой nodePath. Найти и обновить под-ветвь:
      val loc0Opt = v0.pathToLoc( snr.nodePath )

      if (loc0Opt.isEmpty)
        logger.error( ErrorMsgs.NODE_PATH_MISSING_INVALID, msg = snr )

      (for {
        loc0 <- loc0Opt
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )
        if {
          val r = mns0.infoPot isPendingWithStartTime snr.tstampMs
          if (!r) logger.log( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = (snr, mns0.infoPot.state) )
          r
        }
      } yield {
        val (nodesAppend2, loc2) = snr.subNodesRespTry.fold(
          // Ошибка запроса. Сохранить её в состояние.
          {ex =>
            val mns1 = MNodeState.infoPot.modify( _.fail(ex) )(mns0)
            val nodesAppend1 = HashMap.empty + (treeId -> mns1)
            (nodesAppend1, loc0)
          },

          // Положительный ответ сервера, обновить данные по текущему узлу, замёржив поддерево в текущий loc.
          {resp =>
            // Самоконтроль: получено поддерево для правильного узла:
            require( mns0.infoPot.exists(_.id ==* resp.subTree.rootLabel.id) )

            val nodesAppend1 = resp.subTree
              // Маппим все узлы поддерева в MNodeState, хотя нужно отмаппить только subForest.
              // map ленив, поэтому текущий (корневой) узел поддерева будет перезаписан ниже без лишних вычислений.
              .map { lknNode =>
                val mns1 = MNodeState.fromRespNode(lknNode)
                lknNode.id -> mns1
              }
              .loc
              // Текущий узел дерева: патчим текущее состояние, сохраняя рантаймовые под-состояния нетронутыми.
              .setLabel {
                val lknNode = resp.subTree.rootLabel
                val mns1 = (MNodeState.infoPot.modify( _.ready(lknNode)) )(mns0)
                lknNode.id -> mns1
              }
              // Остальные под-узлы - берём, что сервер прислал.
              .toTree
              // Сворачием всё дерево в HashMap:
              .flatten
              .iterator
              .to( HashMap )

            // собрать поддерево MNodeState на основе текущего состояния и полученного с сервера поддерева.
            val loc1 = loc0.setTree(
              MNodeState.respTreeToIdsTree( resp.subTree )
            )

            (nodesAppend1, loc1)
          }
        )

        val v2 = v0.copy(
          idsTree  = v0.idsTree ready loc2.toTree,
          opened = Some( snr.nodePath ),
          nodesMap = v0.nodesMap.merged( nodesAppend2 )( Keep.right ),
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
        locOpt0 = v0.pathToLoc( m.nodePath )
        loc0 <- {
          if (locOpt0.isEmpty)
            logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          locOpt0
        }
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )
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
              onNode    = v0.nodesMap.mnsPath(loc0).rcvrKey,
            )
            .transform { tryResp =>
              Success( AdvOnNodeResp(m.nodePath, tryResp) )
            }
        }

        val mns1 = MNodeState.adv.set(
          Some( MNodeAdvState(
            newIsEnabledPot = Pot.empty[Boolean]
              .ready(m.isEnabled)
              .pending()
          ))
        )(mns0)
        // Залить в карту узлов обновление состояния узла:
        var modF = MTree.nodesMap.modify(_ + (treeId -> mns1))

        // Если false=>true, то нужно сфокусироваться на данном узле: чтобы галочки потом раскрылись.
        if (m.isEnabled && !(v0.opened contains m.nodePath))
          modF = modF andThen (MTree.opened set Some(m.nodePath))

        val v2 = modF(v0)
        updated(v2, fx)
      })
        .getOrElse( noChange )


    // Сигнал завершения реквеста изменения состояния размещения карточки на узле.
    case m: AdvOnNodeResp =>
      val v0 = value
      val locOpt0 = v0.pathToLoc( m.nodePath )

      if (locOpt0.isEmpty)
        logger.warn( ErrorMsgs.NODE_PATH_MISSING_INVALID, msg = m )

      (for {
        loc0 <- locOpt0
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )
      } yield {
        // Пропатчить инфу по узлу необходимыми данными:
        val mns1 = m.tryResp.fold(
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
        )(mns0)

        val v2 = MTree.nodesMap.modify(_ + (treeId -> mns1))(v0)
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
        newChild = MNodeState.respTreeToIdsTree( Tree.Leaf(resp) )
      } yield {
        // Залить обновления в дерево, а CreateNodeAh удалит addState для текущего rcvrKey.
        val v2 = MTree.setNodes {
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
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )
        info <- mns0.infoPot.toOption
        if {
          val r = info.isAdmin contains[Boolean] true
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
            .setNodeEnabled( info.id, m.isEnabled )
            .transform { tryRes =>
              val r = NodeIsEnabledUpdateResp(opened, info.id, tryRes)
              Success(r)
            }
        }

        val mns1 = MNodeState.isEnableUpd.set(
          Some( MNodeEnabledUpdateState(
            newIsEnabled  = m.isEnabled,
            request       = Pending()
          ) )
        )(mns0)

        val v2 = MTree.nodesMap.modify( _ + (treeId -> mns1) )(v0)
        updated(v2, fx)
      })
        .getOrElse( noChange )


    // Ответ сервер по поводу флага isEnabled для какого-то узла.
    case m: NodeIsEnabledUpdateResp =>
      val v0 = value

      (for {
        loc0 <- v0.pathToLoc( m.nodePath )
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )
        info <- mns0.infoPot.toOption
      } yield {
        val mns2 = m.resp.fold(
          // При ошибке запроса: сохранить ошибку в состояние
          {ex =>
            MNodeState.isEnableUpd
              .composeTraversal( Traversal.fromTraverse[Option, MNodeEnabledUpdateState] )
              .modify { nodeEnabledState =>
                nodeEnabledState.copy(
                  newIsEnabled  = info.isEnabled contains[Boolean] true,
                  request       = nodeEnabledState.request.fail(ex)
                )
              }
          },

          // Если всё ок, то обновить состояние текущего узла.
          {newNodeInfo =>
            MNodeState.infoPot.modify(_.ready( newNodeInfo )) andThen
            MNodeState.isEnableUpd.set( None )
          }
        )(mns0)

        val v2 = MTree.nodesMap.modify(_ + (treeId -> mns2))(v0)
        updated(v2)
      })
        .getOrElse( noChange )


    // Сигнал о завершении запроса к серверу по поводу удаления узла.
    case m: NodeDeleteResp =>
      val v0 = value

      (for {
        loc0 <- v0.pathToLoc( m.nodePath )
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )
      } yield {
        m.resp.fold(
          {ex =>
            val mns2 = MNodeState.infoPot.modify(_.fail(ex))(mns0)
            val v2 = MTree.nodesMap.modify(_ + (treeId -> mns2))(v0)
            updated(v2)
          },
          {_ =>
            loc0
              .delete
              // В корне пирамиды находится юзер. Удалить сам себя он наверное не может...
              .fold {
                // Но если смог, то надо перезагружать страницу:
                val fx = Effect.action {
                  DomQuick.reloadPage()
                  DoNothing
                }
                effectOnly(fx)
              } { loc1 =>
                // Успешно удалён некорневой узел дерева. Подчистить состояние:
                val v2 = v0.copy(
                  idsTree     = v0.idsTree.ready( loc1.toTree ),
                  nodesMap  = v0.nodesMap -- loc0.tree.flatten.iterator,
                  opened    = for {
                    path0 <- v0.opened
                    path0Len = path0.length
                    if path0Len > 1
                  } yield {
                    path0.slice( 0, path0Len - 1 )
                  }
                )
                updated(v2)
              }
          }
        )
      })
        .getOrElse( noChange )


    // Сигнал завершения запроса сохранения с сервера.
    case m: NodeEditSaveResp =>
      val v0 = value

      // Нужно найти узел, который обновился. Теоретически возможно, что это не текущий узел, хоть и маловероятно.
      def findF(treeId: String): Option[(String, MNodeState)] = {
        for {
          mns <- v0.nodesMap.get( treeId )
          info <- mns.infoPot.toOption
          if info.id ==* m.nodeId
        } yield {
          treeId -> mns
        }
      }

      (for {
        (treeId, mns0) <- v0.openedLoc
          .flatMap { m =>
            findF( m.getLabel )
          }
          .orElse {
            logger.log( ErrorMsgs.NODE_PATH_MISSING_INVALID, msg = (m, v0.opened) )
            v0.idsTree
              .toOption
              .flatMap( _.loc.tree.flatten.iterator.flatMap( findF ).nextOption() )
          }
        nodeInfo2 <- m.tryResp.toOption
      } yield {
        val mns2 = MNodeState.infoPot.modify( _.ready(nodeInfo2) )(mns0)
        val v2 = MTree.nodesMap.modify(_ + (treeId -> mns2))(v0)
        updated(v2)
      })
        .getOrElse( noChange )


    // Юзер почитать развернуть подробности по тарифу.
    case TfDailyShowDetails =>
      val v0 = value

      (for {
        loc0 <- v0.openedLoc
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )
        lens = MNodeState.tfInfoWide
        if !lens.get(mns0)
      } yield {
        val mns2 = (lens set true)(mns0)
        val v2 = MTree.nodesMap.modify(_ + (treeId -> mns2))(v0)
        updated(v2)
      })
        .getOrElse( noChange )


    // Сигнал о завершении запроса к серверу на тему редактирования тарифа размещения.
    case m: TfDailySavedResp =>
      val v0 = value

      (for {
        loc0 <- v0.openedLoc
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )
        // Ошибка должна быть отработана EditTfDailyAh:
        nodeInfo2 <- m.tryResp.toOption
      } yield {
        val mns2 = MNodeState.infoPot.modify(_.ready(nodeInfo2))(mns0)
        val v2 = MTree.nodesMap.modify(_ + (treeId -> mns2))(v0)

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
        locOpt0 = v0.pathToLoc( m.nodePath )
        loc0 <- {
          if (locOpt0.isEmpty)
            logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          locOpt0
        }
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )
        if {
          val r = mns0.adv.isEmpty
          // Нельзя тыкать галочку, когда уже идёт обновление состояния на сервере.
          if (!r)
            logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, mns0) )
          r
        }
        openedPath <- v0.opened
        info <- mns0.infoPot.toOption
      } yield {
        // Всё ок, можно обновлять текущий узел и запускать реквест на сервер.
        // Организовать реквест на сервер.
        val fx = Effect {
          api
            .setAdvShowOpened(
              adId          = adId,
              isShowOpened  = m.isChecked,
              onNode        = v0.nodesMap.mnsPath(loc0).rcvrKey,
            )
            .transform { tryResp =>
              Success( AdvShowOpenedChangeResp(openedPath, m, tryResp) )
            }
        }

        val mns2 = MNodeState.adv.modify { advOpt0 =>
          val adv0 = advOpt0 getOrElse MNodeAdvState.from( info.adv )
          val adv2 = MNodeAdvState.isShowOpenedPot.modify( _.ready(m.isChecked).pending() )(adv0)
          Some(adv2)
        }(mns0)

        // Обновить состояние формы.
        val v2 = MTree.nodesMap.modify(_ + (treeId -> mns2))(v0)
        updated(v2, fx)
      })
        .getOrElse(noChange)


    // Ответ сервера по запросу обновления галочки раскрытого
    case m: AdvShowOpenedChangeResp =>
      val v0 = value

      (for {
        loc0 <- v0.pathToLoc( m.nodePath )
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )
      } yield {
        val mns2 = m.tryResp.fold (
          // При ошибке запроса: сохранить ошибку в состояние
          {ex =>
            MNodeState.adv
              .composeTraversal( Traversal.fromTraverse[Option, MNodeAdvState] )
              .composeLens( MNodeAdvState.isShowOpenedPot )
              .set( Pot.empty[Boolean].fail(ex) )
          },
          // Если всё ок, то обновить состояние текущего узла.
          {mLknNode2 =>
            MNodeState.adv.set(None) andThen
            MNodeState.infoPot.modify( _.ready(mLknNode2) )
          }
        )(mns0)

        val v2 = MTree.nodesMap.modify(_ + (treeId -> mns2))(v0)
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
        locOpt0 = v0.pathToLoc( m.nodePath )
        loc0 <- {
          if (locOpt0.isEmpty)
            logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          locOpt0
        }
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )
        // Нельзя тыкать галочку, когда уже идёт обновление состояния на сервере.
        if {
          val r = mns0.adv.isEmpty
          if (!r)
            logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, mns0) )
          r
        }
        opened <- v0.opened
        info <- mns0.infoPot.toOption
      } yield {
        // Организовать реквест на сервер.
        val fx = Effect {
          api
            .setAlwaysOutlined(
              adId              = adId,
              isAlwaysOutlined  = m.isChecked,
              onNode            = v0.nodesMap.mnsPath(loc0).rcvrKey,
            )
            .transform { tryResp =>
              Success( AlwaysOutlinedResp(opened, m, tryResp) )
            }
        }

        val mns2 = MNodeState.adv.modify { advOpt0 =>
          val adv0 = advOpt0 getOrElse MNodeAdvState.from( info.adv )
          val adv2 = MNodeAdvState.alwaysOutlined.modify( _.ready(m.isChecked).pending() )(adv0)
          Some(adv2)
        }(mns0)

        val v2 = MTree.nodesMap.modify(_ + (treeId -> mns2))(v0)
        updated(v2, fx)
      })
        .getOrElse( noChange )


    // Ответ сервера по запросу обновления галочки раскрытого
    case m: AlwaysOutlinedResp =>
      val v0 = value

      (for {
        loc0 <- v0.pathToLoc( m.nodePath )
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )
      } yield {
        val mns2 = m.tryResp.fold(
          // При ошибке запроса: сохранить ошибку в состояние
          {ex =>
            MNodeState.adv
              .composeTraversal( Traversal.fromTraverse[Option, MNodeAdvState] )
              .composeLens( MNodeAdvState.alwaysOutlined )
              .set( Pot.empty[Boolean].fail(ex) )
          },
          // Если всё ок, то обновить состояние текущего узла.
          {node2 =>
            MNodeState.adv.set(None) andThen
            MNodeState.infoPot.modify( _.ready( node2 ) )
          }
        )(mns0)

        val v2 = MTree.nodesMap.modify(_ + (treeId -> mns2))(v0)
        updated(v2)
      })
        .getOrElse( noChange )


    // Сигнал запуска или завершения инициализации дерева.
    case m: TreeInit =>
      val v0 = value

      if (m.treePot ===* Pot.empty) {
        // Запрашивается инициализация дерева. Начать:
        // Начальная инициализация дерева от корня.
        val fx = Effect {
          api
            .subTree(
              adId = confRO.value.adIdOpt
            )
            .transform { tryRes =>
              val treePot2 = m.treePot.withTry(tryRes)
              Success( TreeInit( treePot2 ) )
            }
        }
        val v2 = MTree.idsTree.modify( _.pending() )(v0)
        updated(v2, fx)

      } else {
        // Ответ сервера на инициализацию
        val v2 = m.treePot.toTry.fold [MTree] (
          {ex =>
            MTree.idsTree.modify( _.fail(ex) )(v0)
          },
          {resp =>
            val rootNodeId = MTreeRoles.Root.treeId

            // Залить начальное дерево:
            val subTree2 = Tree.Node[String](
              root   = rootNodeId,
              forest = {
                val appendedTree = EphemeralStream.cons(
                  MNodeState.respTreeToIdsTree( resp.subTree ),
                  EphemeralStream.emptyEphemeralStream
                )

                // Предыдущее дерево: берём из текущего (как бы пустого) состояния, т.к. там могут быть результаты BeaconsDetected:
                v0.idsTree
                  .toOption
                  .map { nodesTree0 =>
                    // Удалить из старого дерева узлы текущего юзера, если они там были
                    def findF(idsTree: Tree[String]): Boolean = {
                      v0.nodesMap
                        .get( idsTree.rootLabel )
                        .exists( _.role ==* MTreeRoles.Normal )
                    }

                    val subForest0 = nodesTree0.subForest

                    if (subForest0.iterator exists findF)
                      subForest0.filter( !findF(_) )
                    else
                      subForest0
                  }
                  .filterNot( _.isEmpty )
                  .fold( appendedTree )( _ ++ appendedTree )
              }
            )

            val appendNodes = resp.subTree
              .map { m =>
                m.id -> MNodeState.fromRespNode( m )
              }
              .flatten
              .iterator
              .to( HashMap )

            // Т.к. обновляем дерево, то какие id осталвяем в карте nodesMap?
            val keepTreeIds = subTree2
              .flatten
              .iterator
              .toSet

            var nodesMap2 = v0.nodesMap
            val rmTreeIds = v0.nodesMap.keySet -- keepTreeIds
            if (rmTreeIds.nonEmpty)
              nodesMap2 = nodesMap2 -- rmTreeIds
            if (appendNodes.nonEmpty)
              nodesMap2 = nodesMap2.merged( appendNodes )(Keep.right)
            if (!(nodesMap2 contains rootNodeId))
              nodesMap2 = nodesMap2 + (rootNodeId -> MNodeState.mkRootNode)

            var v1 = (
              MTree.setNodes( subTree2 ) andThen
              MTree.nodesMap.set( nodesMap2 )
            )(v0)

            // opened: надо по возможности выставить в текущее значение (если оно актуально),
            // либо в conf.nodeId, либо в корневой узел, если он есть.
            v1.openedLoc
              // TODO Если loc пустой, попытаться максимально приблизится к нему.
              //      Понадобится при перезагрузке дерева, когда глубокие под-узлы могут быть недоступны.
              .fold {
                // Текущий путь не актуален после сброса дерева: возможно, conf.nodeId содержит искомый узел.
                (for {
                  confNodeId <- confRO.value.onNodeId
                  loc0 <- subTree2
                    .loc
                    .map( nodesMap2.apply )
                    .find { loc =>
                      loc
                        .getLabel
                        .infoPot
                        .exists(_.id ==* confNodeId)
                    }
                } yield {
                  MTree.opened set Some(loc0.toNodePath)
                })
                  .orElse {
                    // Выбрать корневой узел.
                    val modF = MTree.opened set Some(subTree2.loc.toNodePath)
                    Some(modF)
                  }
                  .orElse {
                    // Обнулить opened, если выставлен.
                    Option.when( v1.opened.nonEmpty ) {
                      (MTree.opened set None)
                    }
                  }
                  // Вернуть MTree
                  .fold(v1)( _(v1) )
              }(_ => v1)   // Оставить текущее значение opened, если opened-локация работает.
          }
        )
        updated(v2)
      }

  }

}
