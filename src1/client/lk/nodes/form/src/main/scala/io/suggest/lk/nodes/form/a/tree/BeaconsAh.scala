package io.suggest.lk.nodes.form.a.tree

import diode._
import diode.data.Pot
import io.suggest.ble.BeaconDetected
import io.suggest.lk.nodes.{MLknBeaconsScanReq, MLknConf}
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.primo.Keep
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.spa.DiodeUtil.Implicits._
import japgolly.univeq._
import scalaz.Tree

import scala.collection.immutable.HashMap
import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.09.2020 22:03
  * Description: Контроллер для работы с маячками.
  */
class BeaconsAh[M](
                    modelRW     : ModelRW[M, MTreeOuter],
                    confRO      : ModelRO[MLknConf],
                    lkNodesApi  : ILkNodesApi,
                  )
  extends ActionHandler(modelRW)
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Выставить в дерево обнаруженные поблизости маячки. Экшен исходит из выдачи.
    case m: BeaconsDetected =>
      val v0 = value

      val tree0 = v0.tree
        .nodesOpt
        .getOrElse {
          Tree.Leaf(
            root   = MNodeState.mkRoot,
          )
        }

      // В начало дерева надо добавить/обновить группу видимых маячков:
      val loc0 = tree0.loc

      // Найти текущую группу маячков в дереве:
      val bcnsGroupLocOpt0 = loc0
        .findChild( _.rootLabel.role ==* MTreeRoles.BeaconsDetected )

      // Дополнить группу маячков свеже-полученными данными:
      val (bcnsGroupLoc1, haveBcnIds) = bcnsGroupLocOpt0.fold {
        // Пока нет подсписка с маячками. Добавить в начало общего дерева:
        val bcnsSubTreeEmpty = Tree.Leaf(
          root = MNodeState(
            role = MTreeRoles.BeaconsDetected,
          ),
        )
        val loc1 = loc0.insertDownFirst( bcnsSubTreeEmpty )
        loc1 -> Set.empty[String]

      } { bcnsGroupLoc0 =>
        // Нужно пройти текущую группу, обновив инфу в уже отрендеренных маячках.
        lazy val bcnSignalNow = BeaconDetected.seenNowMs()
        val subForest2 = bcnsGroupLoc0
          .tree
          .subForest
          // Форсируем без lazy, чтобы выкинуть из памяти старые инстасы MBeaconSignal.
          .iterator
          .map { bcnTree0 =>
            val mns0 = bcnTree0.rootLabel
            val nodeIdOpt = mns0.nodeId
            (for {
              nodeId <- nodeIdOpt
              bcnSignalOpt2 = m.beacons.get( nodeId )
              bcnSignal2 <- bcnSignalOpt2 orElse mns0.beacon.map(_.data)
            } yield {
              val bcnState2 = MNodeBeaconState(
                data      = bcnSignal2,
                isVisible = bcnSignalOpt2.nonEmpty || bcnSignal2.detect.isStillVisibleNow( bcnSignalNow ),
              )
              val bcnSubForest = bcnTree0.subForest
              val bcnLabel2 = ( MNodeState.beacon set Some(bcnState2) )( bcnTree0.rootLabel )
              Tree.Node( bcnLabel2, bcnSubForest ) -> nodeIdOpt
            })
              .getOrElse( bcnTree0 -> None )
          }
          .toList

        val rootLbl = bcnsGroupLoc0.getLabel
        val loc1 = bcnsGroupLoc0.setTree {
          Tree.Node( rootLbl, subForest2.map(_._1).toEphemeralStream )
        }

        // Собираем id уже пройденных маячков:
        val existingBcnIds = subForest2
          .iterator
          .flatMap(_._2)
          .toSet

        loc1 -> existingBcnIds
      }

      // Нужно определить новые маячки, добавив их в хвост subForest.
      val appendBcns =
        if (haveBcnIds.isEmpty) m.beacons
        else m.beacons.filterKeys( !haveBcnIds.contains(_) )


      val bcnsGroupLoc2 = if (appendBcns.isEmpty) {
        // Не требуется добавлять новых маячков в список:
        bcnsGroupLoc1

      } else {
        // Добавляем неизвестные ранее маячки в список:
        val tree1 = Tree.Node(
          root = bcnsGroupLoc1.getLabel,
          forest = bcnsGroupLoc1.tree.subForest ++ appendBcns
            .valuesIterator
            .map { bcnSignal =>
              Tree.Leaf {
                MNodeState(
                  role = MTreeRoles.BeaconSignal,
                  beacon = Some( MNodeBeaconState(bcnSignal) ),
                  // Порыться в кэше на предмет серверной инфы по маячку:
                  infoPot = Pot.fromOption {
                    bcnSignal.detect.signal.beaconUid
                      .flatMap( v0.beacons.cacheMap.get )
                      .flatMap( _.nodeScanResp )
                  },
                )
              }
            }
            .to( LazyList )
            .toEphemeralStream,
        )
        bcnsGroupLoc1.setTree( tree1 )
      }

      var modStateF = MTreeOuter.tree.modify {
        MTree.setNodes( bcnsGroupLoc2.toTree )
      }

      // TODO Если исчез opened-маячок, то обновить opened на валидное значение.
      // TODO Что делать, если одновременно исчезли все маячки? Удалять группу маячков из списка? Или просто сворачивать её?

      var fxAcc = List.empty[Effect]

      if (!v0.beacons.scanReq.isPending) {
        // Если есть маячки для запроса с сервера, то запросить инфу по ним.
        val unknownBcnIds = (for {
          bcnTree <- bcnsGroupLoc2.tree.subForest.iterator
          mns = bcnTree.rootLabel
          if mns.infoPot.isEmpty
          beaconState <- mns.beacon
          beaconUid <- beaconState.data.detect.signal.beaconUid
        } yield {
          beaconUid
        })
          .toSet

        if (unknownBcnIds.nonEmpty) {
          // Есть неизвестные маячки: организовать scan-запрос на сервер:
          val scanReqFx = Effect {
            val reqArgs = MLknBeaconsScanReq(
              beaconUids = unknownBcnIds,
              adId       = confRO.value.adIdOpt,
            )
            lkNodesApi
              .beaconsScan( reqArgs )
              .transform { tryResp =>
                val action = BeaconsScanResp(
                  reqArgs = reqArgs,
                  tryResp = tryResp,
                )
                Success( action )
              }
          }
          fxAcc ::= scanReqFx

          modStateF = modStateF andThen MTreeOuter.beacons
            .composeLens( MBeaconScan.scanReq )
            .modify( _.pending() )
        }
      }

      // Сохранить обновлённое состояние:
      val v2 = modStateF(v0)
      ah.updatedMaybeEffect(v2, fxAcc.mergeEffects)


    // Отработка результата запроса на сервер с маячками.
    case m: BeaconsScanResp =>
      val v0 = value

      m.tryResp.fold(
        // Запрос не удался.
        {ex =>
          val v2 = MTreeOuter.beacons
            .composeLens( MBeaconScan.scanReq )
            .modify(_.fail(ex))(v0)
          // TODO sc: эффект check-connectivity в выдачу?
          updated(v2)
        },

        // Ответ получен - разобраться.
        {resp =>
          // Найденные маячки:
          val now = System.currentTimeMillis()
          var cachedMapAppend = resp.subTree
            .subForest
            .map { nodeResp =>
              val lknNode = nodeResp.rootLabel
              val cacheEntry = MBeaconCachedEntry(
                nodeScanResp = Some( lknNode ),
                fetchedAtMs = now,
              )
              lknNode.id -> cacheEntry
            }
            // Собираем промежуточную карту, т.к. keySet нужен на следующем шаге:
            .iterator
            .to( HashMap )

          // Определить НЕнайденные на сервере маячки, чтобы инфу о них тоже закэшировать:
          val notFoundBcnIds = m.reqArgs.beaconUids diff cachedMapAppend.keySet
          if (notFoundBcnIds.nonEmpty) {
            val notFoundValue = MBeaconCachedEntry(
              fetchedAtMs = now,
            )
            val notFoundMap = (for {
              notFoundBcnId <- notFoundBcnIds.iterator
            } yield {
              // Есть неизвестные для сервера маячки. Закэшировать:
              notFoundBcnId -> notFoundValue
            })
              .to( HashMap )

            cachedMapAppend = cachedMapAppend.merged( notFoundMap )( Keep.right )
          }

          // Залить новую карту маячков в состояние:
          val v2 = MTreeOuter.beacons.modify(
            MBeaconScan.cacheMap.modify( _.merged(cachedMapAppend)(Keep.right) ) andThen
            MBeaconScan.scanReq.modify(_.ready(resp))
          )(v0)

          updated(v2)
        }
      )


    // Обновить кэш после любого редактирования узла-маячка, имеющегося в кэш-карте.
    case m: INodeUpdatedResp =>
      (for {
        nodeId <- m.nodeIdOpt
        nodeUpdatedOpt <- m.nodeUpdated.toOption
        v0 = value
        cachedEntry0 <- v0.beacons.cacheMap.get( nodeId )
      } yield {
        // Заменить beacon в карте кэша на полученный ответ сервера.
        val cachedEntry2 = (MBeaconCachedEntry.nodeScanResp set nodeUpdatedOpt)( cachedEntry0 )
        val v2 = MTreeOuter.beacons
          .composeLens( MBeaconScan.cacheMap )
          .modify(_ + (nodeId -> cachedEntry2))(v0)
        updated(v2)
      })
        .getOrElse( noChange )

  }

}
