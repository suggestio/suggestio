package controllers.sc

import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.sc.MScApiVsns
import io.suggest.sc.sc3.{MSc3Resp, MSc3RespAction, MScQs}
import util.acl.IMaybeAuth
import japgolly.univeq._
import io.suggest.es.model.MEsUuId.Implicits._
import io.suggest.geo.MGeoLoc
import models.req.IReq
import play.api.libs.json.Json
import play.api.mvc.Result

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
  with ScTags
  with IMaybeAuth
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


    /** Логика фокусировки - в первую очередь, т.к. она может определять index-логику. */
    lazy val focOrIndexLogicFutOpt = for (focQs <- qs.foc) yield {
      for {
        // Получить данные по продьюсеру, если переход в другую выдачу.
        focToIndexProducerOpt <- {
          if (focQs.focAfterIndex) {
            Future.successful(None)
          } else {
            _isFocGoToProducerIndexFut( qs )
          }
        }

        // Если фокусировка после index, то надо дождаться результатов index и пробрасывать их в логику работы.
        qs2 <- {
          if (focQs.focAfterIndex) {
            for (inxLogicOpt <- adsSearchAfterIndexQsOptFut) yield {
              inxLogicOpt
                .map { qsAfterIndex =>
                  val qss2 = qsAfterIndex
                    .withFoc( qsAfterIndex.foc )
                  LOGGER.trace(s"$logPrefix Will use qs from indexLogic:\n old = $qs\n new = $qss2")
                  qss2
                }
                .getOrElse {
                  LOGGER.debug(s"$logPrefix inx logic missing, but focAfterIndex set")
                  qs
                }
            }
          } else {
            Future.successful( qs )
          }
        }
      } yield {
        val logic = focToIndexProducerOpt.fold [LogicCommonT with IRespActionFut] {
          // Фокусируемся на запрошенной карточке:
          LOGGER.trace(s"$logPrefix Focus to $focQs")
          FocusedLogicHttpV3( qs2 )(_request)
        } { producerNode =>
          // Переход в index-выдачу продьюсера:
          LOGGER.trace(s"$logPrefix Foc from ad#${focQs.lookupAdId} to index to ${producerNode.idOrNull}")
          ScFocToIndexLogicV3(producerNode, qs2)(_request)
        }
        focToIndexProducerOpt -> logic
      }
    }


    /** Собрать focused-логику, если она требуется. */
    def focLogicOptFut: Future[Option[FocusedLogicHttpV3]] = {
      val resOpt = for {
        focQs <- qs.foc
        // .get: Пусть будет ошибка, если логика двух этих методов начнёт разъезжаться.
        focOrIndexLogicFut = focOrIndexLogicFutOpt.get
      } yield {
        LOGGER.trace(s"$logPrefix focQs=$focQs")
        for {
          (_, focOrIndexLogic) <- focOrIndexLogicFut
        } yield {
          LOGGER.trace(s"$logPrefix focOrIndexLogic = $focOrIndexLogic")
          focOrIndexLogic match {
            case l: FocusedLogicHttpV3 =>
              Some(l)
            // Если запрошен foc->index, то при наличии принудительного foc-флага вернуть и focused-логику тоже.
            case inxLogic: ScIndexLogic =>
              // Собрать focused-логику, которая будет после focus-index перескока:
              OptionUtil.maybe( focQs.focAfterIndex ) {
                val l = FocusedLogicHttpV3( inxLogic._qs )(_request)
                LOGGER.trace(s"$logPrefix Focused logic contains index logic and focAfterIndex=true. Creating after-index focus logic:\n $l")
                l
              }
            // Should never happen
            case other =>
              LOGGER.error(s"$logPrefix Unexpected focusing logic: $other")
              None
          }
        }
      }
      // Раскрыть внешний Option: т.к. Option уже внутри Future.
      resOpt.getOrElse( Future.successful(None) )
    }


    /** Запустить focused-логику, когда это требуется запросом. */
    def focRespActionOptFut = _logicOpt2stateRespActionOptFut( focLogicOptFut )

    /** Запустить абстрактную логику на исполнение через её интерфейс. */
    private def _logicOpt2stateRespActionOptFut(logicOptFut: Future[Option[LogicCommonT with IRespActionFut]]): Future[Option[MSc3RespAction]] = {
      val fut = for {
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
    private def _logic2stateRespActionFut(logic: LogicCommonT with IRespActionFut): Future[MSc3RespAction] = {
      val raFut = logic.respActionFut
      logic.saveScStat()
      raFut
    }


    /** Собрать index-логику, когда требуется. */
    lazy val indexLogicOptFut: Future[Option[ScIndexLogic]] = {
      if (qs.index.nonEmpty) {
        val logic = ScIndexLogicHttp(qs)(_request)
        LOGGER.trace(s"$logPrefix Normal index-logic created: $logic")
        Future.successful( Some(logic) )
      } else if ( qs.foc.exists(_.focIndexAllowed) ) {
        for {
          (_, focLogic) <- focOrIndexLogicFutOpt.get
        } yield {
          focLogic match {
            case inxLogic: ScIndexLogic =>
              LOGGER.trace(s"$logPrefix Foc-index jump detected, index-logic already here: $inxLogic")
              Some( inxLogic )
            case _ => None
          }
        }
      } else {
        Future.successful(None)
      }
    }

    // Сначала запускать index, если есть в запросе:
    lazy val indexRespActionOptFut = _logicOpt2stateRespActionOptFut( indexLogicOptFut )


    // Когда запрашивается отложенная плитка или теги, требуются параметры из индекса.
    lazy val adsSearchAfterIndexQsOptFut: Future[Option[MScQs]] = for {
      scIndexRespActionOpt <- indexRespActionOptFut
    } yield {
      for {
        scIndexRespAction   <- scIndexRespActionOpt
        scIndexResp         <- scIndexRespAction.index
      } yield {
        val qs2 = qs.copy(
          search = qs.search.copy(
            rcvrId = scIndexResp.nodeId.toEsUuIdOpt
          ),
          common = qs.common.copy(
            locEnv = qs.common.locEnv.copy(
              geoLocOpt = if (scIndexResp.nodeId.nonEmpty) {
                // Задан узел - координаты не нужны.
                None
              } else {
                // Узел не задан. Попытаться достать координаты.
                scIndexResp.geoPoint
                  .map( MGeoLoc(_) )
                  .orElse( qs.common.locEnv.geoLocOpt )
              }
            )
          )
        )
        LOGGER.trace(s"$logPrefix Ads search after index qs2=$qs2")
        qs2
      }
    }

    /** Вернуть qs, который нужно использовать на следующих стадиях, нуждающихся в qs. */
    private def __qsMaybeWaitIndex(isWaitIndex: Boolean): Future[MScQs] = {
      if (isWaitIndex) adsSearchAfterIndexQsOptFut.map(_.get)
      else Future.successful(qs)
    }


    // Запустить сбор карточек плитки, если требуется:
    def gridAdsRespActionFutOpt: Option[Future[MSc3RespAction]] = for {
      isGridAdsAfterIndex <- qs.common.searchGridAds
    } yield {
      for {
        qs2         <- __qsMaybeWaitIndex(isGridAdsAfterIndex)
        logic       = TileAdsLogicV3(qs2)(_request)
        respAction  <- _logic2stateRespActionFut( logic )
      } yield {
        LOGGER.trace(s"$logPrefix Search for grid ads, waitIndex=$isGridAdsAfterIndex\n res = $respAction")
        respAction
      }
    }


    /** Запуск поиска тегов, если запрошен. */
    def tagsRespActionFutOpt: Option[Future[MSc3RespAction]] = for {
      isTagsAfterIndex <- qs.common.searchTags
    } yield {
      for {
        qs2         <- __qsMaybeWaitIndex(isTagsAfterIndex)
        logic       = ScTagsLogicV3(qs2)(_request)
        respAction  <- _logic2stateRespActionFut( logic )
      } yield {
        LOGGER.trace(s"$logPrefix Search tags. AfterIndex?${isTagsAfterIndex}\n res = $respAction")
        respAction
      }
    }


    /** Сборка JSON-ответа сервера. */
    def scRespFut: Future[MSc3Resp] = {
      val _focRespActionOptFut = focRespActionOptFut
      val _gridAdsRespActionOptFut = FutureUtil.optFut2futOptPlain( gridAdsRespActionFutOpt )
      val _indexRespActionOptFut = indexRespActionOptFut
      val _tagsRespActionOptFut = FutureUtil.optFut2futOptPlain( tagsRespActionFutOpt )
      for {
        focRaOpt        <- _focRespActionOptFut
        gridAdsRaOpt    <- _gridAdsRespActionOptFut
        indexRaOpt      <- _indexRespActionOptFut
        tagsRaOpt       <- _tagsRespActionOptFut
      } yield {
        val respActions = (
          indexRaOpt ::
          gridAdsRaOpt ::
          focRaOpt ::
          tagsRaOpt ::
          Nil
        ).flatten
        LOGGER.trace(s"$logPrefix Resp actions: foc?${focRaOpt.nonEmpty} grid?${gridAdsRaOpt.nonEmpty} index?${indexRaOpt.nonEmpty} tags?${tagsRaOpt.nonEmpty}, total=${respActions.size}")
        MSc3Resp(
          respActions = respActions
        )
      }
    }


    /** Сборка полноценного HTTP-ответа. */
    def resultFut: Future[Result] = {
      if (qs.foc.exists(_.focIndexAllowed) && qs.index.nonEmpty) {
        LOGGER.debug(s"$logPrefix Index args defined, but focJumpAllowed too. Only one is possible. qs=$qs")
        BadRequest

      } else if (qs.foc.exists(_.focIndexAllowed) && qs.index.nonEmpty) {
        LOGGER.debug(s"$logPrefix focIndexAllowed and index args defined both, but only one of two is possible. qs=$qs")
        BadRequest

      } else if (qs.common.apiVsn.majorVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
        // Скомпилировать все ответы воедино.
        for {
          scResp <- scRespFut
        } yield {
          Ok( Json.toJson(scResp) )
            .cacheControl( 20 )
        }

      } else {
        val msg = s"API not implemented for version ${qs.common.apiVsn}."
        LOGGER.debug(s"$logPrefix $msg remoteIP=${_request.remoteClientAddress}")
        NotImplemented(msg)
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
