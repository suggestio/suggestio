package io.suggest.lk.nodes.form.a.tree

import diode._
import diode.data.Pot
import io.suggest.lk.nodes.MLknBeaconsScanReq
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

      // Сборка маячков без lazy/z.Need, т.к. анализ собранного списка будет ниже.
      val beaconsForest = m.beacons
        .iterator
        .map { bcn =>
          Tree.Leaf(
            MNodeState(
              role = MTreeRoles.BeaconSignal,
              bcnSignal = Some(bcn),
              // Порыться в кэше на предмет серверной инфы по маячку:
              infoPot = Pot.fromOption {
                v0.beacons.cacheMap
                  .get( bcn.id )
                  .flatMap( _.nodeScanResp )
              },
            )
          )
        }
        .to( List )

      // Сборка поддерева beacons:
      val beaconsSubTree = Tree.Node(
        root = MNodeState(
          role = MTreeRoles.BeaconsDetected,
        ),
        forest = beaconsForest.toEphemeralStream,
      )

      // Обновление дерева:
      val beaconUpdatedLoc = loc0
        .findChild( _.rootLabel.role ==* MTreeRoles.BeaconsDetected )
        .fold {
          // Пока нет подсписка с маячками. Добавить в начало:
          loc0.insertDownFirst( beaconsSubTree )
        } { loc1 =>
          // Заменить текущий элемент обновлённым поддеревом.
          loc1.setTree( beaconsSubTree )
        }

      var modStateF = MTreeOuter.tree.modify {
        MTree.setNodes( beaconUpdatedLoc.toTree )
      }

      // TODO Если исчез opened-маячок, то обновить opened на валидное значение.
      // TODO Что делать, если одновременно исчезли все маячки? Удалять группу маячков из списка? Или просто сворачивать её?

      var fxAcc = List.empty[Effect]

      if (!v0.beacons.scanReq.isPending) {
        // Если есть маячки для запроса с сервера, то запросить инфу по ним.
        val unknownBcnIds = (for {
          bcnTree <- beaconsForest.iterator
          bcn = bcnTree.rootLabel
          if bcn.infoPot.isEmpty
          bcnSignal <- bcn.bcnSignal
        } yield {
          bcnSignal.id
        })
          .toSet

        if (unknownBcnIds.nonEmpty) {
          // Есть неизвестные маячки: организовать scan-запрос на сервер:
          val scanReqFx = Effect {
            val reqArgs = MLknBeaconsScanReq(
              beaconUids = unknownBcnIds,
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
