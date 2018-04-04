package controllers.sc

import _root_.util.di.IScUtil
import _root_.util.n2u.IN2NodesUtilDi
import io.suggest.common.coll.Lists
import io.suggest.common.css.FocusedTopLeft
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.dev.MSzMult
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.{IMNodes, MNode}
import io.suggest.model.n2.node.search.MNodeSearch
import io.suggest.primo.id.OptId
import io.suggest.sc.MScApiVsns
import io.suggest.sc.resp.MScRespActionTypes
import io.suggest.sc.sc3.{MSc3AdData, MSc3AdsResp, MSc3Resp, MSc3RespAction}
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.util.logs.IMacroLogs
import models.blk
import models.im.MImgT
import models.im.logo.LogoOpt_t
import models.msc._
import models.req.IReq
import play.api.mvc.Result
import play.twirl.api.Html
import util.acl._
import views.html.sc.foc._
import io.suggest.sc.focus.{MLookupMode, MLookupModes}
import play.api.libs.json.Json
import util.ad.IJdAdUtilDi
import util.showcase.IScAdSearchUtilDi
import util.stat.IStatUtil
import japgolly.univeq._

import scala.collection.immutable
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.11.14 19:38
 * Description: Поддержка открытых рекламных карточек.
 */
trait ScFocusedAdsBase
  extends ScController
  with IMacroLogs
  with IScUtil
  with ScCssUtil
  with IN2NodesUtilDi
  with IMNodes
  with IScAdSearchUtilDi
  with ICanEditAdDi
{

  import mCommonDi._

  /** Базовая логика обработки запросов сбора данных по рекламным карточкам и компиляции оных в результаты выполнения запросов. */
  abstract class FocusedAdsLogic extends LogicCommonT with AdCssRenderArgs {

    /** Параллельный рендер блоков, находящихся за пределом экрана, должен будет возращать результат этого типа для каждого блока. */
    type OBT

    /** Исходные критерии поиска карточек. */
    val _qs: MScAdsFocQs

    lazy val logPrefix = s"foc(${ctx.timestamp}):"

    /** Sync-состояние выдачи, если есть. */
    def _scStateOpt: Option[ScJsState]

    /** Является ли указанный продьюсер очень внешним в качестве ресивера? */
    def is3rdPartyProducer(producerId: String): Boolean = {
      val hasProdAsRcvr = _qs.search.rcvrIdOpt.exists(_.id ==* producerId)
      // TODO Тут был какой-то говнокод: _qs.search.prodIdOpt.nonEmpty && _qs.search.prodIdOpt ==* _qs.search.rcvrIdOpt
      !hasProdAsRcvr
    }

    lazy val mAdsSearchFut: Future[MNodeSearch] = {
      scAdSearchUtil.qsArgs2nodeSearch(_qs.search, Some(_qs.apiVsn))
    }

    /** Поисковые критерии для подсчёта общего кол-ва результатов. */
    def madsCountSearchFut = mAdsSearchFut

    /** В countAds() можно отправлять и обычный adSearch: forceFirstIds там игнорируется. */
    def madsCountFut: Future[Long] = {
      if (_qs.search.hasAnySearchCriterias) {
        madsCountSearchFut
          .flatMap(mNodes.dynCount)
      } else {
        LOGGER.info(s"$logPrefix No ads search criteriase, count will be zeroed.")
        Future.successful(0L)
      }
    }
    lazy val madsCountIntFut = madsCountFut.map(_.toInt)

    /**
      * 2014.jan.28: Если не найдены какие-то элементы, то сообщить об этом в логи.
      * Это нужно для более быстрого выявления проблем с валидными ссылками на несуществующие карточки.
      *
      * @param mads найденные рекламные карточки.
      * @param ids id запрошенных рекламных карточек.
      */
    protected def logMissingFirstIds(mads: Iterable[MNode], ids: Traversable[String]): Unit = {
      if (mads.size != ids.size) {
        // Выявить, какие id не были найдены.
        val idsFound = mads.iterator.flatMap(_.id).toSet
        val idsWant = ids.toSet
        val idsNotFound = idsWant -- idsFound
        val sb = new StringBuilder(128, "logInvalidFirstIds(): Client requested inexisting ad ids: ")
        idsNotFound.foreach { id =>
          sb.append(id).append(',').append(' ')
        }
        sb.setLength(sb.length - 2)
        LOGGER.debug(sb.toString())
      }
    }

    /** Первые карточки, если запрошены. */
    def firstAdsFut: Future[Seq[MNode]] = {
      firstAdIdsFut.flatMap { firstAdIds =>
        if (firstAdIds.nonEmpty) {
          for (mads <- mNodesCache.multiGet(firstAdIds)) yield {
            // Залоггировать недостающие элементы.
            logMissingFirstIds(mads, firstAdIds)
            // 2016.jul.5 Восстановить исходный порядок first-элементов. v2-выдача плавно переехала на них.
            if (mads.size > 1)
              OptId.orderByIds(firstAdIds, mads)
            else
              mads
          }
        } else {
          Future.successful( Nil )
        }
      }
    }


    /** Если выставлены forceFirstIds, то нужно подолнительно запросить получение указанных
      * id карточек и выставить их в начало списка mads1. */
    lazy val mads2Fut: Future[Seq[MNode]] = {
      val _mads1Fut = mads1Fut
      for {
        firstAds  <- firstAdsFut
        mads      <- _mads1Fut
      } yield {
        // Нано-оптимизация: По идее тут списки или stream'ы, а firstAds содержит только одну карточку. Поэтому можно попробовать смержить с О(1).
        Lists.appendSeqHead(firstAds, mads)
      }
    }

    def prodIdsFut: Future[Set[String]] = {
      for (mads2 <- mads2Fut) yield {
        val iter = for {
          mad <- mads2.iterator
          e   <- mad.edges.withPredicateIter(MPredicates.OwnedBy)
          // TODO В теории тут может выскочить person, который узлом-продьюсером не является.
          // Такое возможно, если пользователи будут напрямую владеть карточками.
          nodeId <- e.nodeIds
        } yield {
          nodeId
        }
        iter.toSet
      }
    }

    /** Список продьюсеров, относящихся к запрошенным focused-карточкам.
      * Порядок продьюсеров в списке неопределён. */
    lazy val mads2ProdsFut: Future[Seq[MNode]] = {
      prodIdsFut.flatMap { prodIds =>
        mNodesCache.multiGet(prodIds)
      }
    }

    /** Карта продьюсеров, относящихся к запрошенным focused-карточкам. */
    lazy val mads2ProdsMapFut: Future[Map[String, MNode]] = {
      for (prods <- mads2ProdsFut) yield {
        OptId.els2idMap(prods)
      }
    }

    lazy val mads2andBrArgsFut: Future[Seq[blk.RenderArgs]] = {
      val _mads2Fut = mads2Fut
      val _withCssClasses = withCssClasses
      _mads2Fut.flatMap { mads =>
        Future.traverse(mads) { mad =>
          for (brArgs <- scUtil.focusedBrArgsFor(mad, _qs.screen)) yield {
            brArgs.copy(
              inlineStyles    = false,
              cssClasses      = _withCssClasses,
              // 2015.mar.06: FIXME Это значение сейчас перезаписывается таким же через showcase.js. // TODO Они должны быть в стилях, а не тут.
              topLeft         = for (_ <- brArgs.wideBg) yield FocusedTopLeft,
              apiVsn          = _qs.apiVsn
            )
          }
        }
      }
    }


    def mads4blkRenderFut: Future[Seq[blk.RenderArgs]] = {
      mads2andBrArgsFut
    }

    /** Карта СЫРЫХ логотипов продьюсеров без подгонки под экран.
      * Если в карте нет искомого продьюсера, то значит он без логотипа-картинки. */
    def prod2logoImgMapFut: Future[Map[String, MImgT]] = {
      mads2ProdsFut
        .flatMap( logoUtil.getLogoOfNodes )
    }
    /** Карта логотипов продьюсеров, подогнанных под запрашиваемый экран. */
    lazy val prod2logoScrImgMapFut: Future[Map[String, MImgT]] = {
      for {
        logosMap   <- prod2logoImgMapFut
        nodeLogos2 <- Future.traverse( logosMap ) { case (nodeId, logoImgRaw) =>
          for (logo4scr <- logoUtil.getLogo4scr(logoImgRaw, _qs.screen)) yield {
            nodeId -> logo4scr
          }
        }
      } yield {
        nodeLogos2.toMap
      }
    }

    /** Параллельный рендер последовательности блоков. */
    def renderedAdsFut: Future[Seq[OBT]] = {
      // Форсируем распараллеливание асинхронных операций.
      val _mads4blkRenderFut  = mads4blkRenderFut
      val _producersMapFut    = mads2ProdsMapFut

      // touch-воздействие, чтобы запустить процесс. Сама карта будет опрошена в focAdsRenderArgsFor()
      prod2logoScrImgMapFut

      val _firstAdIndexFut1 = firstAdIndexFut
      for {
        madsCountInt    <- madsCountIntFut
        mads4blkRender  <- _mads4blkRenderFut
        producersMap    <- _producersMapFut
        firstAdIndex    <- _firstAdIndexFut1
        res <- {
          val (_, futs) = Lists.mapFoldLeft(mads4blkRender, acc0 = (firstAdIndex, blockHtmlRenderAcc0)) {
            case (acc0 @ (index, brAcc0), brArgs) =>
              // Сразу инкрементим счетчик, т.к. если отсчитывать от offset, то будет ноль при первом вызове.
              val resOpt = for {
                // Вычисляем id узла-продьюсера.
                producerId  <- n2NodesUtil.madProducerId(brArgs.mad)
                // Вычисляем продьюсера.
                producer    <- producersMap.get(producerId)
              } yield {
                // Запустить рендер одного блока.
                val index1 = index + 1
                val args = AdBodyTplArgs(
                  brArgs      = brArgs,
                  producer    = producer,
                  index       = index1,
                  adsCount    = madsCountInt,
                  is3rdParty  = is3rdPartyProducer(producerId)
                )
                val (renderFut, brAcc1) = renderOneBlockAcc(args, brAcc0)
                (index1, brAcc1) -> renderFut
              }
              resOpt getOrElse {
                LOGGER.warn(s"Unable to render ad[${brArgs.mad.idOrNull}] #$index, because producer node or info about it is missing.")
                acc0 -> null
              }
          }
          val futs1 = futs.filter { _ != null }
          Future.sequence(futs1)
        }

      } yield {
        res
      }
    }

    /**
     * Отправить в очередь на исполнение рендер одного блока, заданного контейнером аргументов.
     * Метод должен вызываться в реализации логики из кода реализации метода renderOuterBlock().
      *
      * @param args Аргументы рендера блока.
     * @return Фьючерс с html-рендером одного блока.
     */
    def renderBlockHtml(args: IAdBodyTplArgs): Future[Html] = {
      Future {
        _adTpl(args)(ctx)
      }
    }

    /** Что же будет рендерится в качестве текущей просматриваемой карточки? */
    lazy val focAdOptFut: Future[Option[blk.RenderArgs]] = {
      for (mads2andBrArgs <- mads2andBrArgsFut) yield {
        mads2andBrArgs.headOption
      }
    }

    /** Фьючерс продьюсера, относящегося к текущей карточке. */
    def focAdProducerOptFut: Future[Option[MNode]] = {
      val _prodsMapFut = mads2ProdsMapFut
      focAdOptFut.flatMap { focAdOpt =>
        FutureUtil.optFut2futOpt( focAdOpt ) { focMad =>
          for (prodsMap <- _prodsMapFut) yield {
            n2NodesUtil
              .madProducerId(focMad.mad)
              .flatMap(prodsMap.get)
          }
        }
      }
    }


    lazy val firstAdIndexFut = _firstAdIndexFut

    /** Сборка аргументов для рендера focused-карточки, т.е. раскрытого блока + оформление продьюсера. */
    protected def focAdsRenderArgsFor(abtArgs: IAdBodyTplArgs): Future[IFocusedAdsTplArgs] = {
      val producer = abtArgs.producer

      val logoImgOptFut: Future[LogoOpt_t] = for {
        logosMap   <- prod2logoScrImgMapFut
      } yield {
        abtArgs.producer
          .id
          .flatMap(logosMap.get)
      }

      val _fgColor = producer.meta.colors.fg.fold(scUtil.SITE_FGCOLOR_DFLT)(_.code)
      val _bgColor = producer.meta.colors.bg.fold(scUtil.SITE_BGCOLOR_DFLT)(_.code)

      for (_logoImgOpt <- logoImgOptFut) yield {
        FocusedAdsTplArgs2(
          abtArgs,
          bgColor     = _bgColor,
          fgColor     = _fgColor,
          hBtnArgs    = HBtnArgs(fgColor = _fgColor),
          logoImgOpt  = _logoImgOpt,
          is3rdParty  = n2NodesUtil
            .madProducerId(abtArgs.brArgs.mad)
            .exists(is3rdPartyProducer),
          jsStateOpt  = _scStateOpt
        )
      }
    }

    /** Сборка контейнера аргументов для вызова шаблона _fullTpl(). */
    def focAdsHtmlArgsFut: Future[IFocusedAdsTplArgs] = {
      val _producerFut = focAdProducerOptFut.map(_.get)
      val _brArgsFut = focAdOptFut.map(_.get)
      val _madsCountIntFut = madsCountIntFut
      val _firstAdIndexFut1 = firstAdIndexFut

      // Склеиваем фьючерсы в набор аргументов вызова focAdsRenderArgsFor().
      for {
        _producer     <- _producerFut
        _brArgs       <- _brArgsFut
        madsCountInt  <- _madsCountIntFut
        firstAdIndex  <- _firstAdIndexFut1
        // Завернуть все данные в общий контейнер аргументов
        abtArgs = AdBodyTplArgs(
          _brArgs, _producer,
          adsCount    = madsCountInt,
          index       = firstAdIndex,
          is3rdParty  = is3rdPartyProducer(_producer.id.get)
        )
        // Запустить сборку аргументов рендера.
        args          <- focAdsRenderArgsFor(abtArgs)
      } yield {
        args
      }
    }

    override def adsCssExternalFut: Future[Seq[AdCssArgs]] = {
      for (mbas <- mads2andBrArgsFut) yield {
        for (mba <- mbas) yield {
          AdCssArgs(mba.mad.id.get, mba.szMult)
        }
      }
    }

    /** Дописывать эти css-классы в стили и в рендер. */
    def withCssClasses = Seq("focused")

    /** Вызов заглавного рендера карточки. */
    def renderFocused(args: IFocusedAdsTplArgs): Html = {
      _fullTpl(args)(ctx)
    }

    // TODO Заглавная карточка специфична только для API v1, но используется в SyncSite для рендера единственной (текущей) карточки.
    // Нужно перевести SyncSite на использование API v2 или на базовый трейт;  v1 будет постепенно выпилено.

    /** Отрендеренное отображение раскрытой карточки вместе с обрамлениями и остальным.
      * Т.е. пригодно для вставки в соотв. div indexTpl. Функция игнорирует значение _withHeadAd.
      *
      * @return Если нет карточек, то будет NoSuchElementException. Иначе фьючерс с HTML-рендером. */
    def focAdHtmlFut: Future[Html] = {
      for (args <- focAdsHtmlArgsFut) yield {
        renderFocused(args)
      }
    }

    /** Опциональный аналог focAdHtmlFut. Функция учитывает значение _withHeadAd. */
    def focAdHtmlOptFut: Future[Option[Html]] = {
      focAdHtmlFut
        .map(Some.apply)
        .recover {
          case _: NoSuchElementException =>
            None
          case ex: Throwable =>
            LOGGER.error("Failed to find focused ad", ex)
            None
        }
    }

    /** Контекстно-зависимая сборка данных статистики. */
    override def scStat: Future[Stat2] = {
      val _rcvrOptFut   = mNodesCache.maybeGetByEsIdCached( _qs.search.rcvrIdOpt )
      val _prodOptFut   = mNodesCache.maybeGetByEsIdCached( _qs.search.prodIdOpt )

      val _userSaOptFut = statUtil.userSaOptFutFromRequest()

      val _madsFut      = mads2Fut
      val _adSearchFut  = mAdsSearchFut

      for {
        _userSaOpt  <- _userSaOptFut
        _rcvrOpt    <- _rcvrOptFut
        _prodOpt    <- _prodOptFut
        _mads       <- _madsFut
        _adSearch   <- _adSearchFut
      } yield {
        var saAcc = List[MAction](
          statUtil.madsAction(_mads, MActionTypes.ScAdsFocused),

          // Сохранить фактический search limit
          MAction(
            actions = Seq( MActionTypes.SearchLimit ),
            count   = Seq( _adSearch.limit )
          ),

          // Сохранить фактически search offset
          MAction(
            actions = Seq( MActionTypes.SearchOffset ),
            count   = Seq( _adSearch.offset )
          )
        )

        // Если идёт начальная фокусировка, то сохранить данные по окликнутой карточке.
        if (_qs.lookupMode contains MLookupModes.Around) {
          for (mad0 <- _mads.find(_.id.contains( _qs.lookupAdId) )) {
            saAcc ::= MAction(
              actions   = Seq(MActionTypes.ScAdsFocusingOnAd),
              nodeId    = mad0.id.toSeq,
              nodeName  = mad0.guessDisplayName.toSeq
            )
          }
        }

        saAcc = statUtil.withNodeAction(MActionTypes.ScRcvrAds, _qs.search.rcvrIdOpt, _rcvrOpt) {
          statUtil.withNodeAction( MActionTypes.ScProdAds, _qs.search.prodIdOpt, _prodOpt )(saAcc)
        }

        new Stat2 {
          override def components   = MComponents.Open :: super.components
          override def statActions  = saAcc
          override def userSaOpt    = _userSaOpt
          override def locEnvOpt    = _qs.search.locEnv.optional
          override def gen          = _qs.search.genOpt
          override def devScreenOpt = _qs.screen
        }

      }
    }


    /** trait NoBrAcc{} замержен в основной код.:
      *  Если поддержка аккамулятора при вызовах renderOutBlock() не требуется, то этот трейт отключит её. */

    /** Тип render-аккамулятора. */
    type BrAcc_t = None.type

    /** Начальный аккамулятор для первого вызова renderOuterBlock(). */
    def blockHtmlRenderAcc0: BrAcc_t = None


    /**
      * Рендер одного блока. В случае Html можно просто вызвать renderBlockHtml().
      * 11.jun.2015: Добавлена поддержка синхронного аккамулятора для передачи данных между вызовами этого метода.
      *
      * @param args Контейнер с данными для запуска рендера.
      * @param brAcc0 Аккамулятор.
      * @return Фьючерс рендера и новый аккамулятор.
      */
    def renderOneBlockAcc(args: AdBodyTplArgs, brAcc0: BrAcc_t): (Future[OBT], BrAcc_t) = {
      (renderOuterBlock(args), brAcc0)
    }

    def renderOuterBlock(args: AdBodyTplArgs): Future[OBT]


    /*
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

    /** Число-отметка текущей логики обработки запроса. */
    private val _currTimeMs = System.currentTimeMillis()


    /** Асинхронный результат поиска сегмента карточек. */
    lazy val adIdsLookupResFut: Future[AdsLookupRes] = {
      _qs.lookupMode
        .filter { _ =>
          _qs.search.hasAnySearchCriterias
        }
        .map { lm =>
          // v2-выдача ищет focused-карточки в выдаче.
          LOGGER.trace(s"$logPrefix v2 focusing: ${_qs.lookupAdId} ${lm.toVisualString}")
          mAdsSearchFut.flatMap { mNodeSearch =>
            _doAdIdsLookup(neededCount = mNodeSearch.limit, lm = lm)
          }
        }
        .orElse {
          OptionUtil.maybe( _qs.lookupMode.isEmpty ) {
            // v3-выдача пытается прочитать ровно одну карточку.
            LOGGER.trace(s"$logPrefix v3: single ad GET: ${_qs.lookupAdId} without lookup.")
            Future.successful(
              AdsLookupRes(
                ids   = NodeIdIndexed(_qs.lookupAdId, 1) :: Nil,
                total = 1
              )
            )
          }
        }
        .getOrElse {
          // Хз, что запрашивается.
          LOGGER.warn(s"$logPrefix v2: not ad-search criterias found, skipping ids lookup.")
          Future.successful( AdsLookupRes(Nil, total = 0) )
        }
    }

    /** Метод рекурсивного поиска сегмента последовательности id карточек в пакетных выхлопах elasticsearch. */
    def _doAdIdsLookup(adId: String = _qs.lookupAdId, lm: MLookupMode,
                       neededCount: Int, limit1: Int = 50, offset1: Int = 0, tryN: Int = 1): Future[AdsLookupRes] = {

      assert(tryN <= 20)

      // Необходимо поискать id focused-карточек в корректном порядке с начала списка.
      val fadsIdsSearchQs = _qs.search.copy(
        limitOpt  = Some(limit1),
        offsetOpt = Some(offset1)
      )

      // TODO Далее какой-то быдлокод с реализацией нетривиальной выборки сегмента последовательности foc-карточек.
      scAdSearchUtil.qsArgs2nodeSearch(fadsIdsSearchQs, Some(_qs.apiVsn)).flatMap { msearch =>

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


    def _firstAdIndexFut: Future[Int] = {
      for (res <- adIdsLookupResFut) yield {
        res.ids.headOption.fold(0)(_.index) //+ 1
      }
    }


    /** 2016.jun.1 Поиск самих focused-карточек в v2 отсутствует.
      * Ищутся только цепочки id, из них выделяются короткие сегменты, которые читаются через multiget. */
    def mads1Fut: Future[Seq[MNode]] = {
      Future.successful( Nil )
    }


    def firstAdIdsFut: Future[Seq[String]] = {
      for (res <- adIdsLookupResFut) yield {
        for (idIndexed <- res.ids) yield {
          idIndexed.nodeId
        }
      }
    }

  }

}


/** Поддержка экшена для focused-ads API v1. */
trait ScFocusedAds
  extends ScFocusedAdsBase
  with IStatUtil
  with IMaybeAuth
  with IJdAdUtilDi
{

  import mCommonDi._


  /** Расширение базовой focused-ads-логики для написания HTTP-экшенов. */
  protected trait FocusedAdsLogicHttp extends FocusedAdsLogic {

    /** Синхронного состояния выдачи тут обычно нет. */
    override def _scStateOpt: Option[ScJsState] = None

    /** Сборка HTTP-ответа. */
    def resultFut: Future[Result]

  }


  /** V3-логика фокусировки на карточке.
    * Повторяет логику v2, но:
    * - client-side render.
    * - jd-карточки вместо традиционных.
    *
    * Некоторая унификация с JSON плитки.
    *
    * @param _qs Параметры рендера.
    * @param _request Исходный HTTP-реквест.
    */
  protected class FocusedLogicHttpV3(override val _qs: MScAdsFocQs)
                                    (override implicit val _request: IReq[_])
    extends FocusedAdsLogicHttp
  {
    // TODO Код тут очень похож на код рендера одной карточки в ScAdsTileLogicV3. Потому что jd-карточки раскрываются в плитки.

    override type OBT = MSc3AdData

    lazy val tileArgs = scUtil.getTileArgs(_qs.screen)

    /** Рендер одной focused-карточки. */
    override def renderOuterBlock(args: AdBodyTplArgs): Future[OBT] = {
      Future {
        val mad = args.brArgs.mad
        val tpl = jdAdUtil.getNodeTpl( mad )
        val edges2 = jdAdUtil.filterEdgesForTpl(tpl, mad.edges)
        val jdFut = jdAdUtil.mkJdAdDataFor
          .show(
            nodeId        = mad.id,
            nodeEdges     = edges2,
            tpl           = tpl,
            szMult        = tileArgs.szMult,
            allowWide     = true,
            forceAbsUrls  = _qs.apiVsn.forceAbsUrls
          )(ctx)
          .execute()

        // Проверить права на редактирование у текущего юзера.
        val isEditAllowed = canEditAd.isUserCanEditAd(_request.user, mad = args.brArgs.mad, producer = args.producer)

        for (jd <- jdFut) yield {
          MSc3AdData(
            jd = jd,
            canEdit = Some(isEditAllowed)
          )
        }
      }
        .flatten
    }

    /** Сборка HTTP-ответа v3-выдачи. */
    override def resultFut: Future[Result] = {
      val _renderedAdsFut = renderedAdsFut

      // Собрать ответ на запрос, когда всё будет подготовлено
      val szMult = MSzMult.fromDouble( tileArgs.szMult )

      for {
        renderedAds <- _renderedAdsFut
      } yield {
        val scResp = MSc3Resp(
          respActions = MSc3RespAction(
            acType = MScRespActionTypes.AdsFoc,
            ads = Some(MSc3AdsResp(
              ads     = renderedAds,
              szMult  = szMult
            ))
          ) :: Nil
        )

        // Вернуть HTTP-ответ. Короткий кэш просто для защиты от дублирующихся запросов.
        Ok( Json.toJson(scResp) )
          .cacheControl( if (ctx.request.user.isAnon) 20 else 6 )
      }
    }

  }


  /** Экшен для рендера горизонтальной выдачи карточек.
    *
    * @param qs URL-аргументы запроса.
    * @return JSONP с отрендеренными карточками.
    */
  def focusedAds(qs: MScAdsFocQs) = maybeAuth().async { implicit request =>
    val logic = getLogicFor(qs)
    // Запустить изменябельное тело экшена на исполнение.
    _focusedAds(logic)
  }

  /**
    * Тело экщена focusedAds() вынесено сюда для возможности перезаписывания.
    *
    * @param logic Экземпляр focused-логики.
    * @return Фьючерс с результатом.
    */
  protected def _focusedAds(logic: FocusedAdsLogicHttp): Future[Result] = {
    _showFocusedAds(logic)
  }

  /**
    * Юзер просматривает карточки в раскрытом виде (фокусируется). Отрендерить браузер карточек.
    *
    * @param logic Закешированная исходная focused-логика рендера.
    * @return Фьючерс с http-результатом.
    */
  protected def _showFocusedAds(logic: FocusedAdsLogicHttp): Future[Result] = {
    // Запускаем сборку ответа:
    val resultFut = logic.resultFut

    // И запускаем сохранение статистики по текущему действу:
    logic.saveScStat()

    // Вернуть основной результат экшена.
    resultFut
  }


  /** Перезаписываемый сборкщик логик для версий. */
  def getLogicFor(qs: MScAdsFocQs)(implicit request: IReq[_]): FocusedAdsLogicHttp = {
    if (qs.apiVsn.majorVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
      new FocusedLogicHttpV3(qs)
    } else {
      throw new IllegalArgumentException(s"Unsupported API version: ${qs.apiVsn} :: ${request.method} ${request.uri} FROM ${request.remoteClientAddress}")
    }
  }

}
