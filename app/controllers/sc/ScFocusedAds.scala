package controllers.sc

import _root_.util.jsa.{JsAppendById, JsAction, SmRcvResp, Js}
import io.suggest.util.Lists
import models.jsm.ProducerAdsResp
import models.msc._
import play.api.mvc.Result
import play.twirl.api.Html
import util.showcase._
import util.SiowebEsUtil.client
import util.PlayMacroLogsI
import util.acl._
import views.html.sc.foc._
import play.api.libs.json._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.collection.immutable
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.11.14 19:38
 * Description: Поддержка открытых рекламных карточек.
 */
trait ScFocusedAdsBase extends ScController with PlayMacroLogsI {

  /** Базовая логика обработки запросов сбора данных по рекламным карточкам и компиляции оных в результаты выполнения запросов. */
  trait FocusedAdsLogic extends AdCssRenderArgs {

    /** Параллельный рендер блоков, находящихся за пределом экрана, должен будет возращать результат этого типа для каждого блока. */
    type OBT

    // TODO Не искать вообще карточки, если firstIds.len >= adSearch.size
    // TODO Выставлять offset для поиска с учётом firstIds?

    def _adSearch: FocusedAdsSearchArgs
    def _scStateOpt: Option[ScJsState]
    implicit def _request: AbstractRequestWithPwOpt[_]

    // TODO Не искать вообще карточки, если firstIds.len >= adSearch.size
    // TODO Выставлять offset для поиска с учётом firstIds?
    lazy val mads1Fut: Future[Seq[MAd]] = {
      if (_adSearch.maxResults > _adSearch.forceFirstIds.size) {
        // Костыль, т.к. сортировка forceFirstIds на стороне ES-сервера всё ещё не пашет:
        val adSearch2 = if (_adSearch.forceFirstIds.isEmpty) {
          _adSearch
        } else {
          new AdSearchWrapper {
            override def _dsArgsUnderlying  = _adSearch
            override def forceFirstIds      = Nil
            override def withoutIds         = _adSearch.forceFirstIds
          }
        }
        MAd.dynSearch(adSearch2)

      } else {
        // Все firstIds перечислены, возвращаемый размер не подразумевает отдельного поиска.
        Future successful Nil
      }
    }

    def is3rdPartyProducer(producerId: String): Boolean = {
      !(_adSearch.receiverIds contains producerId)
    }

    /** В countAds() можно отправлять и обычный adSearch: forceFirstIds там игнорируется. */
    def madsCountFut: Future[Long] = {
      MAd.dynCount(_adSearch)
    }
    lazy val madsCountIntFut = madsCountFut.map(_.toInt)

