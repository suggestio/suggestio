package controllers.sc

import io.suggest.common.empty.OptionUtil.Implicits._
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.MEsUuId.Implicits._
import io.suggest.geo.{MGeoLoc, MGeoLocSources, MLocEnv}
import io.suggest.n2.node.MNode
import io.suggest.sc.ads.{MAdsSearchReq, MScFocusArgs}
import io.suggest.sc.sc3._
import io.suggest.sc.{MScApiVsns, ScConstants}
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
import models.req.IReq
import play.api.libs.json.Json

import javax.inject.Inject
import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.18 15:04
  * Description: Единое API для выдачи: разные запросы и их сложные комбинации в рамках только одного запроса.
  *
  * Это позволяет запрашивать, например, focused и grid карточки одновременно только одним запросом.
  *
  * Другой полезные случай: возможность запрашивать focused->index+grid+focused, чтобы за один запрос получался
  * переход в нужную выдачу с уже раскрытой карточкой за один запрос к Sc-контроллеру.
  */
final class ScUniApi @Inject()(
                                val scCtlUtil: ScCtlUtil,
                              )
  extends MacroLogsImpl
{

  // Parts of Showcase, used for different tasks.
  // To deduplicate instances for dynamic injections, all inject calls moved into scCtlApi.
  protected[this] lazy val scAdsTile = new ScAdsTile( scCtlUtil )
  protected[this] lazy val scFocusedAds = new ScFocusedAds( scCtlUtil )
  protected[this] lazy val scSearch = new ScSearch( scCtlUtil )
  protected[this] lazy val scIndex = new ScIndex( scCtlUtil )


  import scCtlUtil._
  import sioControllerApi._


  /** Logic of Sc UniApi action wrapped into class. */
  case class ScPubApiLogicHttpV3(qs: MScQs)
                                (override implicit val _request: IReq[_]) extends scCtlUtil.LazyContext {

    // Разобрать qs, собрать на исполнение.
    lazy val logPrefix = s"PubApi#${System.currentTimeMillis()}:"

    lazy val geoIpInfo = scCtlUtil.GeoIpInfo(qs)

    /** Надо ли выполнять перескок focused => index в какой-то другой узел?
      * Да, если фокусировка активна, перескок разрешён, и проверка перескока вернула узел.
      */
    lazy val focJumpToIndexNodeOptFut: Future[Option[MNode]] = {
      (for {
        focQs <- qs.foc
        if focQs.indexAdOpen.nonEmpty
      } yield {
        // Запустить нормальную проверку на перескок в индекс.
        scUtil.isFocGoToProducerIndexFut( qs )
      })
        .getOrNoneFut
    }


    /** Собрать index-логику, когда требуется. */
    lazy val indexLogicOptFut: Future[Option[scIndex.ScIndexLogic]] = {
      if (qs.index.nonEmpty) {
        val logic = scIndex.ScIndexLogic(qs, geoIpInfo)(_request)
        LOGGER.trace(s"$logPrefix Normal index-logic created: $logic")
        Future.successful( Some(logic) )
      } else if ( qs.foc.exists(_.indexAdOpen.nonEmpty) ) {
        for (toNnodeOpt <- focJumpToIndexNodeOptFut) yield {
          for (toNode <- toNnodeOpt) yield {
            val inxLogic = scIndex.ScFocToIndexLogicV3(toNode, qs, geoIpInfo)( _request )
            LOGGER.trace(s"$logPrefix Foc-index jump, index-ad-open logic $inxLogic")
            inxLogic
          }
        }
      } else {
        Future.successful(None)
      }
    }

    // Сначала запускать index, если есть в запросе:
    lazy val indexRaOptFut = _logicOpt2stateRespActionOptFut( indexLogicOptFut )

    /** qs после перехода в новый index.
      * Если перехода в index нет, то будут исходные qs. */
    lazy val qsAfterIndexFut: Future[MScQs] = {
      for {
        scIndexRespActionOpt <- indexRaOptFut
        indexLogicOpt <- indexLogicOptFut
      } yield {
        LOGGER.trace(s"$logPrefix scIndexRespActionOpt ? ${scIndexRespActionOpt.nonEmpty}")
        (for {
          scIndexRespAction   <- scIndexRespActionOpt
          if scIndexRespAction.acType ==* MScRespActionTypes.Index
          nodesResp = scIndexRespAction.search.get
          if !nodesResp.hasManyNodes
          ps <- nodesResp.nodes.headOption
          scIndexResp = ps.props
        } yield {
          val qs2 = qs.copy(
            search = MAdsSearchReq.rcvrId
              .set(scIndexResp.nodeId.toEsUuIdOpt)(qs.search),
            common = {
              MScCommonQs.locEnv
                .composeLens( MLocEnv.geoLoc )
                .set {
                  if (scIndexResp.nodeId.isEmpty) {
                    // Узел не задан. Попытаться достать координаты.
                    scIndexResp.geoPoint
                      .fold {
                        qs.common.locEnv.geoLoc
                      } { geoPoint =>
                        MGeoLoc(
                          geoPoint,
                          source = Some(MGeoLocSources.NodeInfo)
                        ) :: Nil
                      }
                  } else {
                    Nil
                  }
                }(
                  // Использовать indexLogin
                  indexLogicOpt.fold(qs)(_._qs).common
                )
            },
            // Надо ли сразу фокусировать кликнутую карточку после перехода в новый index?
            foc = for {
              foc0 <- qs.foc
              if {
                // Разрешить авто-фокус, если это запрос при запуске, т.е. в рамках пачки index+grid+foc:
                qs.grid.exists(_.focAfterJump contains[Boolean] true) ||
                // Разрешать авто-фокус при любом переходе куда угодно (выключено по итогам разговоров):
                ScConstants.Focused.AUTO_FOCUS_AFTER_FOC_INDEX
              }
            } yield {
              // если требуется авто-фокусировка, то снять флаг focIndex на всякий случай (для возможной от бесконечных переходов). В норме - это ни на что не должно влиять.
              (MScFocusArgs.indexAdOpen set None)(foc0)
            }
          )
          LOGGER.trace(s"$logPrefix Ads search after index qs2=$qs2")
          qs2
        }) getOrElse {
          // Не будет index'а в ответе, возвращаем исходный qs для дальнейших действий.
          LOGGER.trace(s"$logPrefix no new index, using origin qs.")
          qs
        }
      }
    }

    lazy val radioBeaconsCtxFut = qsAfterIndexFut.flatMap { scQs2 =>
      scAdSearchUtil.radioBeaconsSearch( scQs2.common.locEnv.beacons )
    }

    /** Собрать focused-логику, если она требуется. */
    def focLogicOptFut: Future[Option[scFocusedAds.FocusedLogicHttpV3]] = {
      for {
        // Нужно разобраться, какие параметры брать за основу в зависимости от флага в qs.
        qsAfterIndex <- qsAfterIndexFut
      } yield {
        for (focQs <- qsAfterIndex.foc) yield {
          LOGGER.trace(s"$logPrefix Focused logic active, focQs = $focQs")
          scFocusedAds.FocusedLogicHttpV3( qsAfterIndex )(_request)
        }
      }
    }


    /** Запустить focused-логику, когда это требуется запросом. */
    def focRaOptFut =
      _logicOpt2stateRespActionOptFut( focLogicOptFut )

    /** Запустить абстрактную логику на исполнение через её интерфейс. */
    private def _logicOpt2stateRespActionOptFut(logicOptFut: Future[Option[ScCtlUtil#LogicCommonT with IRespActionFut]]): Future[Option[MSc3RespAction]] = {
      val fut: Future[Option[MSc3RespAction]] = for {
        logicOpt  <- logicOptFut
        logic     = logicOpt.get
        ra        <- _logic2stateRespActionFut( logic )
      } yield {
        Some(ra)
      }
      fut.recover { case _: NoSuchElementException =>
        None
      }
    }

    /** Исполнить абстрактную логику и сохранить статистику через интерфейс логики. */
    private def _logic2stateRespActionFut(logic: IRespActionFut): Future[MSc3RespAction] = {
      val raFut = logic.respActionFut
      // Если logicCommon, то запустить в фоне сохранение статистики.
      // TODO Собирать единую статистику для всего uni-запроса, а не для каждого экшена?
      logic match {
        case logicCommon: ScCtlUtil#LogicCommonT =>
          logicCommon.saveScStat()
        case _ =>
          // do nothing
      }
      raFut
    }


    // Запустить сбор карточек плитки, если требуется:
    def gridAdsRaOptFut: Future[Option[MSc3RespAction]] = {
      val isWithGridFut = if (qs.grid.nonEmpty) {
        LOGGER.trace(s"$logPrefix qs.grid defined = ${qs.grid}")
        Future.successful( true )
      } else {
        // Проверить наличие index+focused в ответе, дополнив ответ автоматически.
        for (inxSwitchOpt <- focJumpToIndexNodeOptFut) yield {
          val r = inxSwitchOpt.isDefined
          if (r) LOGGER.trace(s"$logPrefix focJump allowed, will also return grid ads")
          r
        }
      }

      (for {
        isWithGrid  <- isWithGridFut
        if isWithGrid
        indexRaOpt  <- indexRaOptFut
        // Если пачка узлов возвращается, то нет смысла возвращать плитку - становится не ясно, к какому из узлов оно относится.
        // И тогда на клиент возвращаются карточки, размещённые просто на карте.
        if {
          val hasManyIndexes = indexRaOpt.exists(_.search.exists(_.nodes.lengthIs > 1))
          LOGGER.trace(s"$logPrefix hasManyIndexes?$hasManyIndexes, indexRa = ${indexRaOpt.orNull}")
          !hasManyIndexes
        }
        qs2         <- qsAfterIndexFut
        logic       = scAdsTile.TileAdsLogic(qs2, radioBeaconsCtxFut)(_request)
        respAction  <- _logic2stateRespActionFut( logic )
          // Обход перехватчика NSEE - пусть пойдёт по нормальному логгированию.
          .recover { case ex: NoSuchElementException =>
            throw new RuntimeException(ex)
          }
      } yield {
        LOGGER.trace(s"$logPrefix Search for grid ads => ${respAction.ads.iterator.flatMap(_.ads).size} ads")
        Some( respAction )
      })
        .recover { case ex =>
          if (ex.isInstanceOf[NoSuchElementException])
            LOGGER.trace(s"$logPrefix Future.filter returned false, isWithGrid?$isWithGridFut", ex)
          else
            LOGGER.warn(s"$logPrefix Grid respAction failed.", ex)
          None
        }
    }


    /** Запуск поиска узлов (в т.ч. тегов), если запрошен. */
    def searchRaOptFut: Future[Option[MSc3RespAction]] = {
      val futOpt = for {
        _ <- qs.nodes
      } yield {
        // Запрошен поиск узлов. Подготовить список искомых узлов.
        for {
          qs2         <- qsAfterIndexFut
          logic       = scSearch.ScSearchLogic(qs2.common.apiVsn)(qs2, radioBeaconsCtxFut, geoIpInfo)(_request)
          respAction  <- _logic2stateRespActionFut( logic )
        } yield {
          LOGGER.trace(s"$logPrefix Search nodes => ${respAction.search.iterator.flatMap(_.nodes).size} results")
          respAction
        }
      }
      FutureUtil.optFut2futOptPlain( futOpt )
    }

    /** Рассчёт контрольной суммы rcvrsMap для confUpdate или иных целей. */
    def rcvrsMapUrlArgsFut = advGeoRcvrsUtil.rcvrsMapUrlArgs()(ctx)


    /**
      * Попутно раздаются свежие данные для обновления данных в конфиге выдачи:
      * Изначально - текущую версию rcvrsMap.json, например.
      */
    private def confUpdateRaOptFut: Future[Option[MSc3RespAction]] = {
      // Не возвращать confUpdate при search-запросах.
      (for {
        // Решено, что надо вернуть свежие "новости".
        // Запустить рассчёт данных карты:
        rcvrsMapUrlArgs <- rcvrsMapUrlArgsFut

      } yield {
        val ra = MSc3RespAction(
          acType = MScRespActionTypes.ConfUpdate,
          confUpdate = Some(
            MScConfUpdate(
              rcvrsMap = Some(rcvrsMapUrlArgs),
            )
          )
        )
        Some(ra)
      })
        .recover { case ex: Throwable =>
          if (!ex.isInstanceOf[NoSuchElementException])
            LOGGER.error(s"$logPrefix Unable to confUpdate", ex)
          None
        }
    }


    /** Сборка JSON-ответа сервера. */
    def scRespFut: Future[MSc3Resp] = {
      val _indexRaOptFut      = indexRaOptFut
      val _gridAdsRaOptFut    = gridAdsRaOptFut
      val _focRaOptFut        = focRaOptFut
      val _searchRaOptFut     = searchRaOptFut
      val _confUpdateRaOptFut = confUpdateRaOptFut
      for {
        indexRaOpt      <- _indexRaOptFut
        gridAdsRaOpt    <- _gridAdsRaOptFut
        focRaOpt        <- _focRaOptFut
        searchRaOpt     <- _searchRaOptFut
        confUpdateRaOpt <- _confUpdateRaOptFut
      } yield {
        val respActions = (
          indexRaOpt ::
          gridAdsRaOpt ::
          focRaOpt ::
          searchRaOpt ::
          confUpdateRaOpt ::
          Nil
        )
          .flatten

        LOGGER.trace(s"$logPrefix Resp actions[${respActions.length}]: foc?${focRaOpt.nonEmpty} grid?${gridAdsRaOpt.nonEmpty} index?${indexRaOpt.nonEmpty} searchNodes?${searchRaOpt.nonEmpty}, total=${respActions.size}, confUpd?${confUpdateRaOpt.nonEmpty}")
        MSc3Resp(
          respActions = respActions
        )
      }
    }

  }


  /** Экшен Sc Public API, обрабатывающий запросы выдачи, которые не требуют авторизации юзера,
    * либо обрабатывают её самостоятельно.
    *
    * @param qs query string.
    * @return 200 OK + ответ сервера по всем запрошенным вещам.
    */
  def pubApi(qs: MScQs) = maybeAuth().async { implicit request =>
    // Разобрать qs, собрать на исполнение.
    val apiVsnMj = qs.common.apiVsn.majorVsn

    lazy val logPrefix = s"pubApi()#${System.currentTimeMillis()}:"
    LOGGER.trace(s"$logPrefix\n QS = $qs\n Raw qs = ${request.rawQueryString}")

    // Пробежаться по поддерживаемым версиям sc api:
    if (apiVsnMj ==* MScApiVsns.ReactSjs3.majorVsn) {
      // Скомпилировать все ответы воедино.
      val logic = ScPubApiLogicHttpV3( qs )(request)
      for {
        scResp <- logic.scRespFut
      } yield {
        corsUtil.withCorsIfNeeded(
          Ok( Json.toJson(scResp) )
            .cacheControl( 20 )
        )(logic.ctx)
      }

    } else {
      val msg = s"API not implemented for version ${qs.common.apiVsn}."
      LOGGER.debug(s"$logPrefix $msg remoteIP=${request.remoteClientAddress}")
      NotImplemented(msg)
    }
  }

}
