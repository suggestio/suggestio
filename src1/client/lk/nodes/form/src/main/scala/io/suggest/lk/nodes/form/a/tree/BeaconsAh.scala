package io.suggest.lk.nodes.form.a.tree

import diode._
import diode.data.Pot
import io.suggest.ble.BeaconDetected
import io.suggest.lk.nodes.{MLknBeaconsScanReq, MLknConf, MLknNode}
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.primo.Keep
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import scalaz.{Tree, TreeLoc}

import scala.collection.immutable.HashMap
import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.09.2020 22:03
  * Description: Контроллер для работы с маячками.
  */
object BeaconsAh {
  final def PENDING_VALUE_NEED_REGET = -1L
}

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

      val rootTreeId = MTreeRoles.Root.treeId
      val tree0: Tree[String] = v0.tree
        .idsTreeOpt
        .getOrElse {
          Tree.Leaf(
            root   = rootTreeId,
          )
        }

      // В начало дерева надо добавить/обновить группу видимых маячков:
      val loc0 = tree0.loc

      // Найти текущую группу маячков в дереве:
      val bcnGroupId = MTreeRoles.BeaconsDetected.treeId
      val bcnsGroupLocOpt0 = loc0
        .findChild( _.rootLabel ==* bcnGroupId )

      // Дополнить группу маячков свеже-полученными данными:
      val (bcnsGroupLoc1, nodesAppend1, haveBcnIds) = bcnsGroupLocOpt0
        .fold [(TreeLoc[String], HashMap[String, MNodeState], Set[String])] {
          // Пока нет подсписка с маячками. Добавить в начало общего дерева:
          val bcnsSubTreeEmpty = Tree.Leaf(
            root = bcnGroupId,
          )
          val mns2 = MNodeState(
            role = MTreeRoles.BeaconsDetected,
          )
          val nodesAppend = HashMap.empty + (bcnGroupId -> mns2)
          val loc1 = loc0.insertDownFirst( bcnsSubTreeEmpty )
          (loc1, nodesAppend, Set.empty[String])

        } { bcnsGroupLoc0 =>
          // Нужно пройти текущую группу, обновив инфу в уже отрендеренных маячках.
          lazy val bcnSignalNow = BeaconDetected.seenNowMs()

          val nodesAppend2 = (for {
            treeKeySubtree0 <- bcnsGroupLoc0
              .tree
              .subForest
              // Форсируем без lazy, чтобы выкинуть из памяти старые инстасы MBeaconSignal.
              .iterator
            treeKey = treeKeySubtree0.rootLabel
            mns0 <- v0.tree.nodesMap.get( treeKey )
            nodeIdOpt = mns0.nodeId
            nodeId <- nodeIdOpt
            bcnSignalOpt2 = m.beacons.get( nodeId )
            bcnSignal2 <- bcnSignalOpt2 orElse mns0.beacon.map(_.data)
            bcnState2 = MNodeBeaconState(
              data      = bcnSignal2,
              isVisible = bcnSignalOpt2.nonEmpty || bcnSignal2.detect.isStillVisibleNow( bcnSignalNow ),
            )
            mns2 = (MNodeState.beacon set Some(bcnState2))(mns0)
          } yield {
            treeKey -> mns2
          })
            .to( HashMap )

          // Собираем id уже пройденных маячков:
          (bcnsGroupLoc0, nodesAppend2, nodesAppend2.keySet)
        }

      // Нужно определить НОВЫЕ маячки, которых нет в исходном дереве.
      val appendBcns = if (haveBcnIds.isEmpty)
        m.beacons
      else
        m.beacons
          .view
          .filterKeys( !haveBcnIds.contains(_) )
          .toMap

      // Сборка обновлённой карты данных по узлам:
      val nodesAppend2: HashMap[String, MNodeState] = if (appendBcns.isEmpty) {
        nodesAppend1
      } else {
        nodesAppend1.merged[MNodeState](
          appendBcns
            .view
            .mapValues { bcnSignal =>
              MNodeState(
                role = MTreeRoles.BeaconSignal,
                beacon = Some( MNodeBeaconState(bcnSignal) ),
                // Порыться в кэше на предмет серверной инфы по маячку:
                infoPot = bcnSignal.detect.signal.beaconUid
                  .flatMap( v0.tree.nodesMap.get )
                  .fold( Pot.empty[MLknNode] )( _.infoPot ),
              )
            }
            .to( HashMap )
        )( Keep.right )
      }

      // Собрать обновлённый location для дерева узлов:
      val bcnsGroupLoc2 = if (appendBcns.isEmpty) {
        // Не требуется добавлять новых маячков в список:
        bcnsGroupLoc1
      } else {
        // Добавляем неизвестные ранее маячки в список:
        val tree1 = Tree.Node(
          root = bcnsGroupLoc1.getLabel,
          forest = bcnsGroupLoc1.tree.subForest ++ appendBcns
            .keySet
            .toEphemeralStream
            .map( Tree.Leaf(_) ),
        )
        bcnsGroupLoc1.setTree( tree1 )
      }

      var modTreeF = MTree.setNodes( bcnsGroupLoc2.toTree )

      var nodesMap2 = v0.tree.nodesMap
      if (v0.tree.idsTreeOpt.isEmpty || !(nodesMap2 contains rootTreeId))
        nodesMap2 += (rootTreeId -> MNodeState.mkRootNode)
      if (nodesAppend2.nonEmpty)
        nodesMap2 = nodesMap2.merged( nodesAppend2 )(Keep.right)
      if (nodesMap2 !===* v0.tree.nodesMap)
        modTreeF = modTreeF andThen (MTree.nodesMap set nodesMap2)

      // Нужно проинкрементить корневой элемент opened-path после добавления корневой подгруппы Bluetooth-маячков.
      if (
        v0.tree.opened.exists(_.nonEmpty) &&
        bcnsGroupLocOpt0.isEmpty
      ) {
        modTreeF = modTreeF andThen MTree.opened.modify { openedOpt0 =>
          openedOpt0.map { opened0 =>
            (opened0.head + 1) :: opened0.tail
          }
        }
      }

      var modTreeOuterF = MTreeOuter.tree.modify( modTreeF )
      var modBeaconsAcc = List.empty[MBeaconScan => MBeaconScan]

      // TODO Если исчез opened-маячок, то обновить opened на валидное значение.

      var fxAcc = List.empty[Effect]

      if (!v0.beacons.scanReq.isPending) {
        // Если есть маячки для запроса с сервера, то запросить инфу по ним.
        val unknownBcnIds = (for {
          bcnTree <- bcnsGroupLoc2
            .tree
            .subForest
            .iterator
          treeId = bcnTree.rootLabel
          mnsOpt = nodesMap2.get( treeId )
          if mnsOpt.fold {
            // Нельзя запрашивать с сервера всякие виртуальные узлы или под-группы:
            MTreeRoles
              .withNameOption( treeId )
              .isEmpty
          } { mns =>
            // Если необходимо получить инфу
            (mns.infoPot.isEmpty && !mns.infoPot.isUnavailable) ||
            // Или если выставлен маркер, что нужен reget...
            (mns.infoPot isPendingWithStartTime BeaconsAh.PENDING_VALUE_NEED_REGET)
          }
        } yield {
          (for {
            mns <- mnsOpt
            beacon <- mns.beacon
            bUid <- beacon.data.detect.signal.beaconUid
          } yield {
            bUid
          })
            .orElse {
              mnsOpt
                .flatMap( _.nodeId )
            }
            .getOrElse( treeId )
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

          modBeaconsAcc ::= MBeaconScan.scanReq.modify( _.pending() )

        }
      }

      // Обновлений от сигналов маячков может быть много каждую секунду. Но обновлять экран надо не чаще раза в секунду,
      // иначе всё затупит и встанет колом. Поэтому, нужно silent update, а рендер запускать только по таймауту.
      if (v0.beacons.updateTimeout ==* Pot.empty) {
        fxAcc ::= _reRenderFx
        modBeaconsAcc ::= MBeaconScan.updateTimeout.modify(_.pending())
      }

      // Сохранить обновлённое состояние:
      if (modBeaconsAcc.nonEmpty)
        modTreeOuterF = modTreeOuterF andThen MTreeOuter.beacons.modify( modBeaconsAcc.reduce(_ andThen _) )

      val v2 = modTreeOuterF(v0)
      ah.updatedSilentMaybeEffect( v2, fxAcc.mergeEffects )


    // Срабатывание таймера отложенного рендера.
    case BeaconsRenderTimer =>
      val v0 = value

      if (v0.beacons.updateTimeout.isPending) {
        val v2 = (
          MTreeOuter.beacons
            .composeLens( MBeaconScan.updateTimeout )
            .set( Pot.empty ) andThen
          // Явная пересборка инстанса tree, чтобы шаблоны заметили обновление.
          MTreeOuter.tree
            .modify(_.copy())
        )(v0)
        // updated вызывает пере-рендер в шаблонах.
        updated(v2)
      } else {
        // pending отсутствует в состоянии - игнорируем команду к пере-рендеру.
        noChange
      }


    // Отработка результата запроса на сервер с маячками.
    case m: BeaconsScanResp =>
      val v0 = value

      var modF = m.tryResp.fold(
        // Запрос не удался.
        {ex =>
          MTreeOuter.beacons
            .composeLens( MBeaconScan.scanReq )
            .modify(_.fail(ex))
          // TODO sc: эффект check-connectivity в выдачу?
        },

        // Ответ получен - разобраться.
        {resp =>
          // Найденные маячки:
          var cachedMapAppend = resp.subTree
            .subForest
            .map { nodeResp =>
              val lknNode = nodeResp.rootLabel
              val mns2 = v0.tree
                .nodesMap
                .get( lknNode.id )
                .fold {
                  MNodeState(
                    infoPot   = Pot.empty.ready( lknNode ),
                    role      = MTreeRoles.BeaconSignal,
                  )
                } {
                  MNodeState.infoPot.modify(_.ready(lknNode))
                }
              lknNode.id -> mns2
            }
            // Собираем промежуточную карту, т.к. keySet нужен на следующем шаге:
            .iterator
            .to( HashMap )

          // Определить НЕнайденные на сервере маячки, чтобы инфу о них тоже закэшировать:
          val notFoundBcnIds = m.reqArgs.beaconUids -- cachedMapAppend.keySet
          if (notFoundBcnIds.nonEmpty) {
            val notFoundMap = (for {
              notFoundBcnId <- notFoundBcnIds.iterator
            } yield {
              val mns2 = v0.tree
                .nodesMap
                .get( notFoundBcnId )
                .map( MNodeState.infoPot.modify(_.unavailable()) )
                .getOrElse {
                  MNodeState(
                    infoPot = Pot.empty.unavailable(),
                    role = MTreeRoles.BeaconSignal,
                  )
                }
              // Есть неизвестные для сервера маячки.
              notFoundBcnId -> mns2
            })
              .to( HashMap )

            cachedMapAppend = cachedMapAppend.merged( notFoundMap )( Keep.right )
          }

          // Залить новую карту маячков в состояние:
          (
            MTreeOuter.tree
              .composeLens( MTree.nodesMap )
              .modify( _.merged(cachedMapAppend)(Keep.right) ) andThen
            MTreeOuter.beacons
              .composeLens( MBeaconScan.scanReq )
              .modify(_.ready(resp))
          )
        }
      )

      val fxOpt = Option.when( v0.beacons.updateTimeout ==* Pot.empty ) {
        // TODO Opt тут MTreeOuter.beacons обновляется второй раз. Надо бы объеденить все изменения внутри beacons-поля.
        modF = modF andThen MTreeOuter.beacons
          .composeLens( MBeaconScan.updateTimeout )
          .modify(_.pending())

        _reRenderFx
      }

      val v2 = modF( v0 )
      ah.updatedSilentMaybeEffect( v2, fxOpt )

  }


  private def _reRenderFx: Effect = Effect {
    DomQuick
      .timeoutPromiseT( 1333 )( BeaconsRenderTimer )
      .fut
  }

}
