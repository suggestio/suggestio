package controllers.sc

import io.suggest.sc.ScConstants.Focused
import models.jsm.FocusedAdsResp2
import models.msc._
import models.req.IReq
import play.api.mvc.Result
import util.n2u.IN2NodesUtilDi
import views.html.sc.foc._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.06.15 18:12
 * Description: Логика Focused Ads API v2, которая претерпела значителньые изменения в сравнении с API v1.
 *
 * Все карточки рендерятся одним списком json-объектов, которых изначально было два типа:
 * - Focused ad: _focusedAdTpl.
 * - Full focused ad: _focusedAdsTpl
 * Этот неоднородный массив отрабатывается конечным автоматом на клиенте, который видя full-часть понимает,
 * что последующие за ней не-full части относяться к этому же продьюсеру.
 *
 * Разные куски списка могут прозрачно склеиваться.
 *
 * Так же, сервер может вернуть вместо вышеописанного ответа:
 * - index выдачу другого узла.
 * - команду для перехода по внешней ссылке.
 */
trait ScFocusedAdsV2
  extends ScFocusedAds
  with IN2NodesUtilDi
{

  import mCommonDi._

  /** Реализация v2-логики. */
  protected class FocusedLogicHttpV2(override val _adSearch: FocusedAdsSearchArgs)
                                    (override implicit val _request: IReq[_])
    extends FocusedAdsLogicHttp
    with NoBrAcc
  {

    override def apiVsn = MScApiVsns.Sjs1

    // ! TODO Костыли отработки withoutIds на последующих запросах оказались кривыми костылями. ИХ НАДО УДАЛИТЬ если планы не изменятся.
    // ! Необходимо сделать всё нормально:
    // ! Для случаев focused ad с неизвестным offset система сначала сама определяла бы позицию искомой карточки
    // ! в выдаче на основе данных от mNodes.dynSearchIds(). Затем уже запускалась логика обработки focused-запроса v2.
    // ! На стороне sc нужно слать запрос без offset, (возможно и без limit?), но с принудительным firstAdIds например.


    /** Критерии поиска focused-карточек. */
    override lazy val _mads1SearchFut: Future[FocusedAdsSearchArgs] = {
      val sargsFut0 = super._mads1SearchFut
      // Если запрошена необходимость определения параметров foc-выдачи, но сделать это и внести результаты в поиск.
      _adSearch.focAdIdLookup.fold {
        sargsFut0

      } { lookupAdId =>
        lazy val logPrefix = s"_mads1SearchFut[${System.currentTimeMillis}]: "
        LOGGER.trace(s"$logPrefix adId[$lookupAdId] edges=${_adSearch.outEdges}")

        // Запустить поиск оффсета карточки в focused-выдаче...
        _adIdLookupCurrIndex().flatMap { currIndexOpt =>
          // Проанализировать найденный currIndex, если найден.
          currIndexOpt.fold {
            // Нет карточки в focused-выдаче. TODO Вернуть дефолтовую выдачу? Или что тут надо делать?
            LOGGER.warn(s"_mads1SearchFut: offset not found for ad[$lookupAdId].")
            sargsFut0

          } { currIndex =>
            // Вычислен currIndex искомой карточки в focused-выдаче. Вычислить теперь limit и offset для запросов.
            val withPrevAd  = Focused.isWithPrevAd( currIndex )
            val limit2      = Focused.getLimit( withPrevAd )
            val offset2     = Focused.currIndex2Offset(currIndex, withPrevAd)

            LOGGER.trace(s"$logPrefix foc index => $currIndex; +prevAd=$withPrevAd focusing on $offset2+$limit2")

            // Собрать новый search args.
            for (sargs0 <- sargsFut0) yield {
              new FocusedAdsSearchArgsWrapperImpl {
                override def _dsArgsUnderlying  = sargs0
                override def limit              = limit2
                override def offset             = offset2
              }
            }
          }
        }
      }  // focAdIdLookup.fold()(...)
    }

    /** Поиск offset'а указанной карточки для параметров focused-выдачи под указанную карточку. */
    private def _adIdLookupCurrIndex(limit1: Int = 100, offset1: Int = 0): Future[Option[Int]] = {
      // Доп.защита от возможной бесконечной рекурсии в случае программной ошибки.
      if (offset1 > 1000 || offset1 < 0) {
        LOGGER.error(s"_adIdLookupCurrIndex($limit1, $offset1): Offset too large, stop lookup, ${_request.uri} from ${_request.remoteAddress}.")
        Future.successful(None)

      } else {
        // Необходимо поискать id focused-карточек в корректном порядке с начала списка.
        val fadsIdsSearch = new FocusedAdsSearchArgsWrapperImpl {
          override def _dsArgsUnderlying = _adSearch
          override def limit      = limit1
          override def limitOpt   = Some(limit1)
          override def offset     = offset1
          override def offsetOpt  = Some(offset1)
        }
        val fadIdsFut = mNodes.dynSearchIds(fadsIdsSearch)

        fadIdsFut.flatMap { fadIds =>
          // Попытаться найти id искомой карточки среди результатов.
          val focAdId = _adSearch.focAdIdLookup.get
          fadIds
            .iterator
            .zipWithIndex
            .find { case (adId, i) =>
              focAdId == adId
            }
            .map(_._2)
            .fold [Future[Option[Int]]] {
              // Если карточка не найдена в списках, но ещё есть куда искать, то поискать ещё.
              if (fadIds.length >= limit1) {
                _adIdLookupCurrIndex(
                  limit1  = limit1,
                  offset1 = offset1 + limit1
                )
              } else {
                Future.successful( None )
              }
            } { i =>
              Future.successful( Some(i) )
            }
        }
      }
    }


    // При рендере генерятся контейнеры render-результатов, который затем конвертируются в json.
    override type OBT = FocRenderResult

    override def renderOuterBlock(args: AdBodyTplArgs): Future[OBT] = {
      val fullArgsFut = focAdsRenderArgsFor(args)

      val bodyFut = renderBlockHtml(args)
        .map { htmlCompressUtil.html2str4json }

      val controlsFut = for {
        fullArgs <- fullArgsFut
      } yield {
        htmlCompressUtil.html2str4json(
          _controlsTpl(fullArgs)
        )
      }

      val producerId = n2NodesUtil.madProducerId(args.brArgs.mad).get

      for {
        body      <- bodyFut
        controls  <- controlsFut
      } yield {
        val humanIndex1 = args.index
        FocRenderResult(
          madId       = args.brArgs.mad.id.get,
          body        = body,
          controls    = controls,
          producerId  = producerId,
          humanIndex  = humanIndex1,
          index       = humanIndex1 - 1
        )
      }
    }


    /** Сборка HTTP-ответа APIv2. */
    override def resultFut: Future[Result] = {
      val _blockHtmlsFut = blocksHtmlsFut

      val _stylesFut = jsAdsCssFut
        .map(htmlCompressUtil.txt2str)

      for {
        madsCount   <- madsCountIntFut
        blockHtmls  <- _blockHtmlsFut
        _styles     <- _stylesFut
      } yield {
        val resp = FocusedAdsResp2(blockHtmls, madsCount, _styles)
        Ok(resp.toJson)
      }
    }

  }


  // Добавить поддержку v2-логики в getLogic()
  override def getLogicFor(adSearch: FocusedAdsSearchArgs)
                          (implicit request: IReq[_]): FocusedAdsLogicHttp = {
    if (adSearch.apiVsn == MScApiVsns.Sjs1) {
      new FocusedLogicHttpV2(adSearch)
    } else {
      super.getLogicFor(adSearch)
    }
  }

}
