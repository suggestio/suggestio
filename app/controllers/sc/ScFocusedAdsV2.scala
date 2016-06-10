package controllers.sc

import models.MNode
import models.jsm.FocusedAdsResp2
import models.mlu.{MLookupMode, MLookupModes}
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

  import mCommonDi._

  /** Реализация v2-логики. */
  protected class FocusedLogicHttpV2(override val _adSearch: FocusedAdsSearchArgs)
                                    (override implicit val _request: IReq[_])
    extends FocusedAdsLogicHttp
    with NoBrAcc
  {

    override def apiVsn = MScApiVsns.Sjs1


    case class NodeIdIndexed(nodeId: String, index: Int) {
      override def toString: String = {
        nodeId + "#" + index
      }
    }
    case class AdsLookupRes(ids: Seq[NodeIdIndexed], total: Int)


    /** Асинхронный результат поиска сегмента карточек. */
    lazy val adIdsLookupResFut = _doAdIdsLookup()

    /** Метод рекурсивного поиска сегмента последовательности id карточек в пакетных выхлопах elasticsearch. */
    def _doAdIdsLookup(adId: String = _adSearch.lookupAdId, lm: MLookupMode = _adSearch.lookupMode,
                       neededCount: Int = _adSearch.limit, limit1: Int = 50, offset1: Int = 0): Future[AdsLookupRes] = {

      // Необходимо поискать id focused-карточек в корректном порядке с начала списка.
      val fadsIdsSearch = new FocusedAdsSearchArgsWrapperImpl {
        override def _dsArgsUnderlying = _adSearch
        override def limit      = limit1
        override def limitOpt   = Some(limit1)
        override def offset     = offset1
        override def offsetOpt  = Some(offset1)
      }
      val fadIdsFut = mNodes.dynSearchIds(fadsIdsSearch)

      // Собрать id после необходимого id с минимальными затратами ресурсов.
      def takeIdsAfter(iter: Iterator[NodeIdIndexed], max: Int = _adSearch.limit): Seq[NodeIdIndexed] = {
        iter
          .dropWhile(_.nodeId != adId)
          .slice(1, max + 1)                      // Взять только необходимое кол-во элементов, если оно там есть.
          .toSeq
      }

      // Добавить индексы элементам итератора с поправкой на текущий offset1.
      // Получаться порядковые номера карточек: 0, 1, 2, ...
      def offsetIndexed(iter: Iterator[String], sign: Boolean): Iterator[NodeIdIndexed] = {
        val sign1 = if (sign) 1 else -1
        for ((id, index0) <- iter.zipWithIndex) yield {
          NodeIdIndexed(id, offset1 + sign1*index0)
        }
      }

      lazy val logPrefix = s"_doAdLookup(${System.currentTimeMillis}): "
      LOGGER.trace(s"$logPrefix [$adId] $lm, need=$neededCount limit=$limit1 offset=$offset1")

      fadIdsFut.flatMap { fadIds =>

        lazy val hasAdId = fadIds.contains( adId )
        lazy val fadIdsSize = fadIds.size
        lazy val fadIdsHasNexts = fadIdsSize >= limit1
        def fadIdsHasNextsF() = fadIdsHasNexts

        def fadIdsIter = offsetIndexed(fadIds.iterator, sign = true)
        def fadIdsReverseIter = offsetIndexed(fadIds.reverseIterator, sign = false)

        LOGGER.trace(s"$logPrefix Found $fadIdsSize ids, first found is [${fadIds.mkString(",")}]")

        val (fadIds2, shouldNextF): (Seq[NodeIdIndexed], () => Boolean) = lm match {

          // Сбор id карточек после указанной adId
          case MLookupModes.After =>
            val _fadIds2 = takeIdsAfter( fadIdsIter )
            // При просмотре элементов вправо можно переходить на следующую выборку, если длина текущей упирается в лимит.
            _fadIds2 -> fadIdsHasNextsF

          // Сбор id карточек, предшествующих указанной карточке.
          case MLookupModes.Before =>
            // Всё просто: берём и собираем в обратном порядке.
            val _fadIds2 = takeIdsAfter( fadIdsReverseIter )
              .reverse
            // Была ли найдена текущая карточка? Да, если список предшествующих ей непуст ИЛИ же пуст т.к. искомая карточка первая в списке.
            val _f = { () =>
              _fadIds2.isEmpty || !fadIds.headOption.contains(adId)
            }
            _fadIds2 -> _f

          // Сбор id карточек вокруг желаемой карточки.
          case MLookupModes.Around =>
            if (hasAdId) {
              val b = Seq.newBuilder[NodeIdIndexed]
              // Выбрать id'шники до указанной.
              val needCountBefore = (_adSearch.limit - 1) / 2
              val beforeIds = takeIdsAfter( fadIdsReverseIter, needCountBefore )
                .reverse
              b ++= beforeIds

              // Выбрать id'шники после указанного.
              val needCountAfter = _adSearch.limit - 1 - needCountBefore
              val afterIds = takeIdsAfter( fadIdsIter, needCountAfter )

              // Включить текущий id'шник.
              val currIndex = beforeIds.lastOption
                .map(_.index + 1)
                .orElse { afterIds.headOption.map(_.index - 1) }
                .getOrElse(0)
              b += NodeIdIndexed(adId, currIndex)

              // Включить id'шники после текущего.
              b ++= afterIds

              // Подготовить результат.
              val _f = { () =>
                needCountAfter > afterIds.size  &&  fadIdsHasNexts
              }
              b.result() -> _f

            } else {
              // Нет вообще искомой карточки среди упомянутых.
              Nil -> fadIdsHasNextsF
            }
        }

        val foundCount = fadIds2.size
        LOGGER.trace(s"$logPrefix ids[$foundCount] = [${fadIds2.mkString(", ")}]")

        // Дедубликация кода возврата результата без дальнейшего погружения в рекурсию.
        def resFut = Future.successful {
          AdsLookupRes(fadIds2, fadIds.total.toInt)
        }

        if (foundCount < neededCount) {
          // Найдено недостаточно элементов. Или их просто нет, или их можно найти в следующей порции.

          if (shouldNextF()) {
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
                if (lm2.withAfter)  neededCount2 / 2 + 1  else  neededCount2
              )
            }

            // Надо поискать ещё в след.порции выдачи, запустить поиск.
            val fut2 = _doAdIdsLookup(
              adId          = fadIds2.lastOption.fold(adId)(_.nodeId),
              lm            = lm2,
              neededCount   = neededCount2,
              limit1        = limit1,
              offset1       = offset1 + limit1 - backOffset
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


    override def _firstAdIndexFut: Future[Int] = {
      for (res <- adIdsLookupResFut) yield {
        res.ids.headOption.fold(0)(_.index) + 1
      }
    }


    /** 2016.jun.1 Поиск самих focused-карточек в v2 отсутствует.
      * Ищутся только цепочки id, из них выделяются короткие сегменты, которые читаются через multiget. */
    override def mads1Fut: Future[Seq[MNode]] = {
      Future.successful( Nil )
    }


    override def firstAdIdsFut: Future[Seq[String]] = {
      for (res <- adIdsLookupResFut) yield {
        res.ids.map(_.nodeId)
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
