package io.suggest.sc.controller.inx

import diode.{ActionHandler, ActionResult, Effect, ModelRO, ModelRW}
import io.suggest.lk.m.CsrfTokenEnsure
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp}
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.index.{MIndexesRecentJs, MSc3IndexResp, MScIndexInfo, MScIndexes}
import io.suggest.sc.model.inx.save.MIndexesRecentOuter
import io.suggest.sc.model.search.{MNodesFoundS, MSearchCssProps, MSearchRespInfo}
import io.suggest.sc.model.{LoadIndexRecents, MScRoot, NodeRecentNodeClick, ResetUrlRoute, SaveRecentIndex}
import io.suggest.sc.util.ScRoutingUtil
import io.suggest.sc.util.api.IScStuffApi
import io.suggest.sc.view.search.SearchCss
import io.suggest.spa.DiodeUtil.Implicits.PotOpsExt
import io.suggest.spa.{DAction, DoNothing, SioPages}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.spa.DiodeUtil.Implicits._

import scala.util.{Success, Try}

/** Controller for recent indexes list. */
final class NodesRecentAh[M](
                              scStuffApi           : => IScStuffApi,
                              scRootRO             : ModelRO[MScRoot],
                              modelRW              : ModelRW[M, MIndexesRecentOuter],
                            )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  private def recents2searchCssModF(inxRecents: MScIndexes) = {
    MIndexesRecentOuter.searchCss
      .andThen( SearchCss.args )
      .andThen( MSearchCssProps.nodesFound )
      .andThen( MNodesFoundS.req )
      .modify( _.ready(
        MSearchRespInfo(
          resp = MGeoNodesResp(
            nodes = for (inxInfo <- inxRecents.indexes) yield {
              MGeoNodePropsShapes(
                props = inxInfo.indexResp,
              )
            }
          ),
        )
      ))
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Action about choosing node inside recent nodes list.
    case m: NodeRecentNodeClick =>
      val v0 = value

      val outer_saved_LENS = MIndexesRecentOuter.saved

      // Go to previosly-visited location. Find in recent-list previous showcase state:
      outer_saved_LENS
        .get( v0 )
        .iterator
        .flatMap(_.indexes)
        .find(_.indexResp ===* m.inxRecent)
        .fold {
          logger.warn( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          noChange

        } { inxRecent =>
          // Click to node. Recover choosen state:
          val routeFx = Effect.action {
            // Patch current showcase route with data from previous state:
            val mroot = scRootRO.value
            val currentRoute = ScRoutingUtil.root_internals_info_currRoute_LENS.get( mroot )
            var nextRoute = currentRoute
              .fold( inxRecent.state )( _ silentSwitchingInto inxRecent.state )

            // If mobile screen, close menu panel (via route flag):
            if (nextRoute.menuOpened && mroot.grid.core.jdConf.gridColumnsCount <= 3)
              nextRoute = (SioPages.Sc3.menuOpened replace false)(nextRoute)

            ResetUrlRoute( mods = Some(_ => nextRoute) )
          }

          effectOnly( routeFx )
        }


    // Do start for reading saved recent sc-indexes list.
    case m: LoadIndexRecents =>
      val v0 = value

      (for {
        indexes2 <- m.pot.toOption
      } yield {
        // If !clean, and there cleared results from server, let's recover original sorting, because server returns unsorted chunked result.
        // TODO Need to check for NEW items, added into recent nodes list during request time. So, need to work with 3 lists (current (from current state), requested, and server-responded result).
        val pot2 = (for {
          indexesOuter0 <- v0.saved
          indexes0 = indexesOuter0.indexes
          if !m.clean && !m.pot.isFailed &&
            indexes0.nonEmpty && indexes2.nonEmpty
        } yield {
          val nodeId2i = indexes0
            .iterator
            .flatMap(_.state.nodeId)
            .zipWithIndex
            .toMap

          MScIndexes.indexes.modify { inxs =>
            inxs.sortBy { inx =>
              inx.state.nodeId
                .flatMap( nodeId2i.get )
                .getOrElse( Int.MaxValue )
            }
          }(indexes2)
        })
          .orElse( m.pot )

        // Successfully read result:
        val modF = (
          (MIndexesRecentOuter.saved replace pot2) andThen
          // Update CSS for nodes list, because everything is ok.
          (recents2searchCssModF( indexes2 ))
        )

        // If clean, launch request to server (to check/ensure nodes, update names/colors, receive updated pictures URLs in CDN, etc).
        val fxOpt = Option.when( m.clean && indexes2.indexes.nonEmpty ) {
          Effect.action {
            CsrfTokenEnsure(
              onComplete = Some {
                Effect {
                  scStuffApi
                    .fillNodesList( indexes2 )
                    .transform { tryRes =>
                      // Convert responses to runtime state format. Replace updated nodes.
                      val respRes = for (resps2 <- tryRes) yield {
                        val resps2Map = (for {
                          r <- resps2.iterator
                          nodeId <- r.nodeId
                        } yield {
                          nodeId -> r
                        })
                          .toMap

                        MScIndexes.indexes.modify { inxs0 =>
                          for {
                            inx0 <- inxs0
                            nodeId <- inx0.indexResp.nodeId
                            resp2 <- resps2Map.get( nodeId )
                          } yield {
                            MScIndexInfo.indexResp.replace( resp2 )(inx0)
                          }
                        }(indexes2)
                      }
                      val a = m.copy(
                        clean = false,
                        pot = m.pot.withTry(respRes),
                      )
                      Success(a)
                    }
                }
              }
            )
          }
        }
        val v2 = modF(v0)
        ah.updatedMaybeEffect( v2, fxOpt )
      })
        .orElse {
          for (ex <- m.pot.exceptionOption) yield {
            logger.error( ErrorMsgs.SRV_REQUEST_FAILED, ex, m )
            // Save request error to state:
            val v2 = MIndexesRecentOuter.saved
              .modify( _.fail(ex) )(v0)
            updated(v2)
          }
        }
        .getOrElse {
          if (m.clean) {
            // Start effect for read data from client local storage:
            val fx = Effect.action {
              val tryRes = Try {
                MIndexesRecentJs
                  .get()
                  .getOrElse( MScIndexes.empty )
              }
              LoadIndexRecents.pot.modify(_ withTry tryRes)(m)
            }
            val outer_saved_LENS = MIndexesRecentOuter.saved
            val v2 = if (outer_saved_LENS.exist(_.isPending)(v0)) {
              v0
            } else {
              outer_saved_LENS.modify(_.pending())(v0)
            }
            updatedSilent( v2, fx )

          } else {
            // Just rebuild current state instance, so some lazy vals will be updated.
            val v2 = v0.copy()
            updated(v2)
          }
        }


    // Save info about new node index:
    case m: SaveRecentIndex =>
      val v0 = value

      m.inxRecent2.fold {
        // Effect, if there are a route and something is changed in:
        val mroot = scRootRO.value
        val fxOpt = for {
          currRoute         <- mroot.internals.info.currRoute
          currIndexRespData <- mroot.index.respOpt
        } yield {
          Effect.action {
            // Read currently saved state:
            val recentOpt0 = v0.saved.toOption orElse {
              val tryRes = Try {
                MIndexesRecentJs.get()
              }
              for (ex <- tryRes.failed)
                logger.warn( ErrorMsgs.KV_STORAGE_ACTION_FAILED, ex )
              tryRes
                .toOption
                .flatten
            }

            // Make MIndexInfo for saving instance.
            val inxInfo2 = MScIndexInfo(
              state         = currRoute,
              indexResp     = {
                if (currIndexRespData.geoPoint.nonEmpty) {
                  currIndexRespData
                } else {
                  // If no geo-point for node, read it from geo map center:
                  (
                    MSc3IndexResp.geoPoint replace Some(mroot.index.search.geo.mapInit.state.center)
                  )(currIndexRespData)
                }
              },
            )

            // Save/update new state:
            val recentOpt2 = recentOpt0.fold [Option[MScIndexes]] {
              val ir = MScIndexes( inxInfo2 :: Nil )
              Some(ir)
            } { recents0 =>
              val isDuplicate = recents0.indexes.exists { m =>
                (m.state isSamePlaceAs currRoute)
              }

              Option.when(!isDuplicate) {
                val maxItems = MScIndexes.MAX_RECENT_ITEMS
                MScIndexes.indexes.modify { r0 =>
                  // Remove old values from list, that looking same. (ignoring isLoggedIn and other route flags).
                  val r1 = r0.filterNot(_.indexResp isLogoTitleBgSame inxInfo2.indexResp)
                  // Limit list by maximum length.
                  val r2 =
                    if (r1.lengthIs > maxItems)  r1.take( maxItems - 1 )
                    else  r1
                  // So, prepend updated list with new element.
                  inxInfo2 :: r2
                } (recents0)
              }
            }

            recentOpt2.fold[DAction] {
              DoNothing
            } { recents2 =>
              // Write into permanent storage:
              for {
                ex <- Try( MIndexesRecentJs.save( recents2 ) ).failed
              } {
                logger.warn( ErrorMsgs.KV_STORAGE_ACTION_FAILED, ex )
              }

              SaveRecentIndex( recentOpt2 )
            }
          }
        }

        fxOpt.fold (noChange) { fx =>
          val v2 = MIndexesRecentOuter.saved
            .modify( _.pending() )( v0 )
          updatedSilent(v2, fx)
        }

      } { inxRecent2 =>
        val v2 = (
          MIndexesRecentOuter.saved
            .modify( _.ready(inxRecent2) ) andThen
            recents2searchCssModF( inxRecent2 )
        )(v0)

        // Non-silent, because updating render for some parts in left menu:
        updated( v2 )
      }

  }

}
