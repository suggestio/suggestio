package io.suggest.lk.nodes.form.a.tree

import diode._
import diode.data.Pot
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.lk.nodes.{MLknConf, MLknModifyQs}
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.log.Log
import io.suggest.primo.Keep
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.sjs.dom2.DomQuick
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import scalaz.{EphemeralStream, Tree}
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.spa.DoNothing

import scala.collection.immutable.HashMap
import scala.util.{Success, Try}

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
    case m: ModifyNode =>
      val v0 = value

      val nodePathOpt = m.nodePath
        .orElse( v0.opened )

      // adId.isEmpty не проверяем: любые режимы формы поддерживаются.
      (for {
        nodePath <- nodePathOpt
        locOpt0 = v0.pathToLoc( nodePath )
        loc0 <- {
          if (locOpt0.isEmpty)
            logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          locOpt0
        }
        treeId = loc0.getLabel
        mns0 <- v0.nodesMap.get( treeId )

        // Сверить rcvrKey, если задан. Обычно, он тут не задан, ибо Pot.empty и операция синхронная.
        rcvrKey = v0.nodesMap
          .mnsPath(loc0)
          .rcvrKey
        if m.nodeRk.isEmpty || (m.nodeRk contains[RcvrKey] rcvrKey)

        // Далее - расхождение логики в зависимости от состояния m.nextPot.
        res <- {
          if (m.nextPot ==* Pot.empty) {
            // Шаг 1: собрать и запустить запрос изменения настройки на сервер:
            val nodeOption0 = mns0.optionMods.getOrElse( m.key, Pot.empty )

            if ( nodeOption0.isPending ) {
              // Другой запрос уже запущен. Ждём таймаута.
              logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, mns0) )
              None

            } else if (
              // Новое значение флага уже не новое?
              mns0.infoPot
                .exists(
                  _.options.get(m.key) contains m.value
                )
            ) {
              // Ничего менять не требуется: в узле уже сохранено текущее состояние флага.
              logger.log( ErrorMsgs.INACTUAL_NOTIFICATION, msg = (m, mns0.infoPot) )
              None

            } else {
              val pot2 = Pot.empty
                .ready( m.value )
                .pending()

              // Всё ок, можно обновлять текущий узел и запускать реквест на сервер.
              val fx = Effect {
                // реквест на сервер:
                api
                  .modifyNode(
                    MLknModifyQs(
                      onNodeRk  = rcvrKey,
                      adIdOpt   = confRO().adIdOpt,
                      opKey     = m.key,
                      opValue   = m.value,
                    )
                  )
                  .transform { tryResp =>
                    val res = m.copy(
                      nodeRk  = Some( rcvrKey ),
                      nextPot = m.nextPot withTry tryResp,
                    )
                    Success( res )
                  }
              }

              val mns1 = MNodeState.optionMods
                .modify( _ + (m.key -> pot2) )(mns0)

              // Залить в карту узлов обновление состояния узла:
              var modF = MTree.nodesMap.modify(_ + (treeId -> mns1))

              // Если false=>true, то нужно сфокусироваться на данном узле: чтобы галочки потом раскрылись.
              if (m.value.bool.getOrElseFalse && !(v0.opened contains nodePath))
                modF = modF andThen (MTree.opened set nodePathOpt)

              val v2 = modF(v0)
              Some( updated(v2, fx) )
            }

          } else if (m.nextPot.isReady) {
            // Шаг 2: Отработать завершение запроса запроса изменения опции.
            // Сервер сохранил ок, и прислал обновлённый узел. Обновить состояние.
            val mns2 = (
              (MNodeState.infoPot set m.nextPot) andThen
              (MNodeState.optionMods.modify(_ - m.key))
            )(mns0)
            val v2 = MTree.nodesMap.modify(_ + (treeId -> mns2))(v0)
            Some( updated(v2) )

          } else (for {
            ex <- m.nextPot.exceptionOption
            mod0 <- mns0.optionMods.get( m.key )
          } yield {
            // Сбрасываем хранимое внутри значение, чтобы визуально откатиться на текущее значение сервера.
            val mod2 = mod0.emptyPot.fail(ex)
            val mns2 = (
              (MNodeState.optionMods.modify(_ + (m.key -> mod2)))
            )(mns0)
            val v2 = MTree.nodesMap.modify(_ + (treeId -> mns2))(v0)
            updated(v2)
          })
        }

      } yield res)
        .getOrElse( noChange )


    // Положительный ответ сервера по поводу добавления нового узла.
    case m: CreateNodeResp =>
      val v0 = value
      (for {
        idsTree0 <- v0.idsTreeOpt
        loc0 <- idsTree0.loc.pathToNode( m.parentPath )
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
              .flatMap { loc1 =>
                // Успешно удалён некорневой узел дерева.
                // Вероятно, узел может быть в дереве объявлен где-то ещё, поэтому зачищаем всё оставшееся дерево целиком:
                loc1
                  .toTree
                  .filter { currTreeId =>
                    currTreeId !=* m.nodeId
                  }
              }
              // Если None: В корне пирамиды находится root-узел или юзер. Юзер удалить сам себя он наверное не может...
              .fold {
                // Но если смог, то надо перезагружать страницу:
                val fx = Effect.action {
                  DomQuick.reloadPage()
                  DoNothing
                }
                effectOnly(fx)
              } { tree2 =>
                val v2 = v0.copy(
                  idsTree   = v0.idsTree.ready( tree2 ),
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
        m.treePot.toTry.fold(
          {ex =>
            logger.error( ErrorMsgs.INIT_FLOW_UNEXPECTED, ex, m )
            val v2 = MTree.idsTree.modify( _.fail(ex) )(v0)
            updated(v2)
          },
          {resp =>
            val rootNodeId = MTreeRoles.Root.treeId

            val appendedTree = EphemeralStream.cons(
              MNodeState.respTreeToIdsTree( resp.subTree ),
              EphemeralStream.emptyEphemeralStream
            )

            val rootSubForestKeepOpt = v0.idsTree
              .toOption
              .map { idsTree0 =>
                // Нелениво взять из старого леса только возможную подгруппу видимых маячков:
                val bcnsGroupId = MTreeRoles.BeaconsDetected.treeId
                idsTree0
                  .subForest
                  .iterator
                  .filter( _.rootLabel ==* bcnsGroupId )
                  .toList
                  .toEphemeralStream
              }
              .filterNot( _.isEmpty )

            val rootAndKeepSubForests = rootSubForestKeepOpt
              .fold( appendedTree ) { _ ++ appendedTree }

            // Залить начальное дерево:
            val idsTree2 = Tree.Node[String](
              root   = rootNodeId,
              forest = rootAndKeepSubForests,
            )

            val appendNodes = resp.subTree
              .map { m =>
                m.id -> MNodeState.fromRespNode( m )
              }
              .flatten
              .iterator
              .to( HashMap )

            // Есть соблазн задействовать старую карту узлов, но изменение режима формы означает, что
            // старая инфа об узлах неверна в новом режиме (не содержит корректных значений Adv или ADN-полей).
            // Поэтому, начинаем с чистого листа, и недостающиую инфу по текущим видимым маячкам надо будет перезапросить с сервера:
            var nodesMap2 = v0.nodesMap.empty +
              (rootNodeId -> v0.nodesMap.getOrElse(rootNodeId, MNodeState.mkRootNode))
            // Промигрировать другие нужные узлы из старой карты:
            for {
              keepedTreeIds <- rootSubForestKeepOpt
              keepedTreeIdTree <- keepedTreeIds.iterator
              keepedTreeId <- keepedTreeIdTree.flatten.iterator
              mns0 <- v0.nodesMap.get( keepedTreeId )
            } {
              // Выставить отрицательный pending, чтобы в BeaconsAh обнаружилась необходимость перекачать инфу по маячку:
              val mns2 = if (mns0.role ==* MTreeRoles.BeaconSignal)
                MNodeState.infoPot.modify( _.pending(BeaconsAh.PENDING_VALUE_NEED_REGET) )(mns0)
              else
                mns0
              nodesMap2 = nodesMap2 + (keepedTreeId -> mns2)
            }
            // Добавить в карту узлы с сервера:
            if (appendNodes.nonEmpty)
              nodesMap2 = nodesMap2.merged( appendNodes )(Keep.right)

            val v1 = (
              MTree.setNodes( idsTree2 ) andThen
              MTree.nodesMap.set( nodesMap2 )
            )(v0)

            // После очистки могли появиться неизвестные видимые bluetooth-маячки, и нужно подумать над организацией beacon-scan-запроса на сервер:
            val bcnScanFx = BeaconsDetected( Map.empty ).toEffectPure

            // opened: надо по возможности выставить в текущее значение (если оно актуально),
            // либо в conf.nodeId, либо в корневой узел, если он есть.
            val v2 = v1
              .openedLoc
              // TODO Если loc пустой, попытаться максимально приблизится к нему.
              //      Понадобится при перезагрузке дерева, когда глубокие под-узлы могут быть недоступны.
              .fold {
                // Текущий путь не актуален после сброса дерева: возможно, conf.nodeId содержит искомый узел.
                (for {
                  confNodeId <- confRO.value.onNodeId
                  loc0 <- Try {
                    idsTree2
                      .loc
                      .map( nodesMap2.apply )
                      .find { loc =>
                        loc
                          .getLabel
                          .infoPot
                          .exists(_.id ==* confNodeId)
                      }
                  }
                    .toOption
                    .flatten
                } yield {
                  MTree.opened set Some(loc0.toNodePath)
                })
                  .orElse {
                    // Выбрать корневой узел.
                    val modF = MTree.opened set Some(idsTree2.loc.toNodePath)
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

            updated( v2, bcnScanFx )
          }
        )
      }

  }

}
