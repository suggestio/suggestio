package controllers.sc

import io.suggest.common.empty.OptionUtil
import OptionUtil.Implicits._
import io.suggest.common.fut.FutureUtil
import io.suggest.sc.{MScApiVsns, ScConstants}
import io.suggest.sc.sc3._
import util.acl.IMaybeAuth
import japgolly.univeq._
import io.suggest.es.model.MEsUuId.Implicits._
import io.suggest.geo.MGeoLoc
import io.suggest.model.n2.node.MNode
import models.req.IReq
import play.api.libs.json.Json
import util.showcase.IScUtil
import FutureUtil.Implicits._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.18 15:04
  * Description: Единое API для выдачи: разные запросы и их сложные комбинации в рамках только одного запроса.
  *
  * Это позволяет запрашивать, например, focused и grid карточки одновременно только одним запросом.
  *
  * Другой полезные случай: возможность запрашивтаь focused->index+grid+focused, чтобы за один запрос получался
  * переход в нужную выдачу с уже раскрытой карточкой за один запрос к Sc-контроллеру.
  */
trait ScUniApi
  extends ScIndex
  with ScAdsTile
  with ScIndexAdOpen
  with ScSearch
  with IMaybeAuth
  with IScUtil
{

  import mCommonDi.ec


  /** Логика Sc UApi.
    * Тело метода-экшена переусложнилось до полной невозможности им пользоваться и
    * его расширять.
    */
  case class ScPubApiLogicHttpV3(qs: MScQs)
                                (override implicit val _request: IReq[_]) extends LazyContext {

    // Разобрать qs, собрать на исполнение.
    lazy val logPrefix = s"PubApi#${System.currentTimeMillis()}:"


    /** Надо ли выполнять перескок focused => index в какой-то другой узел?
      * Да, если фокусировка активна, перескок разрешён, и проверка перескока вернула узел.
      */
    lazy val focJumpToIndexNodeOptFut: Future[Option[MNode]] = {
      val mnodeOptFutOpt = for {
        focQs <- qs.foc
        if focQs.focIndexAllowed
      } yield {
        // Запустить нормальную проверку на перескок в индекс.
        scUtil.isFocGoToProducerIndexFut( qs )
      }
      mnodeOptFutOpt.getOrNoneFut
    }


    /** Собрать index-логику, когда требуется. */
    lazy val indexLogicOptFut: Future[Option[ScIndexLogic]] = {
      if (qs.index.nonEmpty) {
        val logic = ScIndexLogic(qs)(_request)
        LOGGER.trace(s"$logPrefix Normal index-logic created: $logic")
        Future.successful( Some(logic) )
      } else if ( qs.foc.exists(_.focIndexAllowed) ) {
        for (toNnodeOpt <- focJumpToIndexNodeOptFut) yield {
          for (toNode <- toNnodeOpt) yield {
            val inxLogic = ScFocToIndexLogicV3(toNode, qs)( _request )
            LOGGER.trace(s"$logPrefix Foc-index jump detected, index-logic already here: $inxLogic")
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
      } yield {
        LOGGER.trace(s"$logPrefix scIndexRespActionOpt ? ${scIndexRespActionOpt.nonEmpty}")
        val qsAfterIndex = for {
          scIndexRespAction   <- scIndexRespActionOpt
          if scIndexRespAction.acType ==* MScRespActionTypes.Index
          nodesResp = scIndexRespAction.search.get
          if !nodesResp.hasManyNodes
          ps <- nodesResp.nodes.headOption
          scIndexResp = ps.props
        } yield {
          val qs2 = qs.copy(
            search = qs.search.copy(
              rcvrId = scIndexResp.nodeId.toEsUuIdOpt
            ),
            common = qs.common.copy(
              locEnv = qs.common.locEnv.copy(
                // Когда задан узел - координаты не нужны.
                geoLocOpt = OptionUtil.maybeOpt( scIndexResp.nodeId.isEmpty ) {
                  // Узел не задан. Попытаться достать координаты.
                  scIndexResp.geoPoint
                    .map( MGeoLoc(_) )
                    .orElse( qs.common.locEnv.geoLocOpt )
                }
              )
            ),
            // Надо ли сразу фокусировать кликнутую карточку после перехода в новый index?
            foc = for {
              foc0 <- qs.foc
              if ScConstants.Focused.AUTO_FOCUS_AFTER_FOC_INDEX
            } yield {
              // если требуется авто-фокусировка, то снять флаг focIndex на всякий случай (для возможной от бесконечных переходов). В норме - это ни на что не должно влиять.
              foc0.copy(
                focIndexAllowed = false
              )
            }
          )
          LOGGER.trace(s"$logPrefix Ads search after index qs2=$qs2")
          qs2
        }
        qsAfterIndex.getOrElse {
          // Не будет index'а в ответе, возвращаем исходный qs для дальнейших действий.
          LOGGER.trace(s"$logPrefix no new index, using origin qs.")
          qs
        }
      }
    }


    /** Собрать focused-логику, если она требуется. */
    def focLogicOptFut: Future[Option[FocusedLogicHttpV3]] = {
      for {
        // Нужно разобраться, какие параметры брать за основу в зависимости от флага в qs.
        qsAfterIndex <- qsAfterIndexFut
      } yield {
        for (focQs <- qsAfterIndex.foc) yield {
          LOGGER.trace(s"$logPrefix Focused logic active, focQs = $focQs")
          FocusedLogicHttpV3( qsAfterIndex )(_request)
        }
      }
    }


    /** Запустить focused-логику, когда это требуется запросом. */
    def focRaOptFut =
      _logicOpt2stateRespActionOptFut( focLogicOptFut )

    /** Запустить абстрактную логику на исполнение через её интерфейс. */
    private def _logicOpt2stateRespActionOptFut(logicOptFut: Future[Option[LogicCommonT with IRespActionFut]]): Future[Option[MSc3RespAction]] = {
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
        case logicCommon: LogicCommonT =>
          logicCommon.saveScStat()
        case _ =>
          // do nothing
      }
      raFut
    }


    // Запустить сбор карточек плитки, если требуется:
    def gridAdsRaOptFut: Future[Option[MSc3RespAction]] = {
      val isWithGridFut = if (qs.common.searchGridAds.nonEmpty) {
        Future.successful( true )
      } else {
        // Проверить наличие index+focused в ответе, дополнив ответ автоматически.
        for (inxSwithOpt <- focJumpToIndexNodeOptFut) yield {
          inxSwithOpt.isDefined
        }
      }

      val fut = for {
        isWithGrid  <- isWithGridFut
        if isWithGrid
        qs2         <- qsAfterIndexFut
        logic       = TileAdsLogic(qs2)(_request)
        respAction  <- _logic2stateRespActionFut( logic )
      } yield {
        LOGGER.trace(s"$logPrefix Search for grid ads => ${respAction.ads.iterator.flatMap(_.ads).size} ads")
        respAction
      }

      fut.toOptFut
    }


    /** Запуск поиска тегов, если запрошен. */
    def searchRaOptFut: Future[Option[MSc3RespAction]] = {
      val futOpt = for {
        _ <- qs.common.searchNodes
      } yield {
        for {
          qs2         <- qsAfterIndexFut
          logic       = ScSearchLogic(qs2.common.apiVsn)(qs2)(_request)
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
      val someRaOrNseeFut: Future[Option[MSc3RespAction]] = for {
        // 2018-11-15 Только на index-ответы возвращать конфиг. Апдейты не часты, поэтому возвращать их на каждый чих не нужно.
        // В будущем можно чаще/реже возвращать всё это добро.
        indexLogicOpt <- indexLogicOptFut
        if indexLogicOpt.nonEmpty

        // Решено, что надо вернуть свежие "новости".
        // Запустить рассчёт данных карты:
        rcvrsMapUrlArgs <- rcvrsMapUrlArgsFut

      } yield {
        val ra = MSc3RespAction(
          acType = MScRespActionTypes.ConfUpdate,
          confUpdate = Some(
            MScConfUpdate(
              rcvrsMap = Some(rcvrsMapUrlArgs)
            )
          )
        )
        Some(ra)
      }

      someRaOrNseeFut.recover { case ex: Throwable =>
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
        Ok( Json.toJson(scResp) )
          .cacheControl( 20 )
      }

    } else {
      val msg = s"API not implemented for version ${qs.common.apiVsn}."
      LOGGER.debug(s"$logPrefix $msg remoteIP=${request.remoteClientAddress}")
      NotImplemented(msg)
    }
  }

}
