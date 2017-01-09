package controllers.sc

import models.MNode
import models.mlu.{MLookupMode, MLookupModes}
import models.msc._
import models.msc.resp._
import models.req.IReq
import play.api.libs.json.Json
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
  * - index-выдачу другого узла.
  * - команду для перехода по внешней ссылке.
  *
  *
  * 2016.may.26
  * Изначально в focused v2 был search-поиск focused-карточек в gen-порядке на основе limit/offset,
  * т.е. что-то наподобии v1, но сложнее. Однако потом возникли проблемы:
  * - При десериализации foc-состояния из URL нет данных о limit/offset.
  * - При сдвиге выдачи из-за снятия/размещения карточек в выдаче.
  * - При погрешностях random-сортировки elasticsearch.
  * - При нештатной ситуации из-за какой-либо ошибки.
  *
  * Решено было, чтобы сервер сам каждый раз вычислял сегмент focused-цепочки на основе id карточки
  * и направления поиска. Это также унифицировало ряд вещей: десериализацию из URL и просто листание (навигацию).
  */

trait ScFocusedAdsV2
  extends ScFocusedAds
  with IN2NodesUtilDi
{

  protected[this] case class NodeIdIndexed(nodeId: String, index: Int) {
    override def toString: String = {
      nodeId + "#" + index
    }
  }

  protected[this] case class AdsLookupRes(ids: Seq[NodeIdIndexed], total: Int)

  /** Интерфейс результата анализа узлов и направления lookup'а внутри _doAdIdsLookup(). */
  private trait ILookupRes {
    def fadIds2     : Seq[NodeIdIndexed]
    def shouldNext  : Boolean
  }


  import mCommonDi._


  /** Общая логика обработки focused-выдачи v2. */
  abstract class FocusedLogicV2 extends FocusedAdsLogic {

    /** Число-отметка текущей логики обработки запроса. */
    private val _currTimeMs = System.currentTimeMillis()


    /** Асинхронный результат поиска сегмента карточек. */
    lazy val adIdsLookupResFut: Future[AdsLookupRes] = {
      if (_qs.search.hasAnySearchCriterias) {
        mAdsSearchFut.flatMap { mNodeSearch =>
          _doAdIdsLookup(neededCount = mNodeSearch.limit)
        }
      } else {
        LOGGER.info(s"$logPrefix v2: not ad-search criterias found, skipping ids lookup.")
        Future.successful( AdsLookupRes(Nil, total = 0) )
      }
    }

    /** Метод рекурсивного поиска сегмента последовательности id карточек в пакетных выхлопах elasticsearch. */
    def _doAdIdsLookup(adId: String = _qs.lookupAdId, lm: MLookupMode = _qs.lookupMode,
                       neededCount: Int, limit1: Int = 50, offset1: Int = 0, tryN: Int = 1): Future[AdsLookupRes] = {

      assert(tryN <= 20)

      // Необходимо поискать id focused-карточек в корректном порядке с начала списка.
      val fadsIdsSearchQs = _qs.search.copy(
        limitOpt  = Some(limit1),
        offsetOpt = Some(offset1)
      )

      // TODO Далее какой-то быдлокод с реализацией нетривиальной выборки сегмента последовательности foc-карточек.
      scAdSearchUtil.qsArgs2nodeSearch(fadsIdsSearchQs).flatMap { msearch =>

        val fadIdsFut = mNodes.dynSearchIds(msearch)
        val __limit = msearch.limit

        // Собрать сегмент ids, идущих после необходимого id, длины max.
        def takeIdsAfter(iter: Iterator[NodeIdIndexed], max: Int = __limit): Seq[NodeIdIndexed] = {
          iter
            .dropWhile(_.nodeId != adId)
            .slice(1, max + 1) // Взять только необходимое кол-во элементов, если оно там есть.
            .toSeq
        }

        // Логгируем рекурсию вместе с номером попытки.
        lazy val logPrefix = s"_doAdLookup(${_currTimeMs}#$tryN): "
        LOGGER.trace(s"$logPrefix [$adId] ${lm.toVisualString}, need=$neededCount limit=$limit1 offset=$offset1")

        fadIdsFut.flatMap { fadIds =>

          lazy val hasAdId = fadIds.contains(adId)
          lazy val fadIdsSize = fadIds.size
          lazy val fadIdsHasNexts = fadIdsSize >= limit1

          class SimpleLookupRes(override val fadIds2: Seq[NodeIdIndexed]) extends ILookupRes {
            override def shouldNext = fadIdsHasNexts
          }

          // Добавить индексы элементам итератора с поправкой на текущий offset1.
          // Получаться порядковые номера карточек: 0, 1, 2, ...
          val fadsIdsInxed = fadIds.iterator
            .zipWithIndex
            .map { case (id, index0) =>
              NodeIdIndexed(id, offset1 + index0)
            }
            .toSeq

          def fadIdsIter = fadsIdsInxed.iterator
          def fadIdsReverseIter = fadsIdsInxed.reverseIterator

          LOGGER.trace(s"$logPrefix Found $fadIdsSize ids: [${fadIds.mkString(" ")}]")

          val lRes: ILookupRes = lm match {

            // Сбор id карточек после указанной adId
            case MLookupModes.After =>
              // При просмотре элементов вправо можно переходить на следующую выборку, если длина текущей упирается в лимит.
              new SimpleLookupRes(takeIdsAfter(fadIdsIter))

            // Сбор id карточек, предшествующих указанной карточке.
            case MLookupModes.Before =>
              // Всё просто: берём и собираем в обратном порядке.
              val _fadIds2 = takeIdsAfter(fadIdsReverseIter)
                .reverse
              new SimpleLookupRes(_fadIds2) {
                override def shouldNext: Boolean = {
                  // Была ли найдена текущая карточка? Да, если список предшествующих ей непуст ИЛИ же пуст т.к. искомая карточка первая в списке.
                  _fadIds2.nonEmpty || !fadIds.headOption.contains(adId)
                }
              }

            // Сбор id карточек вокруг желаемой карточки.
            case MLookupModes.Around =>
              if (hasAdId) {
                val b = Seq.newBuilder[NodeIdIndexed]
                // Выбрать id'шники до указанной.
                val needCountBefore = (__limit - 1) / 2
                val beforeIds = takeIdsAfter(fadIdsReverseIter, needCountBefore)
                  .reverse
                b ++= beforeIds

                // Выбрать id'шники после указанного.
                val needCountAfter = __limit - 1 - needCountBefore
                val afterIds = takeIdsAfter(fadIdsIter, needCountAfter)

                // Включить текущий id'шник.
                val currIndex = beforeIds.lastOption
                  .map(_.index + 1)
                  .orElse {
                    afterIds.headOption.map(_.index - 1)
                  }
                  .getOrElse(0)
                b += NodeIdIndexed(adId, currIndex)

                // Включить id'шники после текущего.
                b ++= afterIds

                // Подготовить результат.
                new SimpleLookupRes(b.result()) {
                  override def shouldNext: Boolean = {
                    needCountAfter > afterIds.size && fadIdsHasNexts
                  }
                }

              } else {
                // Нет вообще искомой карточки среди упомянутых.
                new SimpleLookupRes(Nil)
              }
          }

          val fadIds2 = lRes.fadIds2
          val foundCount = fadIds2.size
          LOGGER.trace(s"$logPrefix ids[$foundCount] = [${fadIds2.mkString(" ")}]")

          // Дедубликация кода возврата результата без дальнейшего погружения в рекурсию.
          def resFut = Future.successful {
            AdsLookupRes(fadIds2, fadIds.total.toInt)
          }

          if (foundCount < neededCount) {
            // Найдено недостаточно элементов. Или их просто нет, или их можно найти в следующей порции.

            if (lRes.shouldNext) {
              // Требуется переход на следующую порцию для поиска.
              // Новый режим поиска: если сбор элементов уже начался, то только after.
              val lm2 = if (fadIds2.nonEmpty) MLookupModes.After else lm
              val neededCount2 = neededCount - foundCount

              // Анти-сдвиг вперёд, чтобы не пропустить какие-нибудь карточки. Выжен при before-выборке.
              // 2 -- Нужно подавлять возможные погрешности поиска (+- 1-2 элемента).
              var backOffset = 2
              // А если идёт before/around-поиск, то надо отработать случай, когда искомая карточка идёт самой первой или около того.
              if (lm2.withBefore) {
                backOffset = Math.max(
                  backOffset,
                  if (lm2.withAfter) neededCount2 / 2 + 1 else neededCount2
                )
              }

              // Надо поискать ещё в след.порции выдачи, запустить поиск.
              val fut2 = _doAdIdsLookup(
                adId = fadIds2.lastOption.fold(adId)(_.nodeId),
                lm = lm2,
                neededCount = neededCount2,
                limit1 = limit1,
                offset1 = offset1 + limit1 - backOffset,
                tryN = tryN + 1
              )
              // Добавить текущие найденные элементы в итоговый результат.
              for (nextRes <- fut2) yield {
                nextRes.copy(
                  ids = fadIds2 ++ nextRes.ids
                )
              }

            } else {
              // Некуда больше искать. Вернуть то, что уже есть.
              resFut
            }

          } else {
            // Найдено достаточно элементов. Продолжать не нужно.
            resFut
          }
        }
      }

    }


    override def _firstAdIndexFut: Future[Int] = {
      for (res <- adIdsLookupResFut) yield {
        res.ids.headOption.fold(0)(_.index) //+ 1
      }
    }


    /** 2016.jun.1 Поиск самих focused-карточек в v2 отсутствует.
      * Ищутся только цепочки id, из них выделяются короткие сегменты, которые читаются через multiget. */
    override def mads1Fut: Future[Seq[MNode]] = {
      Future.successful( Nil )
    }


    override def firstAdIdsFut: Future[Seq[String]] = {
      for (res <- adIdsLookupResFut) yield {
        for (idIndexed <- res.ids) yield {
          idIndexed.nodeId
        }
      }
    }

  }


  /** Реализация v2-логики. */
  protected class FocusedLogicHttpV2(override val _qs: MScAdsFocQs)
                                    (override implicit val _request: IReq[_])
    extends FocusedLogicV2
    with FocusedAdsLogicHttp
  {

    // При рендере генерятся контейнеры render-результатов, который затем конвертируются в json.
    override type OBT = MFocRenderResult

    override def renderOuterBlock(args: AdBodyTplArgs): Future[OBT] = {
      val fullArgsFut = focAdsRenderArgsFor(args)

      val bodyFut = renderBlockHtml(args)
        .map { htmlCompressUtil.html2str4json }

      val controlsFut = for {
        fullArgs <- fullArgsFut
      } yield {
        htmlCompressUtil.html2str4json(
          _controlsTpl(fullArgs)(ctx)
        )
      }

      val producerId = n2NodesUtil.madProducerId(args.brArgs.mad).get

      for {
        body      <- bodyFut
        controls  <- controlsFut
      } yield {
        val humanIndex1 = args.index
        MFocRenderResult(
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
        val resp = MScResp(
          scActions = Seq(
            MScRespAction(
              acType = MScRespActionTypes.AdsFoc,
              adsFoc = Some( MScRespAdsFoc(
                fads        = blockHtmls,
                totalCount  = madsCount,
                styles      = _styles
              ))
            )
          )
        )
        Ok( Json.toJson(resp) )
      }
    }

  }  // class FocusedLogicHttpV2


  // Добавить поддержку v2-логики в getLogic()
  override def getLogicFor(qs: MScAdsFocQs)
                          (implicit request: IReq[_]): FocusedAdsLogicHttp = {
    if (qs.apiVsn.majorVsn == MScApiVsns.Sjs1.majorVsn) {
      new FocusedLogicHttpV2(qs)
    } else {
      super.getLogicFor(qs)
    }
  }

}
