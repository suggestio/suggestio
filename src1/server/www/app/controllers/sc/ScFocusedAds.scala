package controllers.sc

import _root_.util.di.{IScStatUtil, IScUtil}
import _root_.util.n2u.IN2NodesUtilDi
import io.suggest.common.css.FocusedTopLeft
import io.suggest.common.fut.FutureUtil
import io.suggest.model.common.OptId
import io.suggest.model.n2.node.IMNodes
import io.suggest.model.n2.node.search.MNodeSearch
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.util.Lists
import models.im.MImgT
import models.im.logo.LogoOpt_t
import models.msc._
import models.req.IReq
import play.api.mvc.Result
import play.twirl.api.Html
import util.PlayMacroLogsI
import util.acl._
import views.html.sc.foc._
import models._
import models.mlu.MLookupModes
import util.showcase.IScAdSearchUtilDi

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
  with PlayMacroLogsI
  with IScUtil
  with ScCssUtil
  with IN2NodesUtilDi
  with IMNodes
  with IScAdSearchUtilDi
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

    /** Поиск focused-карточек. */
    def mads1Fut: Future[Seq[MNode]]

    /** Является ли указанный продьюсер очень внешним в качестве ресивера? */
    def is3rdPartyProducer(producerId: String): Boolean = {
      val hasProdAsRcvr = _qs.search.prodIdOpt.nonEmpty && _qs.search.prodIdOpt == _qs.search.rcvrIdOpt
      !hasProdAsRcvr
    }

    lazy val mAdsSearchFut: Future[MNodeSearch] = {
      scAdSearchUtil.qsArgs2nodeSearch(_qs.search)
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

    def firstAdIdsFut: Future[Seq[String]]

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
    def blocksHtmlsFut: Future[Seq[OBT]] = {
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
    def _firstAdIndexFut: Future[Int]

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

    override def adsCssFieldRenderArgsFut: Future[immutable.Seq[blk.FieldCssRenderArgsT]] = {
      for (mbas <- mads2andBrArgsFut) yield {
        mbas.iterator
          .flatMap { mba =>  mad2craIter(mba, mba.cssClasses) }
          .toStream
      }
    }

    /** Дописывать эти css-классы в стили и в рендер. */
    def withCssClasses = Seq("focused")

    /** Параметры для рендера обрамляющего css блоков (css не полей, а блоков в целом). */
    override def adsCssRenderArgsFut: Future[immutable.Seq[blk.IRenderArgs]] = {
      for (mbas <- mads2andBrArgsFut) yield {
        mbas.toStream
      }
    }

    /** Вызов заглавного рендера карточки. */
    def renderFocused(args: IFocusedAdsTplArgs): Html = {
      _fullTpl(args)(ctx)
    }
    /** Вызов renderFocused() асинхронно, внутри Future{}. Полезно для параллельного рендера блоков. */
    def renderFocusedFut(args: IFocusedAdsTplArgs): Future[Html] = {
      Future {
        renderFocused(args)
      }
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
          case ex: NoSuchElementException =>
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

      val _userSaOptFut = scStatUtil.userSaOptFutFromRequest()

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
          scStatUtil.madsAction(_mads, MActionTypes.ScAdsFocused),

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
        if (_qs.lookupMode == MLookupModes.Around) {
          for (mad0 <- _mads.find(_.id.contains( _qs.lookupAdId) )) {
            saAcc ::= MAction(
              actions   = Seq(MActionTypes.ScAdsFocusingOnAd),
              nodeId    = mad0.id.toSeq,
              nodeName  = mad0.guessDisplayName.toSeq
            )
          }
        }

        saAcc = scStatUtil.withNodeAction(MActionTypes.ScRcvrAds, _qs.search.rcvrIdOpt, _rcvrOpt) {
          scStatUtil.withNodeAction( MActionTypes.ScProdAds, _qs.search.prodIdOpt, _prodOpt )(saAcc)
        }

        new Stat2 {
          override def scComponents = MComponents.Open :: super.scComponents
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

  }

}


/** Поддержка экшена для focused-ads API v1. */
trait ScFocusedAds
  extends ScFocusedAdsBase
  with IScStatUtil
  with MaybeAuth {

  /** Экшен для рендера горизонтальной выдачи карточек.
    *
    * @param qs URL-аргументы запроса.
    * @return JSONP с отрендеренными карточками.
    */
  def focusedAds(qs: MScAdsFocQs) = MaybeAuth().async { implicit request =>
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
    throw new IllegalArgumentException(s"Unsupported API version: ${qs.apiVsn} :: ${request.method} ${request.uri} FROM ${request.remoteAddress}")
  }


  /** Расширение базовой focused-ads-логики для написания HTTP-экшенов. */
  protected trait FocusedAdsLogicHttp extends FocusedAdsLogic {

    /** Синхронного состояния выдачи тут обычно нет. */
    override def _scStateOpt: Option[ScJsState] = None

    /** Сборка HTTP-ответа. */
    def resultFut: Future[Result]

  }

}