    /**
     * 2014.jan.28: Если не найдены какие-то элементы, то сообщить об этом в логи.
     * Это нужно для более быстрого выявления проблем с валидными ссылками на несуществующие карточки.
     * @param mads найденные рекламные карточки.
     * @param ids id запрошенных рекламных карточек.
     */
    protected def logMissingFirstIds(mads: Seq[MAd], ids: Seq[String]): Unit = {
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
    lazy val firstAdsFut: Future[Seq[MAd]] = {
      if (_adSearch.forceFirstIds.nonEmpty && _adSearch.offset <= 0) {
        val ids = _adSearch.forceFirstIds
        val fut = MAd.multiGet(ids)
        fut onSuccess { case mads =>
          logMissingFirstIds(mads, ids)
        }
        fut
      } else {
        Future successful Nil
      }
    }


    /** Если выставлены forceFirstIds, то нужно подолнительно запросить получение указанных
      * id карточек и выставить их в начало списка mads1. */
    lazy val mads2Fut: Future[Seq[MAd]] = {
      val _mads1Fut = mads1Fut
      for {
        firstAds  <- firstAdsFut
        mads      <- _mads1Fut
      } yield {
        // Нано-оптимизация: По идее тут списки или stream'ы, а firstAds содержит только одну карточку. Поэтому можно попробовать смержить с О(1).
        Lists.appendSeqHead(firstAds, mads)
      }
    }

    /** Карта продьюсеров, относящихся к запрошенным focused-карточкам. */
    lazy val mads2ProducersFut: Future[Map[String, MAdnNode]] = {
      mads2Fut.flatMap { mads2 =>
        val producerIds = mads2.iterator.map(_.producerId)
        MAdnNodeCache.multiGet(producerIds)
          .map { ns =>
            ns.iterator
              .map { node => node.id.get -> node }
              .toMap
          }
      }
    }

    lazy val mads2andBrArgsFut: Future[Seq[blk.RenderArgs]] = {
      val _mads2Fut = mads2Fut
      val _ctx = ctx
      val _withCssClasses = withCssClasses
      _mads2Fut flatMap { mads =>
        Future.traverse(mads) { mad =>
          ShowcaseUtil.focusedBrArgsFor(mad)(_ctx)
            .map { brArgs =>
              brArgs.copy(
                inlineStyles    = false,
                cssClasses      = _withCssClasses,
                // 2015.mar.06: FIXME Это значение сейчас перезаписывается таким же через showcase.js. // TODO Они должны быть в стилях, а не тут.
                blockStyle      = brArgs.wideBg.map(_ => "position: absolute; top: 50px; left: 50%;")
              )
            }
        }
      }
    }


    def mads4blkRenderFut: Future[Seq[blk.RenderArgs]] = {
      mads2andBrArgsFut.map { mads =>
        if (_adSearch.withHeadAd) mads.tail else mads // Caused by: java.lang.UnsupportedOperationException: tail of empty list
      }
    }

    lazy val ctx = implicitly[Context]

    /** Тип аккамулятора при рендере блоков через renderOuterBlock(). */
    type BrAcc_t

    /** Начальный аккамулятор для первого вызова renderOuterBlock(). */
    def blockHtmlRenderAcc0: BrAcc_t

    /** Параллельный рендер последовательности блоков. */
    def blocksHtmlsFut: Future[Seq[OBT]] = {
      // Форсируем распараллеливание асинхронных операций.
      val _mads4blkRenderFut = mads4blkRenderFut
      val _producersMapFut = mads2ProducersFut

      madsCountIntFut flatMap { madsCountInt =>
        _mads4blkRenderFut flatMap { mads4blkRender =>
          _producersMapFut flatMap { producersMap =>

            val (_, futs) = Lists.mapFoldLeft(mads4blkRender, acc0 = (firstAdIndex, blockHtmlRenderAcc0)) {
              case ((index, brAcc0), brArgs) =>
                // Сразу инкрементим счетчик, т.к. если отсчитывать от offset, то будет ноль при первом вызове.
                val index1 = index + 1
                val producerId = brArgs.mad.producerId
                val args = AdBodyTplArgs(
                  brArgs    = brArgs,
                  producer  = producersMap(producerId),
                  index     = index1,
                  adsCount  = madsCountInt,
                  is3rdParty = is3rdPartyProducer(producerId)
                )
                val (renderFut, brAcc1) = renderOneBlockAcc(args, brAcc0)
                (index1, brAcc1) -> renderFut
            }
            Future.sequence(futs)

          }
        }
      }
    }

    /**
     * Отправить в очередь на исполнение рендер одного блока, заданного контейнером аргументов.
     * Метод должен вызываться в реализации логики из кода реализации метода renderOuterBlock().
     * @param args Аргументы рендера блока.
     * @return Фьючерс с html-рендером одного блока.
     */
    def renderBlockHtml(args: IAdBodyTplArgs): Future[Html] = {
      Future {
        _adTpl(args)(ctx)
      }
    }

    /**
     * Рендер одного блока. В случае Html можно просто вызвать renderBlockHtml().
     * 11.jun.2015: Добавлена поддержка синхронного аккамулятора для передачи данных между вызовами этого метода.
     * @param args Контейнер с данными для запуска рендера.
     * @param brAcc0 Аккамулятор.
     * @return Фьючерс рендера и новый аккамулятор.
     */
    def renderOneBlockAcc(args: AdBodyTplArgs, brAcc0: BrAcc_t): (Future[OBT], BrAcc_t)

    /** Что же будет рендерится в качестве текущей просматриваемой карточки? */
    lazy val focAdOptFut: Future[Option[blk.RenderArgs]] = {
      mads2andBrArgsFut.map(_.headOption)
    }

    /** Фьючерс продьюсера, относящегося к текущей карточке. */
    def focAdProducerOptFut: Future[Option[MAdnNode]] = {
      val _prodsMapFut = mads2ProducersFut
      focAdOptFut flatMap {
        case Some(focMad) =>
          _prodsMapFut map { prodsMap =>
            prodsMap.get(focMad.mad.producerId)
          }
        case None =>
          Future successful None
      }
    }

    def madProducerOptFut(madOpt: Option[MAd]): Future[Option[MAdnNode]] = {
      val prodIdOpt = madOpt.map(_.producerId)
      MAdnNodeCache.maybeGetByIdCached(prodIdOpt)
    }

    def firstAdIndex = _adSearch.offset + 1

    /** Сборка аргументов для рендера focused-карточки, т.е. раскрытого блока + оформление продьюсера. */
    protected def focAdsRenderArgsFor(abtArgs: IAdBodyTplArgs): Future[IFocusedAdsTplArgs] = {
      val producer = abtArgs.producer
      val logoImgOptFut = ShowcaseUtil.getLogoImgOpt(producer, _adSearch.screen)

      val _fgColor = producer.meta.fgColor getOrElse ShowcaseUtil.SITE_FGCOLOR_DFLT
      val _bgColor = producer.meta.color  getOrElse  ShowcaseUtil.SITE_BGCOLOR_DFLT

      for (_logoImgOpt <- logoImgOptFut) yield {
        FocusedAdsTplArgs2(
          abtArgs,
          bgColor     = _bgColor,
          fgColor     = _fgColor,
          hBtnArgs    = HBtnArgs(fgColor = _fgColor),
          logoImgOpt  = _logoImgOpt,
          is3rdParty  = is3rdPartyProducer(abtArgs.brArgs.mad.producerId),
          jsStateOpt  = _scStateOpt
        )
      }
    }

    /** Сборка контейнера аргументов для вызова шаблона _fullTpl(). */
    def focAdsHtmlArgsFut: Future[IFocusedAdsTplArgs] = {
      val _producerFut = focAdProducerOptFut.map(_.get)
      val _brArgsFut = focAdOptFut.map(_.get)
      val _madsCountIntFut = madsCountIntFut
      // Склеиваем фьючерсы в набор аргументов вызова focAdsRenderArgsFor().
      val abtArgsFut = for {
        _producer     <- _producerFut
        _brArgs       <- _brArgsFut
        madsCountInt  <- _madsCountIntFut
      } yield {
        AdBodyTplArgs(
          _brArgs, _producer,
          adsCount    = madsCountInt,
          index       = firstAdIndex,
          is3rdParty  = is3rdPartyProducer(_producer.id.get)
        )
      }
      // Запустить сборку аргументов рендера.
      abtArgsFut flatMap { abtArgs =>
        focAdsRenderArgsFor(abtArgs)
      }
    }

    override def adsCssExternalFut: Future[Seq[AdCssArgs]] = {
      mads2andBrArgsFut.map { mbas =>
        mbas.map { mba =>
          AdCssArgs(mba.mad.id.get, mba.szMult)
        }
      }
    }

    override def adsCssFieldRenderArgsFut: Future[immutable.Seq[blk.FieldCssRenderArgsT]] = {
      mads2andBrArgsFut.map { mbas =>
        mbas.iterator
          .flatMap { mba =>  mad2craIter(mba, mba.cssClasses) }
          .toStream
      }
    }

    /** Дописывать эти css-классы в стили и в рендер. */
    def withCssClasses = Seq("focused")

    /** Параметры для рендера обрамляющего css блоков (css не полей, а блоков в целом). */
    override def adsCssRenderArgsFut: Future[immutable.Seq[blk.IRenderArgs]] = {
      mads2andBrArgsFut.map { mbas =>
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
      * @return Если нет карточек, то будет NoSuchElementException. Иначе фьючерс с HTML-рендером. */
    def focAdHtmlFut: Future[Html] = {
      focAdsHtmlArgsFut map { args =>
        renderFocused(args)
      }
    }

    /** Опциональный аналог focAdHtmlFut. Функция учитывает значение _withHeadAd. */
    def focAdHtmlOptFut: Future[Option[Html]] = {
      if (_adSearch.withHeadAd) {
        focAdHtmlFut
          .map(Some.apply)
          .recover {
            case ex: NoSuchElementException =>
              None
            case ex: Throwable =>
              LOGGER.error("Failed to find focused ad", ex)
              None
          }
      } else {
        Future successful None
      }
    }

    override def jsAppendCssAction(html: JsString): JsAction = {
      JsAppendById("smResourcesFocused", html)
    }
  }

  /** Если поддержка аккамулятора при вызовах renderOutBlock() не требуется, то этот трейт отключит её. */
  trait NoBrAcc extends FocusedAdsLogic {

    override type BrAcc_t = None.type

    /** Начальный аккамулятор для первого вызова renderOuterBlock(). */
    override def blockHtmlRenderAcc0: BrAcc_t = None

    /** Рендер заэкранного блока. В случае Html можно просто вызвать renderBlockHtml(). */
    override def renderOneBlockAcc(args: AdBodyTplArgs, brAcc0: BrAcc_t): (Future[OBT], BrAcc_t) = {
      (renderOuterBlock(args), brAcc0)
    }

    def renderOuterBlock(args: AdBodyTplArgs): Future[OBT]
  }

}


/** Поддержка экшена для focused-ads API v1. */
trait ScFocusedAds extends ScFocusedAdsBase {

  /** Экшен для рендера горизонтальной выдачи карточек.
    * @param adSearch Поисковый запрос.
    * @return JSONP с отрендеренными карточками.
    */
  def focusedAds(adSearch: FocusedAdsSearchArgs) = MaybeAuth.async { implicit request =>
    val logic = getLogicFor(adSearch)
    // Запустить изменябельное тело экшена на исполнение.
    _focusedAds(logic)
  }

  /**
   * Тело экщена focusedAds() вынесено сюда для возможности перезаписывания.
   * @param logic Экземпляр focused-логики.
   * @param request Экземпляр реквеста.
   * @return Фьючерс с результатом.
   */
  protected def _focusedAds(logic: FocusedAdsLogicHttp)(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    _showFocusedAds(logic)
  }

  /**
   * Юзер просматривает карточки в раскрытом виде (фокусируется). Отрендерить браузер карточек.
   * @param logic Закешированная исходная focused-логика рендера.
   * @param request Исходный запрос.
   * @return Фьючерс с http-результатом.
   */
  protected def _showFocusedAds(logic: FocusedAdsLogicHttp)(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    // Запускаем сборку ответа:
    val resultFut = logic.resultFut

    // В фоне, когда поступят карточки, нужно будет сохранить по ним статистику:
    logic.mads2Fut onSuccess { case mads =>
      ScFocusedAdsStatUtil(
        adSearch    = logic._adSearch,
        madIds      = mads.flatMap(_.id),
        withHeadAd  = logic._adSearch.withHeadAd
      ).saveStats
    }

    resultFut
  }


  /** Перезаписываемый сборкщик логик для версий. */
  def getLogicFor(adSearch: FocusedAdsSearchArgs)(implicit request: AbstractRequestWithPwOpt[_]): FocusedAdsLogicHttp = {
    val vsn = adSearch.apiVsn
    if (vsn == MScApiVsns.Coffee) {
      new FocusedAdsLogicHttpV1(adSearch)
    } else {
      throw new UnsupportedOperationException("Unsupported api vsn: " + vsn)
    }
  }


  /** Расширение базовой focused-ads-логики для написания HTTP-экшенов. */
  protected trait FocusedAdsLogicHttp extends FocusedAdsLogic {

    /** Синхронного состояния выдачи тут обычно нет. */
    override def _scStateOpt: Option[ScJsState] = None

    /** Сборка HTTP-ответа. */
    def resultFut: Future[Result]
  }


  /**
   * Внутренняя реализация Focused-логики для v1 API.
   * Это API имело архитектурные особенности: первая карточка рендерилась с обрамлением и отдельном поле,
   * а остальные рендерились в основном поле. Такой подход вызывал неисправимые проблемы при нескольких
   * продьюсерах в пачке карточек.
   */
  protected class FocusedAdsLogicHttpV1(val _adSearch: FocusedAdsSearchArgs)
                                       (implicit val _request: AbstractRequestWithPwOpt[_])
    extends FocusedAdsLogicHttp
    with NoBrAcc
  {
    /** Тип отрендеренного блока в APIv1 -- это json-строка, содержащая HTML блока без заглавия и прочего. */
    override type OBT = JsString

    /** Рендерим в html, минифицируем, заворачиваем в js-строку. */
    override def renderOuterBlock(args: AdBodyTplArgs): Future[OBT] = {
      renderBlockHtml(args)
        .map(html2jsStr)
    }

    /** Отдельно отфокусированную карточку тоже нужно минифицировать и завернуть в JsString. */
    def focAdHtmlJsStrOptFut: Future[Option[JsString]] = {
      focAdHtmlOptFut
        .map(_.map(html2jsStr))
    }

    def producerAdsRespFut: Future[ProducerAdsResp] = {
      val _focAdHtmlJsStrOptFut = focAdHtmlJsStrOptFut
      for {
        outerBlocksRendered <- blocksHtmlsFut
        focAdHtmlOpt        <- _focAdHtmlJsStrOptFut
      } yield {
        ProducerAdsResp(focAdHtmlOpt, outerBlocksRendered)
      }
    }

    /** Итоговый результат выполнения запроса собирается тут. */
    override def resultFut: Future[Result] = {
      // Запуск сборки css-инжекции в <head> клиента:
      val _jsAppendAdsCssFut = jsAppendAdsCssFut
      for {
        prodAdsResp <- producerAdsRespFut
        cssInject   <- _jsAppendAdsCssFut
      } yield {
        cacheControlShort {
          Ok( Js(10000, cssInject, SmRcvResp(prodAdsResp)) )
        }
      }
    }

  }

}
