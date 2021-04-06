package controllers.sc

import _root_.util.n2u.IN2NodesUtilDi
import io.suggest.common.coll.Lists
import io.suggest.common.css.FocusedTopLeft
import io.suggest.es.model.EsModelDi
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.node.{IMNodes, MNode}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.primo.id.OptId
import io.suggest.sc.ads.{MLookupMode, MLookupModes, MSc3AdData, MSc3AdsResp, MScAdInfo}
import io.suggest.sc.sc3.{MSc3RespAction, MScQs, MScRespActionTypes}
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.util.logs.IMacroLogs
import models.blk
import models.msc._
import models.req.IReq
import util.acl._
import util.ad.IJdAdUtilDi
import util.showcase.{IScAdSearchUtilDi, IScUtil}
import util.stat.IStatUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.11.14 19:38
 * Description: Поддержка открытых рекламных карточек.
 */

trait ScFocusedAds
  extends ScController
  with IMacroLogs
  with IScUtil
  with IN2NodesUtilDi
  with IMNodes
  with IScAdSearchUtilDi
  with ICanEditAdDi
  with IStatUtil
  with IJdAdUtilDi
  with EsModelDi
{

  import mCommonDi._
  import esModel.api._


  /** Отсортировать элементы моделей согласно порядку их id.
    *
    * @param ids Исходные id'шники в исходном порядке.
    * @param els Исходная цепочка элементов.
    * @tparam Id_t Тип используемого id'шника.
    * @tparam T Тип одного элемента модели.
    * @return Итоговая отсортированная коллекция.
    */
  private def orderByIds[Id_t, T <: OptId[Id_t]](ids: IterableOnce[Id_t], els: Seq[T]): Seq[T] = {
    val idsMap = ids
      .iterator
      .zipWithIndex
      .toMap

    els.sortBy { e =>
      e.id
        .flatMap(idsMap.get)
        .getOrElse(0)
    }
  }


  /** Базовая логика обработки запросов сбора данных по рекламным карточкам и компиляции оных в результаты выполнения запросов. */
  abstract class FocusedAdsLogic extends LogicCommonT with IRespActionFut {

    /** Параллельный рендер блоков, находящихся за пределом экрана, должен будет возращать результат этого типа для каждого блока. */
    type OBT

    /** Исходные критерии поиска карточек. */
    val _qs: MScQs

    lazy val logPrefix = s"foc(${ctx.timestamp}):"

    /** Поиск без ble-контекста, т.к. обычно поиск по id карточки. */
    lazy val mAdsSearchFut: Future[MNodeSearch] = {
      val nodeSearch = scAdSearchUtil.qsArgs2nodeSearch( _qs )
      Future.successful( nodeSearch )
    }


    /**
      * 2014.jan.28: Если не найдены какие-то элементы, то сообщить об этом в логи.
      * Это нужно для более быстрого выявления проблем с валидными ссылками на несуществующие карточки.
      *
      * @param mads найденные рекламные карточки.
      * @param ids id запрошенных рекламных карточек.
      */
    protected def logMissingFirstIds(mads: Iterable[MNode], ids: Iterable[String]): Unit = {
      if (mads.size != ids.size) {
        // Выявить, какие id не были найдены.
        val idsFound = mads
          .iterator
          .flatMap(_.id)
          .toSet
        val idsWant = ids.toSet
        val idsNotFound = idsWant -- idsFound
        val sb = new StringBuilder(128, "logInvalidFirstIds(): Client requested inexisting ad ids: ")
        for (id <- idsNotFound) {
          sb.append(id)
            .append(',')
            .append(' ')
        }
        sb.setLength(sb.length - 2)
        LOGGER.debug(sb.toString())
      }
    }

    /** Первые карточки, если запрошены. */
    def firstAdsFut: Future[Seq[MNode]] = {
      firstAdIdsFut.flatMap { firstAdIds =>
        if (firstAdIds.nonEmpty) {
          for {
            mads <- mNodes.multiGetCache( firstAdIds )
          } yield {
            // Залоггировать недостающие элементы.
            logMissingFirstIds(mads, firstAdIds)
            // 2016.jul.5 Восстановить исходный порядок first-элементов. v2-выдача плавно переехала на них.
            if (mads.size > 1)
              orderByIds(firstAdIds, mads)
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
      firstAdsFut
    }

    def prodIdsFut: Future[Set[String]] = {
      for (mads2 <- mads2Fut) yield {
        (for {
          mad <- mads2.iterator
          e   <- mad.edges.withPredicateIter(MPredicates.OwnedBy)
          // TODO В теории тут может выскочить person, который узлом-продьюсером не является.
          // Такое возможно, если пользователи будут напрямую владеть карточками.
          nodeId <- e.nodeIds
        } yield {
          nodeId
        })
          .toSet
      }
    }

    /** Список продьюсеров, относящихся к запрошенным focused-карточкам.
      * Порядок продьюсеров в списке неопределён. */
    def mads2ProdsFut: Future[Seq[MNode]] = {
      prodIdsFut.flatMap { prodIds =>
        mNodes.multiGetCache(prodIds)
      }
    }

    /** Карта продьюсеров, относящихся к запрошенным focused-карточкам. */
    def mads2ProdsMapFut: Future[Map[String, MNode]] = {
      for (prods <- mads2ProdsFut) yield {
        prods
          .zipWithIdIter[String]
          .to(Map)
      }
    }

    def mads2andBrArgsFut: Future[Seq[blk.RenderArgs]] = {
      val _mads2Fut = mads2Fut
      val _withCssClasses = withCssClasses
      _mads2Fut.flatMap { mads =>
        Future.traverse(mads) { mad =>
          for {
            brArgs <- scUtil.focusedBrArgsFor(mad, _qs.common.screen)
          } yield {
            brArgs.copy(
              inlineStyles    = false,
              cssClasses      = _withCssClasses,
              // 2015.mar.06: FIXME Это значение сейчас перезаписывается таким же через showcase.js. // TODO Они должны быть в стилях, а не тут.
              topLeft         = for (_ <- brArgs.wideBg) yield FocusedTopLeft,
              apiVsn          = _qs.common.apiVsn
            )
          }
        }
      }
    }


    def mads4blkRenderFut: Future[Seq[blk.RenderArgs]] = {
      mads2andBrArgsFut
    }

    /** Параллельный рендер последовательности блоков. */
    def renderedAdsFut: Future[Seq[OBT]] = {
      // Форсируем распараллеливание асинхронных операций.
      val _mads4blkRenderFut  = mads4blkRenderFut
      val _producersMapFut    = mads2ProdsMapFut
      for {
        mads4blkRender  <- _mads4blkRenderFut
        producersMap    <- _producersMapFut
        res <- {
          val (_, futs) = Lists.mapFoldLeft(mads4blkRender, acc0 = blockHtmlRenderAcc0) {
            case (acc0 @ brAcc0, brArgs) =>
              // Сразу инкрементим счетчик, т.к. если отсчитывать от offset, то будет ноль при первом вызове.
              val resOpt = for {
                // Вычисляем id узла-продьюсера.
                producerId  <- n2NodesUtil.madProducerId(brArgs.mad)
                // Вычисляем продьюсера.
                producer    <- producersMap.get(producerId)
              } yield {
                // Запустить рендер одного блока.
                val args = AdBodyTplArgs(
                  brArgs      = brArgs,
                  producer    = producer,
                )
                val (renderFut, brAcc1) = renderOneBlockAcc(args, brAcc0)
                brAcc1 -> renderFut
              }
              resOpt getOrElse {
                LOGGER.warn(s"Unable to render ad[${brArgs.mad.idOrNull}] because producer node or info about it is missing.")
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


    /** Дописывать эти css-классы в стили и в рендер. */
    def withCssClasses = Seq("focused")

    lazy val lookupModeOpt = _qs.foc.flatMap(_.lookupMode)

    /** Контекстно-зависимая сборка данных статистики. */
    override def scStat: Future[Stat2] = {
      val _rcvrOptFut   = mNodes.maybeGetByEsIdCached( _qs.search.rcvrId )
      val _prodOptFut   = mNodes.maybeGetByEsIdCached( _qs.search.prodId )

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
            actions = MActionTypes.SearchLimit :: Nil,
            count   = _adSearch.limit :: Nil,
          ),

          // Сохранить фактически search offset
          MAction(
            actions = MActionTypes.SearchOffset :: Nil,
            count   = _adSearch.offset :: Nil,
          )
        )

        // Если идёт начальная фокусировка, то сохранить данные по окликнутой карточке.
        if (lookupModeOpt contains MLookupModes.Around) {
          for {
            qsFoc <- _qs.foc
            mad0  <- _mads.find(_.id.contains( qsFoc.lookupAdId) )
          } {
            saAcc ::= MAction(
              actions   = MActionTypes.ScAdsFocusingOnAd :: Nil,
              nodeId    = mad0.id.toList,
              nodeName  = mad0.guessDisplayName.toList,
            )
          }
        }

        saAcc = statUtil.withNodeAction(MActionTypes.ScRcvrAds, _qs.search.rcvrId, _rcvrOpt) {
          statUtil.withNodeAction( MActionTypes.ScProdAds, _qs.search.prodId, _prodOpt )(saAcc)
        }

        new Stat2 {
          override def components = MComponents.Open :: super.components
          override def statActions = saAcc
          override def userSaOpt = _userSaOpt
          override def locEnvOpt = _qs.common.locEnv.optional
          override def gen = _qs.search.genOpt
          override def devScreenOpt = _qs.common.screen
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

    lazy val lookupAdIdOpt = _qs.foc.map(_.lookupAdId)

    /** Асинхронный результат поиска сегмента карточек. */
    lazy val adIdsLookupResFut: Future[AdsLookupRes] = {
      lookupModeOpt
        .filter { _ =>
          _qs.hasAnySearchCriterias
        }
        .flatMap { lm =>
          for (lookupAdId <- lookupAdIdOpt) yield {
            // v2-выдача ищет focused-карточки в выдаче.
            LOGGER.trace(s"$logPrefix v2 focusing: ${lookupAdIdOpt.orNull} ${lm.toVisualString}")
            mAdsSearchFut.flatMap { mNodeSearch =>
              _doAdIdsLookup(adId = lookupAdId, neededCount = mNodeSearch.limit, lm = lm)
            }
          }
        }
        .orElse {
          for {
            lookupAdId <- lookupAdIdOpt
            if lookupModeOpt.isEmpty
          } yield {
            // v3-выдача пытается прочитать ровно одну карточку.
            LOGGER.trace(s"$logPrefix v3: single ad GET: ${lookupAdIdOpt} without lookup.")
            val r = AdsLookupRes(
              ids   = NodeIdIndexed(lookupAdId, 1) :: Nil,
              total = 1
            )
            Future.successful( r )
          }
        }
        .getOrElse {
          // Хз, что запрашивается.
          LOGGER.warn(s"$logPrefix v2: not ad-search criterias found, skipping ids lookup.")
          Future.successful( AdsLookupRes(Nil, total = 0) )
        }
    }

    /** Метод рекурсивного поиска сегмента последовательности id карточек в пакетных выхлопах elasticsearch. */
    def _doAdIdsLookup(adId: String, lm: MLookupMode,
                       neededCount: Int, limit1: Int = 50, offset1: Int = 0, tryN: Int = 1): Future[AdsLookupRes] = {

      require(tryN <= 20, "Too many iterations")

      // Необходимо поискать id focused-карточек в корректном порядке с начала списка.
      val fadsIdsSearchQs = _qs.search.withLimitOffset(
        limit  = Some(limit1),
        offset = Some(offset1)
      )
      val qs2 = MScQs.search.set(fadsIdsSearchQs)( _qs )

      // TODO Далее какой-то быдлокод с реализацией нетривиальной выборки сегмента последовательности foc-карточек.
      val msearch = scAdSearchUtil.qsArgs2nodeSearch(qs2)

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

      for {
        fadIds <- fadIdsFut
        res <- {
          lazy val hasAdId = fadIds.contains(adId)
          lazy val fadIdsSize = fadIds.size
          lazy val fadIdsHasNexts = fadIdsSize >= limit1

          class SimpleLookupRes(override val fadIds2: Seq[NodeIdIndexed]) extends ILookupRes {
            override def shouldNext = fadIdsHasNexts
          }

          // Добавить индексы элементам итератора с поправкой на текущий offset1.
          // Получаться порядковые номера карточек: 0, 1, 2, ...
          val fadsIdsInxed = fadIds
            .iterator
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
      } yield {
        res
      }
    }


    def firstAdIdsFut: Future[Seq[String]] = {
      for (res <- adIdsLookupResFut) yield {
        for (idIndexed <- res.ids) yield
          idIndexed.nodeId
      }
    }

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
  case class FocusedLogicHttpV3(override val _qs: MScQs)
                               (override implicit val _request: IReq[_])
    extends FocusedAdsLogic
  {
    // TODO Код тут очень похож на код рендера одной карточки в ScAdsTileLogicV3. Потому что jd-карточки раскрываются в плитки.

    override type OBT = MSc3AdData

    lazy val tileArgs = scUtil.getTileArgs( _qs.common.screen )

    /** Рендер одной focused-карточки. */
    override def renderOuterBlock(args: AdBodyTplArgs): Future[OBT] = {
      Future {
        val mad = args.brArgs.mad
        val tpl = mad.extras.doc.get.template
        val edges2 = jdAdUtil.filterEdgesForTpl(tpl, mad.edges)
        val jdFut = jdAdUtil.mkJdAdDataFor
          .show(
            nodeId        = mad.id,
            nodeEdges     = edges2,
            tpl           = tpl,
            jdConf        = tileArgs,
            allowWide     = true,
            selPathRev    = List.empty,
            // Возвращать явно-заданный заголовок карточки, чтобы выводить в заголовке браузера.
            nodeTitle     = mad.meta.basic.nameOpt,
            scApiVsn      = Some( _qs.common.apiVsn ),
          )(ctx)
          .execute()

        // Проверить права на редактирование у текущего юзера.
        val isEditAllowed = canEditAd.isUserCanEditAd(
          user  = _request.user,
          mad   = args.brArgs.mad,
          producer = args.producer,
        )

        for (jd <- jdFut) yield {
          MSc3AdData(
            jd = jd,
            info = MScAdInfo(
              canEditOpt = Some(isEditAllowed),
              flags      = scUtil.collectScRcvrFlags(_qs, args.brArgs.mad),
            )
          )
        }
      }
        .flatten
    }


    /** Сборка RespAction ответа сервера. */
    def respActionFut: Future[MSc3RespAction] = {
      val _renderedAdsFut = renderedAdsFut

      for {
        renderedAds <- _renderedAdsFut
      } yield {
        MSc3RespAction(
          acType = MScRespActionTypes.AdsFoc,
          ads = Some(MSc3AdsResp(
            ads     = renderedAds,
            szMult  = tileArgs.szMult,
          ))
        )
      }
    }

  }

}
