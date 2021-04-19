package io.suggest.sc.c.grid

import diode.{ActionResult, Effect}
import diode.data.Pot
import io.suggest.common.empty.OptionUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.jd.render.u.JdUtil
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.c.{IRespWithActionHandler, MRhCtx}
import io.suggest.sc.m.{MScRoot, ResetUrlRoute, SetErrorState}
import io.suggest.sc.m.dia.err.MScErrorDia
import io.suggest.sc.m.grid.{GridAdKey_t, GridBlockClick, MGridAds, MGridCoreS, MGridItem, MGridS, MScAdData}
import io.suggest.sc.sc3.{MSc3RespAction, MScRespActionType, MScRespActionTypes}
import io.suggest.log.Log
import io.suggest.n2.edge.MEdgeFlags
import io.suggest.sc.ads.MSc3AdData
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import scalaz.{EphemeralStream, Tree, TreeLoc}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.03.2020 16:43
  * Description: Resp-handler для обработки ответа по фокусировке одной карточки.
  */
final class GridFocusRespHandler
  extends IRespWithActionHandler
  with Log
{

  override def isMyReqReason(ctx: MRhCtx): Boolean = {
    ctx.m.reason.isInstanceOf[GridBlockClick]
  }

  override def getPot(ctx: MRhCtx): Option[Pot[_]] = {
    // Тут возвращает Pot для main-блока, т.к. именно он выставляется pending,
    // а focused-карточка ещё не существует в состоянии плитки.
    val gridAds = ctx.value0.grid.core.ads

    // reason может быть не только GridBlockClick, но и GetIndex, например:
    (for {
      gbc <- Option( ctx.m.reason ).collect {
        case m: GridBlockClick => m
      }
      gridKey <- gbc.gridKey
    } yield {
      gridAds
        .interactAdOpt
        .filter( _.getLabel.gridItemWithKey( gridKey ).nonEmpty )
        .orElse {
          for {
            gridKeyPath <- gbc.gridPath
            adPtrsTree <- gridAds.adsTreePot.toOption
            loc <- adPtrsTree.loc.findByGridKeyPath( gridKeyPath )
          } yield {
            loc
          }
        }
        .map( _.getLabel.data )
    })
      .getOrElse {
        Some( gridAds.adsTreePot )
      }
  }

  override def handleReqError(ex: Throwable, ctx: MRhCtx): ActionResult[MScRoot] = {
    val eMsg = ErrorMsgs.XHR_UNEXPECTED_RESP
    logger.error(eMsg, ex, msg = ctx.m)
    val reason = ctx.m.reason.asInstanceOf[GridBlockClick]

    val errFx = Effect.action {
      val m = MScErrorDia(
        messageCode = eMsg,
        potRO       = Some(
          ctx.modelRW.zoom { mroot =>
            Pot
              .fromOption {
                reason.gridPath.flatMap { gridKeyPath =>
                  mroot.grid.core.ads.adsTreePot
                    .toOption
                    .flatMap( _.loc.findByGridKeyPath(gridKeyPath) )
                }
              }
              .flatMap(_.getLabel.data)
          }
        ),
        retryAction = Some( ctx.m.reason ),
      )
      SetErrorState(m)
    }

    val g0 = ctx.value0.grid

    (for {
      adLoc0 <- GridAh.findAd( reason, g0.core.ads )
    } yield {
      val adLoc1 = adLoc0.modifyLabel( MScAdData.data.modify(_.fail(ex)) )
      val v2 = MScRoot.grid
        .composeLens( MGridS.core )
        .composeLens( MGridCoreS.ads )
        .composeLens( MGridAds.adsTreePot )
        .modify( _.map( _ => adLoc1.toTree ) )( ctx.value0 )
      ActionResult.ModelUpdateEffect(v2, errFx)
    })
      .getOrElse {
        logger.warn(ErrorMsgs.FOC_LOOKUP_MISSING_AD, ex, ctx.m.reason )
        ActionResult.EffectOnly( errFx )
      }
  }

  override def isMyRespAction(raType: MScRespActionType, ctx: MRhCtx): Boolean = {
    raType ==* MScRespActionTypes.AdsFoc
  }

  override def applyRespAction(ra: MSc3RespAction, ctx: MRhCtx): ActionResult[MScRoot] = {
    val focQs = ctx.m.qs.foc.get
    val g0 = ctx.value0.grid
    val gridAds0 = g0.core.ads
    val gbcOpt = ctx.m.reason match {
      case gbl: GridBlockClick => Some( gbl )
      case _ => None
    }

    (for {
      adsTree <- gridAds0
        .adsTreePot
        .toOption
      adsTreeRootLoc = adsTree.loc
      // Собрать данные по main-блоку, который фокусируют:
      // reason.gridKey может быть пуст, например когда фокусировка по карточке, которая никак в плитке
      // не представлена в плитке (ad_id взят из URL qs).
      parentLoc0 = gbcOpt
        .flatMap { gbc =>
          // Задан ключ элемента плитки. Надо добавить туда:
          GridAh.findAd( gbc, gridAds0 )
        }
        .orElse {
          // Нет исходного gridKey в исходном экшене. Т.е. карточка запрошена напрямую по ad_id.
          // Поискать в плитке родительский элемент по ad_id.
          for {
            focQs <- ctx.m.qs.foc

            mainLoc <- adsTreeRootLoc.find { scAdLoc =>
              scAdLoc.getLabel.data.exists { scAdData =>
                scAdData.doc.tagId.nodeId.exists { tagNodeId =>
                  focQs.adIds.iterator contains tagNodeId
                }
              }
            }
          } yield {
            // Найдена основа для focused карточки среди текущих карточек.
            mainLoc
          }
        }
        .getOrElse {
          // Добавить текущую карточку в конец текущий плитки. Создание main-блока будет отработано ниже.
          adsTreeRootLoc
        }

      // Найдена точка в дереве, куда надо добавить полученную карточку.
      focResp <- ra.ads
      // TODO focResp.ads.headOption - надо отрабатывать все принятые карточки.
      focAdResp <- focResp.ads.headOption

      // Отрендерить все gridItems для принятой карточки:
      focAdJdDoc = focAdResp.jd.doc
      focAdIndexed = JdUtil.mkTreeIndexed( focAdJdDoc )

      // Поиск main-блока в focused-ответе:
      (mainBlockJdLoc, mainBlockIndex) <- {
        val r = focAdIndexed.getMainBlockOrFirst()
        if (r.isEmpty)
          logger.error( ErrorMsgs.JD_TREE_UNEXPECTED_ROOT_TAG, msg = (focAdIndexed, r) )
        r
      }

    } yield {
      // Убрать возможный pending на родительской карточке:
      val parent = parentLoc0.getLabel
      val parentLoc = if (parent.data.isPending) {
        parentLoc0.modifyLabel( MScAdData.data.modify(_.unPending) )
      } else {
        parentLoc0
      }

      val parentLevel = parentLoc.parents.length

      // Следующий id для сохранения в состоянии:
      var idCounter2 = gridAds0.idCounter

      val mainBlockNodeId = mainBlockJdLoc.rootLabel._1.nodeId
      val isSameNodeIdAsParent = parent.data.exists(_.doc.tagId.nodeId ==* mainBlockNodeId)

      val focAdGridItems = focAdIndexed
        .gridItemsIter
        .map { itemSubTree =>
          val (jdTagId, _) = itemSubTree.rootLabel

          MGridItem(
            gridKey = {
              // Если родительский элемент того же id (например, main-блок),
              // то нужно сделать так, чтобы grid_key совпадали с родительскими.
              OptionUtil.maybeOpt(
                isSameNodeIdAsParent &&
                (jdTagId.selPathRev.headOption contains mainBlockIndex)
              ) {
                // Берём gridKey из main-блока родительской карточки:
                parent
                  .gridItems
                  .find(_.jdDoc.tagId.selPathRev.headOption contains[Int] mainBlockIndex)
                  .map(_.gridKey)
              }
                .getOrElse {
                  val nextId = idCounter2
                  idCounter2 += 1
                  nextId
                }
            },
            jdDoc = focAdJdDoc.copy(
              // TODO Opt разиндексация дерева после индексации (mkTreeIndexed) - это неоптимально. Нельзя ли как-то по-экономнее?
              template  = itemSubTree.map(_._2),
              tagId     = jdTagId,
            ),
          )
        }
        // Ленивость НЕЛЬЗЯ, т.к. функция имеет side-эффекты.
        .to( List )

      // Сборка начального под-дереве для последующего добавления в общее дерево карточек:
      var focAdSubTree = Tree.Leaf {
        val focJdDataJs = MJdDataJs.fromJdData( focAdResp.jd, focAdResp.info )
        MScAdData(
          data      = Pot.empty.ready( focJdDataJs ),
          gridItems = focAdGridItems,
          partialItems = false,
        )
      }

      // Отработать возможные особые случаи, когда карточка добавляется в каких-то нетривиальных условиях.
      val focAddedLoc: TreeLoc[MScAdData] = (for {
        // Попытаться обнаружить ситуацию добавления карточки на верхний уровень, когда отсутствует main-блок для последующего сворачивания.
        // В карточке -- более одно блока?
        focAdDocTail <- focAdResp.jd.doc.template
          .subForest
          .tailOption
        if !focAdDocTail.isEmpty &&
          // Родительский элемент плитки является корневым?
          (parentLevel ==* 0) &&
          // нет флага AlwaysOpened?
          !focAdResp.info.flags
            .exists(_.flag ==* MEdgeFlags.AlwaysOpened)
      } yield {
        // Поискать main среди подготовленных gridItems, попытаться продублировать его grid key на main-блок:
        val unFocAdGridKey: GridAdKey_t = focAdGridItems
          .iterator
          .zipWithIndex
          .find(_._2 ==* mainBlockIndex)
          // Когда добавление раскрытой карточки на верхний уровень, и карточку можно свернуть потом,
          // нужно добавить main-блок над текущей развёрнутой карточкой (для возможности сворачивания потом).
          .fold[GridAdKey_t] {
            // Почему-то не найден main-блок среди ранее подготовленных focAd gridItems:
            logger.warn( ErrorMsgs.NODE_NOT_FOUND, msg = (focAdJdDoc.tagId, mainBlockIndex) )
            val nextGridKey = idCounter2
            idCounter2 += 1
            nextGridKey
          } (_._1.gridKey)

        parentLoc.insertDownLast(
          Tree.Node(
            root = {
              // Производим ленивую расфокусорвку карточки в main-блок, чистим эджи, обновляем jdId:
              val unFocAd = (
                MSc3AdData.jd.modify { jdData0 =>
                  val jdDoc2 = jdData0.doc.copy(
                    template = mainBlockJdLoc.map(_._2),
                    tagId    = mainBlockJdLoc.rootLabel._1,
                  )
                  val mainBlockEdgeUidsMap = jdDoc2.template.edgesUidsMap
                  jdData0.copy(
                    doc   = jdDoc2,
                    edges = jdData0.edges
                      .filter { jdEdge =>
                        jdEdge.edgeDoc.id
                          .exists( mainBlockEdgeUidsMap.contains )
                      },
                  )
                }
              )( focAdResp )
              val unFocJdDataJs = MJdDataJs.fromJdData( unFocAd.jd, unFocAd.info )
              MScAdData(
                Pot.empty.ready( unFocJdDataJs ),
                gridItems = MGridItem(
                  gridKey = unFocAdGridKey,
                  jdDoc   = unFocAd.jd.doc,
                ) :: Nil,
                partialItems = false,
              )
            },
            forest = EphemeralStream( focAdSubTree ),
          )
        )
      })
        // Отработать возможное раскрытие новой под-карточки внутри другой (раскрытой) карточки:
        .orElse {
          for {
            gbl <- gbcOpt
            reasonGridKey <- gbl.gridKey
            // Если добавление на под-уровни (не на верхний ряд карточек),
            if parentLevel >= 1
            parent = parentLoc.getLabel
            // если id родительской карточки отличается от полученной focused-карточки
            if parent.data.exists { parentJdDataJs =>
              parentJdDataJs.doc.tagId.nodeId !=* focAdJdDoc.tagId.nodeId
            }
          } yield {
            // то надо в поддереве продублировать родительскую карточку вокруг полученной focused-карточки.
            // Разбиваем родительскую карточку надвое, врезав новую focused-карточку после кликнутого блока:
            val (beforeGridItems, afterGridItems) = parent.gridItems.span { gridItem =>
              gridItem.gridKey <= reasonGridKey
            }
            val scAdData_gridItems_LENS = MScAdData.gridItems

            // Теперь, организовать полный parentLoc subForest, с несколькими поддеревьями.
            // Используется List acc, т.к. EphemeralStream/LazyList не совместимы с переменными внутри ##::= -выражений.
            var subForestAcc = List.empty[Tree[MScAdData]]
            if (afterGridItems.nonEmpty)
              subForestAcc ::= Tree.Leaf {
                (scAdData_gridItems_LENS set afterGridItems)( parent )
              }
            subForestAcc ::= focAdSubTree
            val hasBefore = beforeGridItems.nonEmpty
            if (hasBefore)
              subForestAcc ::= Tree.Leaf {
                (scAdData_gridItems_LENS set beforeGridItems)( parent )
              }

            // Сохранить обновлённый subForest в дерево:
            parentLoc
              .setTree(
                Tree.Node(
                  parent,
                  subForestAcc.toEphemeralStream
                )
              )
              // Выставить локацию на добавленную focused-карточку, как и в остальных ветвях:
              .getChild(
                if (hasBefore) 2
                else 1
              )
              .get
          }
        }
        // Никакой специальной отработки ситуаций не требуется. Просто добавить под-карточку в дерево.
        .getOrElse {
          parentLoc.insertDownLast( focAdSubTree )
        }

      val focGridKey = focAdGridItems
        .headOption
        .fold {
          logger.warn( ErrorMsgs.SHOULD_NEVER_HAPPEN, msg = (ErrorMsgs.UNEXPECTED_EMPTY_DOCUMENT, focAdGridItems) )
          idCounter2 - 1
        }(_.gridKey)
      val focGridKeyPath = focAddedLoc.gridKeyPath
      val focAddedTree = focAddedLoc.toTree

      // Сохранить новое дерево и счётчик в состояние:
      val gridCore1 = MGridCoreS.ads.set {
        gridAds0.copy(
          idCounter = idCounter2,
          adsTreePot = gridAds0.adsTreePot.ready( focAddedTree ),
          interactWith = Some((focGridKeyPath, focGridKey)),
        )
      }(g0.core)

      // Обновление фокусировки:
      val gridCore2 = GridAh.resetFocus( Some(focGridKeyPath), gridCore1 )

      // Надо проскроллить выдачу на начало открытой карточки:
      val scrollFx = GridAh.scrollToAdFx( focAddedLoc.getLabel, gridCore2.gridBuild )
      val resetRouteFx = ResetUrlRoute().toEffectPure

      val v2 = MScRoot.grid
        .composeLens(MGridS.core)
        .set( gridCore2 )( ctx.value0 )

      val fxOpt = Some(scrollFx + resetRouteFx)
      ActionResult(Some(v2), fxOpt)
    })
      // m.gridPtr = None не отрабатываем, т.к. gridKey должен уже быть выставлен на раннем этапе.
      .getOrElse {
        logger.warn(ErrorMsgs.FOC_LOOKUP_MISSING_AD, msg = focQs)
        ActionResult.NoChange
      }
  }

}
